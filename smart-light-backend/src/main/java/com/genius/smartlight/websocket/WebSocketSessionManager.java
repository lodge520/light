package com.genius.smartlight.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket connected: sessionId={}", session.getId());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
        log.info("WebSocket disconnected: sessionId={}", session.getId());
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public void send(WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.error("WebSocket send failed: sessionId={}", session.getId(), e);
        }
    }

    public void broadcast(String payload) {
        sessions.values().forEach(session -> send(session, payload));
    }
}