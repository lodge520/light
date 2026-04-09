package com.genius.smartlight.controller.admin.lux;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.lux.LuxService;
import com.genius.smartlight.vo.lux.LuxCreateReqVO;
import com.genius.smartlight.vo.lux.LuxRespVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/lux")
@RequiredArgsConstructor
public class LuxController {

    private final LuxService luxService;

    @PostMapping("/create")
    public CommonResult<Long> createLuxRecord(@Valid @RequestBody LuxCreateReqVO reqVO) {
        return CommonResult.success(luxService.createLuxRecord(reqVO));
    }

    @GetMapping("/get-latest")
    public CommonResult<LuxRespVO> getLatestLuxRecord(@RequestParam String deviceCode) {
        return CommonResult.success(luxService.getLatestLuxRecord(deviceCode));
    }

    @GetMapping("/list")
    public CommonResult<List<LuxRespVO>> getLuxRecordList(@RequestParam String deviceCode) {
        return CommonResult.success(luxService.getLuxRecordList(deviceCode));
    }
}