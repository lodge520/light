package com.genius.smartlight.controller.admin.device;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.device.DeviceOnlineService;
import com.genius.smartlight.vo.device.DeviceOnlineStatusRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "设备在线状态")
@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceOnlineController {

    private final DeviceOnlineService deviceOnlineService;

    @Operation(summary = "查询单个设备在线状态")
    @GetMapping("/online-status/{chipId}")
    public CommonResult<DeviceOnlineStatusRespVO> getOnlineStatus(
            @Parameter(description = "芯片ID", example = "ABC123456")
            @PathVariable String chipId) {
        return CommonResult.success(deviceOnlineService.getOnlineStatus(chipId));
    }

    @Operation(summary = "查询在线设备列表")
    @GetMapping("/online-list")
    public CommonResult<List<DeviceOnlineStatusRespVO>> getOnlineStatusList() {
        return CommonResult.success(deviceOnlineService.getOnlineStatusList());
    }
}