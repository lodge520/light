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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "停留时长接口", description = "热区/人流停留时长上报、查询和汇总接口，核心字段包括 chipId、durationValue、statDate、collectTime")
@RestController
@RequestMapping("/admin/duration")
@RequiredArgsConstructor
public class DurationController {

    private final DurationService durationService;

    @Operation(
            summary = "新增或累计停留时长",
            description = "设备端或检测服务上报停留时长。请求体包含 chipId、durationValue；statDate 可选，不传则服务端使用当天日期。"
    )
    @PostMapping("/create")
    public CommonResult<Long> createOrIncrease(@Valid @RequestBody DurationCreateReqVO reqVO) {
        return CommonResult.success(durationService.createOrIncrease(reqVO));
    }

    @Operation(summary = "按芯片ID与日期查询停留时长", description = "根据 chipId 和 statDate 查询单日停留时长统计")
    @GetMapping("/get")
    public CommonResult<DurationRespVO> getByChipIdAndDate(
            @Parameter(description = "芯片唯一ID", example = "ABC123456")
            @RequestParam String chipId,
            @Parameter(description = "统计日期，格式 yyyy-MM-dd", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate) {
        return CommonResult.success(durationService.getByChipIdAndDate(chipId, statDate));
    }

    @Operation(summary = "查询设备全部停留记录", description = "根据 chipId 查询该设备全部停留时长记录")
    @GetMapping("/list")
    public CommonResult<List<DurationRespVO>> getListByChipId(
            @Parameter(description = "芯片唯一ID", example = "ABC123456")
            @RequestParam String chipId) {
        return CommonResult.success(durationService.getListByChipId(chipId));
    }

    @Operation(summary = "按日期范围查询停留记录", description = "根据 chipId、startDate、endDate 查询指定日期范围内的停留记录")
    @GetMapping("/range")
    public CommonResult<List<DurationRespVO>> getListByDateRange(
            @Parameter(description = "芯片唯一ID", example = "ABC123456")
            @RequestParam String chipId,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getListByDateRange(chipId, startDate, endDate));
    }

    @Operation(summary = "按日期范围汇总停留时长", description = "根据 chipId、startDate、endDate 汇总指定设备在日期范围内的总停留时长")
    @GetMapping("/sum")
    public CommonResult<DurationSumRespVO> getSumByDateRange(
            @Parameter(description = "芯片唯一ID", example = "ABC123456")
            @RequestParam String chipId,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getSumByDateRange(chipId, startDate, endDate));
    }

    @Operation(summary = "按日期范围统计各设备停留汇总", description = "根据 startDate 和 endDate 统计各设备在日期范围内的停留时长汇总")
    @GetMapping("/summary")
    public CommonResult<List<DurationDeviceSummaryRespVO>> getDeviceSummaryByDateRange(
            @Parameter(description = "开始日期，格式 yyyy-MM-dd", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd", example = "2026-04-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getDeviceSummaryByDateRange(startDate, endDate));
    }
}
