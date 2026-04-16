package com.genius.smartlight.controller.admin.device;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genius.smartlight.common.CommonResult;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.service.device.DeviceControlService;
import com.genius.smartlight.vo.device.DeviceAnnounceReqVO;
import com.genius.smartlight.vo.device.DeviceAnnounceRespVO;
import com.genius.smartlight.vo.device.DeviceArmControlReqVO;
import com.genius.smartlight.vo.device.DeviceFlowUploadReqVO;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceStateSyncReqVO;
import com.genius.smartlight.websocket.DeviceSessionManager;
import com.genius.smartlight.websocket.WebSocketPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "设备指令与网关接口")
@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceGatewayController {

    private final DeviceMapper deviceMapper;
    private final DeviceSessionManager deviceSessionManager;
    private final WebSocketPushService webSocketPushService;
    private final ObjectMapper objectMapper;
    private final DeviceControlService deviceControlService;

    @Operation(summary = "设备上线通告")
    @PostMapping("/announce")
    public CommonResult<DeviceAnnounceRespVO> announce(
            @Valid @RequestBody DeviceAnnounceReqVO reqVO) {
        DeviceDO exist = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, reqVO.getChipId())
        );

        boolean added = exist != null;

        DeviceAnnounceRespVO respVO = new DeviceAnnounceRespVO();
        respVO.setAdded(added);

        webSocketPushService.pushAnnounce(reqVO.getChipId(), reqVO.getIp(), added);
        return CommonResult.success(respVO);
    }

    @Operation(summary = "控制设备云台方向")
    @PostMapping("/arm/{chipId}")
    public CommonResult<Boolean> armControl(
            @Parameter(description = "芯片ID", example = "ABC123456")
            @PathVariable String chipId,
            @Valid @RequestBody DeviceArmControlReqVO reqVO) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "arm");
        payload.put("chipId", chipId);
        payload.put("direction", reqVO.getDirection());

        sendToDevice(chipId, payload);
        return CommonResult.success(true);
    }

    @Operation(summary = "下发服装图片上传指令")
    @PostMapping("/cloth-upload/{chipId}")
    public CommonResult<Boolean> clothUpload(
            @Parameter(description = "芯片ID", example = "ABC123456")
            @PathVariable String chipId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "command");
        payload.put("cmd", "upload_cloth");

        sendToDevice(chipId, payload);
        return CommonResult.success(true);
    }

    @Operation(summary = "下发人流上传开关指令")
    @PostMapping("/flow-upload/{chipId}")
    public CommonResult<Boolean> flowUpload(
            @Parameter(description = "芯片ID", example = "ABC123456")
            @PathVariable String chipId,
            @Valid @RequestBody DeviceFlowUploadReqVO reqVO) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "command");
        payload.put("cmd", "flow_upload");
        payload.put("enabled", reqVO.getEnabled());

        sendToDevice(chipId, payload);
        return CommonResult.success(true);
    }

    @Operation(summary = "同步设备状态到终端")
    @PostMapping("/state-sync/{chipId}")
    public CommonResult<DeviceRespVO> stateSync(
            @Parameter(description = "芯片ID", example = "ABC123456")
            @PathVariable String chipId,
            @RequestBody DeviceStateSyncReqVO reqVO) {
        return CommonResult.success(deviceControlService.syncStateToDevice(chipId, reqVO));
    }

    private void sendToDevice(String chipId, Object payload) {
        if (!deviceSessionManager.isOnline(chipId)) {
            throw new ServiceException("设备未连接或已离线");
        }

        try {
            String text = objectMapper.writeValueAsString(payload);
            boolean sent = deviceSessionManager.sendToDevice(chipId, text);
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