package com.genius.smartlight.controller.admin.device;

import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.service.device.DeviceService;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceSaveReqVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/ping")
    public CommonResult<String> ping() {
        return CommonResult.success("ok");
    }

    @PostMapping("/create")
    public CommonResult<Long> createDevice(@Valid @RequestBody DeviceSaveReqVO reqVO) {
        return CommonResult.success(deviceService.createDevice(reqVO));
    }

    @PutMapping("/update/{id}")
    public CommonResult<Boolean> updateDevice(@PathVariable Long id,
                                              @Valid @RequestBody DeviceSaveReqVO reqVO) {
        deviceService.updateDevice(id, reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResult<Boolean> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return CommonResult.success(true);
    }

    @GetMapping("/get/{id}")
    public CommonResult<DeviceRespVO> getDevice(@PathVariable Long id) {
        return CommonResult.success(deviceService.getDevice(id));
    }

    @GetMapping("/list")
    public CommonResult<List<DeviceRespVO>> getDeviceList() {
        return CommonResult.success(deviceService.getDeviceList());
    }

    @GetMapping("/get-by-code")
    public CommonResult<DeviceRespVO> getDeviceByCode(@RequestParam String deviceCode) {
        return CommonResult.success(deviceService.getDeviceByCode(deviceCode));
    }
}