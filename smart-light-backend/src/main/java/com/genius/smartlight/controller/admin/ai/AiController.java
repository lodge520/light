package com.genius.smartlight.controller.admin.ai;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.ai.AiService;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI识别接口")
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

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
}