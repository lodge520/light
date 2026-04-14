package com.genius.smartlight.controller.admin.device;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.device.DeviceOnlineService;
import com.genius.smartlight.vo.device.DeviceOnlineStatusRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceOnlineController {

    private final DeviceOnlineService deviceOnlineService;

    @GetMapping("/online-status/{deviceCode}")
    public CommonResult<DeviceOnlineStatusRespVO> getOnlineStatus(@PathVariable String deviceCode) {
        return CommonResult.success(deviceOnlineService.getOnlineStatus(deviceCode));
    }

    @GetMapping("/online-list")
    public CommonResult<List<DeviceOnlineStatusRespVO>> getOnlineStatusList() {
        return CommonResult.success(deviceOnlineService.getOnlineStatusList());
    }
}