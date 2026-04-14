package com.genius.smartlight.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionManager.addSession(session);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("onlineCount", sessionManager.getSessionCount());

        sessionManager.send(session, objectMapper.writeValueAsString(WsMessage.of("connected", data)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText();

            if ("ping".equals(type)) {
                sessionManager.send(session, objectMapper.writeValueAsString(WsMessage.of("pong", "ok")));
            }
        } catch (Exception e) {
            log.warn("Ignore invalid websocket message: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session);
    }
}