package com.genius.smartlight.controller.admin.lux;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.lux.LuxService;
import com.genius.smartlight.service.lux.MultiLuxService;
import com.genius.smartlight.vo.lux.LuxCreateReqVO;
import com.genius.smartlight.vo.lux.MultiLuxRespVO;
import com.genius.smartlight.vo.lux.LuxRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "光照数据管理")
@RestController
@RequestMapping("/admin/lux")
@RequiredArgsConstructor
public class LuxController {

    private final LuxService luxService;
    private final MultiLuxService multiLuxService;
    @Operation(summary = "新增光照记录")
    @PostMapping("/create")
    public CommonResult<Long> createLuxRecord(@Valid @RequestBody LuxCreateReqVO reqVO) {
        return CommonResult.success(luxService.createLuxRecord(reqVO));
    }

    @Operation(summary = "查询设备最新光照")
    @GetMapping("/get-latest")
    public CommonResult<LuxRespVO> getLatestLuxRecord(
            @Parameter(description = "设备编码", example = "device001")
            @RequestParam String deviceCode) {
        return CommonResult.success(luxService.getLatestLuxRecord(deviceCode));
    }

    @Operation(summary = "查询设备光照记录列表")
    @GetMapping("/list")
    public CommonResult<List<LuxRespVO>> getLuxRecordList(
            @Parameter(description = "设备编码", example = "device001")
            @RequestParam String deviceCode) {
        return CommonResult.success(luxService.getLuxRecordList(deviceCode));
    }

    @Operation(summary = "查询多设备光照趋势")
    @GetMapping("/multi-trend")
    public CommonResult<MultiLuxRespVO> getMultiLux() {
        return CommonResult.success(multiLuxService.getMultiLux());
    }
}