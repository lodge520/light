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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Tag(name = "设备指令与网关接口")
@RestController
@RequestMapping("/admin/device")
@RequiredArgsConstructor
public class DeviceGatewayController {

    private static final Set<String> LAMP_ARM_ACTIONS = Set.of(
            "up", "down", "left", "right", "center", "home", "aim_person", "aim_cloth", "stop"
    );

    private static final Set<String> CAM_ARM_ACTIONS = Set.of(
            "up", "down", "left", "right", "center", "home",
            "slide_left", "slide_right", "slide_stop",
            "slider_position",
            "cam_person", "cam_cloth",
            "go_lamp_1", "go_lamp_2", "go_lamp_3",
            "stop"
    );

    private static final Set<String> ARM_SPEEDS = Set.of("slow", "normal", "fast");

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

        boolean added = exist != null && exist.getStoreId() != null;

        DeviceAnnounceRespVO respVO = new DeviceAnnounceRespVO();
        respVO.setAdded(added);

        webSocketPushService.pushAnnounce(
                reqVO.getChipId(),
                reqVO.getIp(),
                reqVO.getDeviceType(),
                added
        );

        return CommonResult.success(respVO);
    }

    @Operation(summary = "控制设备云台方向")
    @PostMapping("/arm/{chipId}")
    public CommonResult<Boolean> armControl(
            @Parameter(description = "芯片ID", example = "ABC123456")
            @PathVariable String chipId,
            @Valid @RequestBody DeviceArmControlReqVO reqVO) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
        );
        if (device == null) {
            throw new ServiceException("设备不存在");
        }

        String deviceType = normalizeArmDeviceType(device.getDeviceType());
        String action = resolveArmAction(reqVO);
        String speed = normalizeArmSpeed(reqVO.getSpeed());

        validateArmAction(deviceType, action);
        validateSliderPosition(deviceType, action, reqVO.getPosition());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "arm");
        payload.put("action", action);
        payload.put("speed", speed);
        payload.put("deviceType", "camlamp".equals(deviceType) ? "cam" : deviceType);
        if ("slider_position".equals(action)) {
            payload.put("position", reqVO.getPosition());
        }

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
            @Valid @RequestBody DeviceStateSyncReqVO reqVO) {
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

    private String resolveArmAction(DeviceArmControlReqVO reqVO) {
        String action = normalizeText(reqVO.getAction());
        if (action == null) {
            action = normalizeText(reqVO.getDirection());
        }
        if (action == null) {
            throw new ServiceException("云台动作不能为空");
        }
        return action;
    }

    private String normalizeArmSpeed(String value) {
        String speed = normalizeText(value);
        if (speed == null) {
            return "normal";
        }
        if (!ARM_SPEEDS.contains(speed)) {
            throw new ServiceException("云台速度只能是 slow、normal 或 fast");
        }
        return speed;
    }

    private String normalizeArmDeviceType(String value) {
        String deviceType = normalizeText(value);
        if ("lamp".equals(deviceType) || "cam".equals(deviceType) || "camlamp".equals(deviceType)) {
            return deviceType;
        }
        throw new ServiceException("设备类型不支持云台控制");
    }

    private void validateArmAction(String deviceType, String action) {
        Set<String> allowedActions = "lamp".equals(deviceType) ? LAMP_ARM_ACTIONS : CAM_ARM_ACTIONS;
        if (!allowedActions.contains(action)) {
            throw new ServiceException("当前设备类型不支持云台动作：" + action);
        }
    }

    private void validateSliderPosition(String deviceType, String action, Integer position) {
        if (!"slider_position".equals(action)) {
            return;
        }
        if ("lamp".equals(deviceType)) {
            throw new ServiceException("lamp 设备不支持滑轨位置控制");
        }
        if (position == null) {
            throw new ServiceException("滑轨位置不能为空");
        }
        if (position < 0 || position > 500) {
            throw new ServiceException("滑轨位置范围必须是 0 到 500 mm");
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
