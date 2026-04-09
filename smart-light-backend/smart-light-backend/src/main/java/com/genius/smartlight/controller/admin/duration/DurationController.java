package com.genius.smartlight.controller.admin.duration;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.duration.DurationService;
import com.genius.smartlight.vo.duration.DurationCreateReqVO;
import com.genius.smartlight.vo.duration.DurationDeviceSummaryRespVO;
import com.genius.smartlight.vo.duration.DurationRespVO;
import com.genius.smartlight.vo.duration.DurationSumRespVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/duration")
@RequiredArgsConstructor
public class DurationController {

    private final DurationService durationService;

    @PostMapping("/create")
    public CommonResult<Long> createOrIncrease(@Valid @RequestBody DurationCreateReqVO reqVO) {
        return CommonResult.success(durationService.createOrIncrease(reqVO));
    }

    @GetMapping("/get")
    public CommonResult<DurationRespVO> getByDeviceCodeAndDate(
            @RequestParam String deviceCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate) {
        return CommonResult.success(durationService.getByDeviceCodeAndDate(deviceCode, statDate));
    }

    @GetMapping("/list")
    public CommonResult<List<DurationRespVO>> getListByDeviceCode(@RequestParam String deviceCode) {
        return CommonResult.success(durationService.getListByDeviceCode(deviceCode));
    }

    @GetMapping("/range")
    public CommonResult<List<DurationRespVO>> getListByDateRange(
            @RequestParam String deviceCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getListByDateRange(deviceCode, startDate, endDate));
    }

    @GetMapping("/sum")
    public CommonResult<DurationSumRespVO> getSumByDateRange(
            @RequestParam String deviceCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getSumByDateRange(deviceCode, startDate, endDate));
    }

    @GetMapping("/summary")
    public CommonResult<List<DurationDeviceSummaryRespVO>> getDeviceSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(durationService.getDeviceSummaryByDateRange(startDate, endDate));
    }
}