package com.genius.smartlight.controller.admin.device;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.device.DeviceService;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceSaveReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "设备管理")
@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "连通性测试")
    @GetMapping("/ping")
    public CommonResult<String> ping() {
        return CommonResult.success("ok");
    }

    @Operation(summary = "创建设备")
    @PostMapping("/create")
    public CommonResult<Long> createDevice(@Valid @RequestBody DeviceSaveReqVO reqVO) {
        return CommonResult.success(deviceService.createDevice(reqVO));
    }

    @Operation(summary = "更新设备")
    @PutMapping("/update/{id}")
    public CommonResult<Boolean> updateDevice(
            @Parameter(description = "设备ID") @PathVariable Long id,
            @Valid @RequestBody DeviceSaveReqVO reqVO) {
        deviceService.updateDevice(id, reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Boolean> deleteDevice(
            @Parameter(description = "设备ID") @PathVariable Long id) {
        deviceService.deleteDevice(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "根据ID查询设备")
    @GetMapping("/get/{id}")
    public CommonResult<DeviceRespVO> getDevice(
            @Parameter(description = "设备ID") @PathVariable Long id) {
        return CommonResult.success(deviceService.getDevice(id));
    }

    @Operation(summary = "查询设备列表")
    @GetMapping("/list")
    public CommonResult<List<DeviceRespVO>> getDeviceList() {
        return CommonResult.success(deviceService.getDeviceList());
    }

    @Operation(summary = "根据芯片ID查询设备")
    @GetMapping("/by-chip-id")
    public CommonResult<DeviceRespVO> getDeviceByChipId(
            @Parameter(description = "芯片ID") @RequestParam String chipId) {
        return CommonResult.success(deviceService.getDeviceByChipId(chipId));
    }
}