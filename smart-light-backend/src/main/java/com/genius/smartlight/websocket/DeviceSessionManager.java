package com.genius.smartlight.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DeviceSessionManager {

    private static final long ONLINE_TIMEOUT_MS = 60_000L;

    private final Map<String, WebSocketSession> deviceSessionMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeenMap = new ConcurrentHashMap<>();

    public void registerDevice(String chipId, WebSocketSession session) {
        WebSocketSession oldSession = deviceSessionMap.put(chipId, session);
        lastSeenMap.put(chipId, System.currentTimeMillis());

        if (oldSession != null && oldSession != session && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (IOException e) {
                log.warn("Close old device session failed, chipId={}", chipId, e);
            }
        }

        log.info("Device registered: chipId={}, sessionId={}", chipId, session.getId());
    }

    public void touch(String chipId) {
        if (chipId != null && !chipId.isBlank()) {
            lastSeenMap.put(chipId, System.currentTimeMillis());
        }
    }

    public boolean isOnline(String chipId) {
        WebSocketSession session = deviceSessionMap.get(chipId);
        Long lastSeen = lastSeenMap.get(chipId);

        return session != null
                && session.isOpen()
                && lastSeen != null
                && System.currentTimeMillis() - lastSeen <= ONLINE_TIMEOUT_MS;
    }

    public Long getLastSeen(String chipId) {
        return lastSeenMap.get(chipId);
    }

    public Set<String> getTrackedChipIds() {
        Set<String> chipIds = new HashSet<>(lastSeenMap.keySet());
        chipIds.addAll(deviceSessionMap.keySet());
        return chipIds;
    }

    public boolean sendToDevice(String chipId, String payload) {
        WebSocketSession session = deviceSessionMap.get(chipId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IOException e) {
            log.error("Send message to device failed, chipId={}", chipId, e);
            return false;
        }
    }

    public String removeBySession(WebSocketSession session) {
        String targetChipId = null;
        for (Map.Entry<String, WebSocketSession> entry : deviceSessionMap.entrySet()) {
            if (entry.getValue() == session) {
                targetChipId = entry.getKey();
                break;
            }
        }

        if (targetChipId != null) {
            deviceSessionMap.remove(targetChipId);
            log.info("Device disconnected: chipId={}, sessionId={}", targetChipId, session.getId());
        }
        return targetChipId;
    }
}