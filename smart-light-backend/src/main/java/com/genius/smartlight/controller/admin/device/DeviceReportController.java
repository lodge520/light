package com.genius.smartlight.controller.admin.device;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.device.DeviceReportService;
import com.genius.smartlight.vo.device.DeviceStateReportReqVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceReportController {

    private final DeviceReportService deviceReportService;

    @PostMapping("/state-report")
    public CommonResult<Boolean> stateReport(@Valid @RequestBody DeviceStateReportReqVO reqVO) {
        deviceReportService.reportState(reqVO);
        return CommonResult.success(true);
    }
}
