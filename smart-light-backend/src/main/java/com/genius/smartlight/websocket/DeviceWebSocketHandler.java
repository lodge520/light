package com.genius.smartlight.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.convert.device.DeviceConvert;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.service.device.DeviceOnlinePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final DeviceSessionManager deviceSessionManager;
    private final DeviceOnlinePushService deviceOnlinePushService;
    private final ObjectMapper objectMapper;
    private final DeviceMapper deviceMapper;
    private final WebSocketPushService webSocketPushService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Device websocket connected: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            String chipId = node.path("chipId").asText();

            if ("register".equals(type)) {
                if (chipId == null || chipId.isBlank()) {
                    log.warn("Device register missing chipId, sessionId={}", session.getId());
                    return;
                }
                deviceSessionManager.registerDevice(chipId, session);
                syncFirmwareInfo(chipId, node);
                deviceOnlinePushService.pushIfChanged(chipId);
                session.sendMessage(new TextMessage("{\"type\":\"registerAck\",\"data\":\"ok\"}"));
                return;
            }

            if ("ping".equals(type)) {
                if (chipId != null && !chipId.isBlank()) {
                    deviceSessionManager.touch(chipId);
                    deviceOnlinePushService.pushIfChanged(chipId);
                }
                session.sendMessage(new TextMessage("{\"type\":\"pong\",\"data\":\"ok\"}"));
                return;
            }

            log.info("Unknown device ws message: {}", message.getPayload());
        } catch (Exception e) {
            log.warn("Invalid device websocket message: {}", message.getPayload(), e);
        }
    }

    private void syncFirmwareInfo(String chipId, JsonNode node) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
        );
        if (device == null) {
            return;
        }

        boolean changed = false;
        String fwVersion = node.path("fwVersion").asText(null);
        if (fwVersion != null && !fwVersion.isBlank()) {
            device.setFirmwareVersion(fwVersion);
            changed = true;
        }

        Integer fwVersionCode = readOptionalInt(node, "fwVersionCode");
        if (fwVersionCode == null) {
            fwVersionCode = readOptionalInt(node, "firmwareVersionCode");
        }
        if (fwVersionCode != null) {
            device.setFirmwareVersionCode(fwVersionCode);
            changed = true;
        }

        String channel = node.path("firmwareChannel").asText(null);
        if (channel == null || channel.isBlank()) {
            channel = node.path("channel").asText(null);
        }
        if (channel != null && !channel.isBlank()) {
            device.setFirmwareChannel(channel);
            changed = true;
        }

        if (device.getOtaStatus() == null || device.getOtaStatus().isBlank()) {
            device.setOtaStatus("idle");
            changed = true;
        }

        if (changed) {
            deviceMapper.updateById(device);
            webSocketPushService.pushState(DeviceConvert.convert(device));
        }
    }

    private Integer readOptionalInt(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual() && !value.asText().isBlank()) {
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String chipId = deviceSessionManager.removeBySession(session);
        if (chipId != null) {
            deviceOnlinePushService.pushIfChanged(chipId);
        }
    }
}
