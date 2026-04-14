package com.genius.smartlight.controller.admin.ai;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.ai.AiService;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping(value = "/fabric-recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<FabricRecognizeRespVO> fabricRecognize(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String deviceCode) {
        return CommonResult.success(aiService.fabricRecognize(deviceCode, file));
    }

    @PostMapping(value = "/person-detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<PersonDetectRespVO> personDetect(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String deviceCode) {
        return CommonResult.success(aiService.personDetect(deviceCode, file));
    }
}