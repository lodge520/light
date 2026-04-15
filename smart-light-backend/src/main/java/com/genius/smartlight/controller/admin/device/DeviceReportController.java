package com.genius.smartlight.controller.admin.device;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.device.DeviceReportService;
import com.genius.smartlight.vo.device.DeviceStateReportReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设备状态上报")
@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceReportController {

    private final DeviceReportService deviceReportService;

    @Operation(summary = "设备上报当前状态")
    @PostMapping("/state-report")
    public CommonResult<Boolean> stateReport(@Valid @RequestBody DeviceStateReportReqVO reqVO) {
        deviceReportService.reportState(reqVO);
        return CommonResult.success(true);
    }
}