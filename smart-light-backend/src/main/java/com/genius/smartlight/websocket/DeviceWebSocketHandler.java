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
            String chipId = node.path("chipId").asText();

            if ("register".equals(type)) {
                if (chipId == null || chipId.isBlank()) {
                    log.warn("Device register missing chipId, sessionId={}", session.getId());
                    return;
                }
                deviceSessionManager.registerDevice(chipId, session);
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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String chipId = deviceSessionManager.removeBySession(session);
        if (chipId != null) {
            deviceOnlinePushService.pushIfChanged(chipId);
        }
    }
}