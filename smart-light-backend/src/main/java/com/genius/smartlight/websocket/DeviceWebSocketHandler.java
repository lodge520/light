package com.genius.smartlight.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Device websocket connected: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            String deviceCode = node.path("deviceCode").asText();

            if ("register".equals(type)) {
                if (deviceCode == null || deviceCode.isBlank()) {
                    log.warn("Device register missing deviceCode, sessionId={}", session.getId());
                    return;
                }
                deviceSessionManager.registerDevice(deviceCode, session);
                deviceOnlinePushService.pushIfChanged(deviceCode);
                session.sendMessage(new TextMessage("{\"type\":\"registerAck\",\"data\":\"ok\"}"));
                return;
            }

            if ("ping".equals(type)) {
                if (deviceCode != null && !deviceCode.isBlank()) {
                    deviceSessionManager.touch(deviceCode);
                    deviceOnlinePushService.pushIfChanged(deviceCode);
                }
                session.sendMessage(new TextMessage("{\"type\":\"pong\",\"data\":\"ok\"}"));
                return;
            }

            log.info("Unknown device ws message: {}", message.getPayload());
        } catch (Exception e) {
            log.warn("Invalid device websocket message: {}", message.getPayload(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceCode = deviceSessionManager.removeBySession(session);
        if (deviceCode != null) {
            deviceOnlinePushService.pushIfChanged(deviceCode);
        }
    }
}