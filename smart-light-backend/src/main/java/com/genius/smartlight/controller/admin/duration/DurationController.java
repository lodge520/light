package com.genius.smartlight.controller.admin.duration;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.duration.DurationService;
import com.genius.smartlight.vo.duration.DurationCreateReqVO;
import com.genius.smartlight.vo.duration.DurationDeviceSummaryRespVO;
import com.genius.smartlight.vo.duration.DurationRespVO;
import com.genius.smartlight.vo.duration.DurationSumRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "停留时长统计")
@RestController
@RequestMapping("/admin/duration")
@RequiredArgsConstructor
public class DurationController {

    private final DurationService durationService;

    @Operation(summary = "新增或累计停留时长")
    @PostMapping("/create")
    public CommonResult<Long> createOrIncrease(@Valid @RequestBody DurationCreateReqVO reqVO) {
        return CommonResult.success(durationService.createOrIncrease(reqVO));
    }

    @Operation(summary = "按设备与日期查询停留时长")
    @GetMapping("/get")
    public CommonResult<DurationRespVO> getByDeviceCodeAndDate(
            @Parameter(description = "设备编码", example = "device001")
            @RequestParam String deviceCode,
            @Parameter(description = "统计日期", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate) {
        return CommonResult.success(durationService.getByDeviceCodeAndDate(deviceCode, statDate));
    }

    @Operation(summary = "查询设备全部停留记录")
    @GetMapping("/list")
    public CommonResult<List<DurationRespVO>> getListByDeviceCode(
            @Parameter(description = "设备编码", example = "device001")
            @RequestParam String deviceCode) {
        return CommonResult.success(durationService.getListByDeviceCode(deviceCode));
    }

    @Operation(summary = "按日期范围查询停留记录")
    @GetMapping("/range")
    public CommonResult<List<DurationRespVO>> getListByDateRange(
            @Parameter(description = "设备编码", example = "device001")
            @RequestParam String deviceCode,
            @Parameter(description = "开始日期", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getListByDateRange(deviceCode, startDate, endDate));
    }

    @Operation(summary = "按日期范围汇总停留时长")
    @GetMapping("/sum")
    public CommonResult<DurationSumRespVO> getSumByDateRange(
            @Parameter(description = "设备编码", example = "device001")
            @RequestParam String deviceCode,
            @Parameter(description = "开始日期", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getSumByDateRange(deviceCode, startDate, endDate));
    }

    @Operation(summary = "按日期范围统计各设备停留汇总")
    @GetMapping("/summary")
    public CommonResult<List<DurationDeviceSummaryRespVO>> getDeviceSummaryByDateRange(
            @Parameter(description = "开始日期", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getDeviceSummaryByDateRange(startDate, endDate));
    }
}