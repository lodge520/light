package com.genius.smartlight.controller.admin.ai;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.ai.AiService;
import com.genius.smartlight.vo.ai.FabricArchiveDeleteRespVO;
import com.genius.smartlight.vo.ai.FabricArchiveItemRespVO;
import com.genius.smartlight.vo.ai.FabricArchivePageRespVO;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Tag(name = "AI识别接口")
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private static final Path FABRIC_ARCHIVE_BASE_DIR = Path.of("/opt/smartlight/uploads/fabric");
    private static final String FABRIC_ARCHIVE_BASE_URL = "https://api.genius.show/uploads/fabric";
    private static final Set<String> FABRIC_ARCHIVE_TYPES = Set.of("original", "annotated", "combined");
    private static final Set<String> FABRIC_ARCHIVE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final DateTimeFormatter FILENAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter RESPONSE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern FABRIC_ARCHIVE_FILENAME_PATTERN = Pattern.compile(
            "^(.+)_([0-9]{8})_([0-9]{6})_[A-Fa-f0-9]{8}_(original|annotated|combined)\\.(jpg|jpeg|png)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FABRIC_ARCHIVE_BASENAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern FABRIC_ARCHIVE_DELETE_FILENAME_PATTERN = Pattern.compile(
            "^(.+)_(original|annotated|combined)\\.(jpg|jpeg|png)$",
            Pattern.CASE_INSENSITIVE
    );

    private final AiService aiService;

    @Operation(summary = "服装识别留档相册")
    @GetMapping("/fabric-archive")
    public CommonResult<FabricArchivePageRespVO> fabricArchive(
            @RequestParam(defaultValue = "combined") String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "30") Integer pageSize) throws IOException {
        return CommonResult.success(listFabricArchive(type, page, pageSize));
    }

    @Operation(summary = "删除服装识别留档图片")
    @DeleteMapping("/fabric-archive")
    public CommonResult<FabricArchiveDeleteRespVO> deleteFabricArchive(
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) String baseName) throws IOException {
        return CommonResult.success(deleteFabricArchiveGroup(filename, baseName));
    }

    @Operation(summary = "服装面料识别")
    @PostMapping(value = "/fabric-recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<FabricRecognizeRespVO> fabricRecognize(
            @Parameter(description = "上传图片文件")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "芯片ID，可选", example = "ABC123456")
            @RequestParam(required = false) String chipId) {
        return CommonResult.success(aiService.fabricRecognize(chipId, file));
    }

    @Operation(summary = "人流检测")
    @PostMapping(value = "/person-detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<PersonDetectRespVO> personDetect(
            @Parameter(description = "上传图片文件")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "芯片ID，可选", example = "ABC123456")
            @RequestParam(required = false) String chipId) {
        return CommonResult.success(aiService.personDetect(chipId, file));
    }

    private FabricArchivePageRespVO listFabricArchive(String type, Integer page, Integer pageSize) throws IOException {
        String normalizedType = normalizeArchiveType(type);
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        Path targetDir = FABRIC_ARCHIVE_BASE_DIR.resolve(normalizedType).normalize();

        List<FabricArchiveItemRespVO> items;
        if (!Files.isDirectory(targetDir)) {
            items = List.of();
        } else {
            try (Stream<Path> stream = Files.list(targetDir)) {
                items = stream
                        .filter(Files::isRegularFile)
                        .filter(this::isArchiveImage)
                        .map(path -> buildArchiveItem(path, normalizedType))
                        .sorted(Comparator.comparing(
                                item -> readLastModifiedMillis(targetDir.resolve(item.getFilename())),
                                Comparator.reverseOrder()
                        ))
                        .toList();
            }
        }

        long total = items.size();
        int fromIndex = Math.min((safePage - 1) * safePageSize, items.size());
        int toIndex = Math.min(fromIndex + safePageSize, items.size());

        FabricArchivePageRespVO respVO = new FabricArchivePageRespVO();
        respVO.setList(items.subList(fromIndex, toIndex));
        respVO.setTotal(total);
        respVO.setPage(safePage);
        respVO.setPageSize(safePageSize);
        return respVO;
    }

    private FabricArchiveDeleteRespVO deleteFabricArchiveGroup(String filename, String baseName) throws IOException {
        String normalizedBaseName = resolveDeleteBaseName(filename, baseName);
        FabricArchiveDeleteRespVO respVO = new FabricArchiveDeleteRespVO();
        respVO.setBaseName(normalizedBaseName == null ? "" : normalizedBaseName);
        respVO.setDeletedFiles(new ArrayList<>());
        respVO.setMissingFiles(new ArrayList<>());

        if (!isSafeArchiveBaseName(normalizedBaseName)) {
            respVO.setSuccess(false);
            respVO.setMsg("Invalid filename or baseName");
            respVO.setDeletedCount(0);
            return respVO;
        }

        for (String archiveType : List.of("original", "annotated", "combined")) {
            for (String extension : List.of(".jpg", ".jpeg", ".png")) {
                Path candidate = FABRIC_ARCHIVE_BASE_DIR
                        .resolve(archiveType)
                        .resolve(normalizedBaseName + "_" + archiveType + extension)
                        .normalize();

                if (!candidate.startsWith(FABRIC_ARCHIVE_BASE_DIR)) {
                    respVO.setSuccess(false);
                    respVO.setMsg("Invalid archive path");
                    respVO.setDeletedCount(respVO.getDeletedFiles().size());
                    return respVO;
                }

                String filePath = candidate.toString();
                if (!Files.exists(candidate)) {
                    respVO.getMissingFiles().add(filePath);
                    continue;
                }

                if (Files.isRegularFile(candidate)) {
                    Files.delete(candidate);
                    respVO.getDeletedFiles().add(filePath);
                } else {
                    respVO.getMissingFiles().add(filePath);
                }
            }
        }

        respVO.setDeletedCount(respVO.getDeletedFiles().size());
        if (respVO.getDeletedCount() > 0) {
            respVO.setSuccess(true);
            respVO.setMsg("Deleted archive image group");
        } else {
            respVO.setSuccess(false);
            respVO.setMsg("Archive image group not found");
        }
        return respVO;
    }

    private String resolveDeleteBaseName(String filename, String baseName) {
        if (baseName != null && !baseName.isBlank()) {
            return baseName.trim();
        }
        if (filename == null || filename.isBlank()) {
            return null;
        }

        String value = filename.trim();
        Matcher matcher = FABRIC_ARCHIVE_DELETE_FILENAME_PATTERN.matcher(value);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private boolean isSafeArchiveBaseName(String baseName) {
        if (baseName == null || baseName.isBlank()) {
            return false;
        }
        if (baseName.contains("..") || baseName.contains("/") || baseName.contains("\\")) {
            return false;
        }
        return FABRIC_ARCHIVE_BASENAME_PATTERN.matcher(baseName).matches();
    }

    private String normalizeArchiveType(String type) {
        String value = type == null ? "combined" : type.trim().toLowerCase(Locale.ROOT);
        return FABRIC_ARCHIVE_TYPES.contains(value) ? value : "combined";
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 30;
        }
        return Math.min(pageSize, 100);
    }

    private boolean isArchiveImage(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return FABRIC_ARCHIVE_EXTENSIONS.stream().anyMatch(filename::endsWith);
    }

    private FabricArchiveItemRespVO buildArchiveItem(Path path, String type) {
        String filename = path.getFileName().toString();
        Map<String, String> parsed = parseArchiveFilename(filename);

        FabricArchiveItemRespVO item = new FabricArchiveItemRespVO();
        item.setFilename(filename);
        item.setUrl(FABRIC_ARCHIVE_BASE_URL + "/" + type + "/" + filename);
        item.setType(type);
        item.setChipId(parsed.getOrDefault("chipId", ""));
        item.setCreateTime(parsed.getOrDefault("createTime", ""));
        item.setSize(readSize(path));
        return item;
    }

    private Map<String, String> parseArchiveFilename(String filename) {
        Matcher matcher = FABRIC_ARCHIVE_FILENAME_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return Map.of("chipId", "", "createTime", "");
        }

        String createTime = "";
        try {
            LocalDateTime time = LocalDateTime.parse(
                    matcher.group(2) + "_" + matcher.group(3),
                    FILENAME_TIME_FORMATTER
            );
            createTime = time.format(RESPONSE_TIME_FORMATTER);
        } catch (Exception ignored) {
            createTime = "";
        }

        return Map.of(
                "chipId", matcher.group(1),
                "createTime", createTime
        );
    }

    private Long readSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private Long readLastModifiedMillis(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class)
                    .lastModifiedTime()
                    .toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }
}
