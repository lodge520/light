package com.genius.smartlight.controller.admin.device;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.vo.device.DeviceAnnounceReqVO;
import com.genius.smartlight.vo.device.DeviceAnnounceRespVO;
import com.genius.smartlight.vo.device.DeviceArmControlReqVO;
import com.genius.smartlight.vo.device.DeviceFlowUploadReqVO;
import com.genius.smartlight.websocket.DeviceSessionManager;
import com.genius.smartlight.websocket.WebSocketPushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.genius.smartlight.service.device.DeviceControlService;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceStateSyncReqVO;


import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceGatewayController {

    private final DeviceMapper deviceMapper;
    private final DeviceSessionManager deviceSessionManager;
    private final WebSocketPushService webSocketPushService;
    private final ObjectMapper objectMapper;
    private final DeviceControlService deviceControlService;

    @PostMapping("/announce")
    public CommonResult<DeviceAnnounceRespVO> announce(@Valid @RequestBody DeviceAnnounceReqVO reqVO) {
        DeviceDO exist = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getDeviceCode, reqVO.getDeviceCode())
        );

        boolean added = exist != null;

        DeviceAnnounceRespVO respVO = new DeviceAnnounceRespVO();
        respVO.setAdded(added);

        webSocketPushService.pushAnnounce(reqVO.getDeviceCode(), reqVO.getIp(), added);
        return CommonResult.success(respVO);
    }

    @PostMapping("/arm/{deviceCode}")
    public CommonResult<Boolean> armControl(@PathVariable String deviceCode,
                                            @Valid @RequestBody DeviceArmControlReqVO reqVO) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "arm");
        payload.put("deviceCode", deviceCode);
        payload.put("direction", reqVO.getDirection());

        sendToDevice(deviceCode, payload);
        return CommonResult.success(true);
    }

    @PostMapping("/cloth-upload/{deviceCode}")
    public CommonResult<Boolean> clothUpload(@PathVariable String deviceCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "command");
        payload.put("cmd", "upload_cloth");

        sendToDevice(deviceCode, payload);
        return CommonResult.success(true);
    }

    @PostMapping("/flow-upload/{deviceCode}")
    public CommonResult<Boolean> flowUpload(@PathVariable String deviceCode,
                                            @Valid @RequestBody DeviceFlowUploadReqVO reqVO) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "command");
        payload.put("cmd", "flow_upload");
        payload.put("enabled", reqVO.getEnabled());

        sendToDevice(deviceCode, payload);
        return CommonResult.success(true);
    }
    @PostMapping("/state-sync/{deviceCode}")
    public CommonResult<DeviceRespVO> stateSync(@PathVariable String deviceCode,
                                                @RequestBody DeviceStateSyncReqVO reqVO) {
        return CommonResult.success(deviceControlService.syncStateToDevice(deviceCode, reqVO));
    }

    private void sendToDevice(String deviceCode, Object payload) {
        if (!deviceSessionManager.isOnline(deviceCode)) {
            throw new ServiceException("设备未连接或已离线");
        }

        try {
            String text = objectMapper.writeValueAsString(payload);
            boolean sent = deviceSessionManager.sendToDevice(deviceCode, text);
            if (!sent) {
                throw new ServiceException("指令发送失败");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("指令序列化失败：" + e.getMessage());
        }
    }
}