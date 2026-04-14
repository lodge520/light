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

    public void registerDevice(String deviceCode, WebSocketSession session) {
        WebSocketSession oldSession = deviceSessionMap.put(deviceCode, session);
        lastSeenMap.put(deviceCode, System.currentTimeMillis());

        if (oldSession != null && oldSession != session && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (IOException e) {
                log.warn("Close old device session failed, deviceCode={}", deviceCode, e);
            }
        }

        log.info("Device registered: deviceCode={}, sessionId={}", deviceCode, session.getId());
    }

    public void touch(String deviceCode) {
        if (deviceCode != null && !deviceCode.isBlank()) {
            lastSeenMap.put(deviceCode, System.currentTimeMillis());
        }
    }

    public boolean isOnline(String deviceCode) {
        WebSocketSession session = deviceSessionMap.get(deviceCode);
        Long lastSeen = lastSeenMap.get(deviceCode);

        return session != null
                && session.isOpen()
                && lastSeen != null
                && System.currentTimeMillis() - lastSeen <= ONLINE_TIMEOUT_MS;
    }

    public Long getLastSeen(String deviceCode) {
        return lastSeenMap.get(deviceCode);
    }

    public Set<String> getTrackedDeviceCodes() {
        Set<String> codes = new HashSet<>(lastSeenMap.keySet());
        codes.addAll(deviceSessionMap.keySet());
        return codes;
    }

    public boolean sendToDevice(String deviceCode, String payload) {
        WebSocketSession session = deviceSessionMap.get(deviceCode);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IOException e) {
            log.error("Send message to device failed, deviceCode={}", deviceCode, e);
            return false;
        }
    }

    public String removeBySession(WebSocketSession session) {
        String targetDeviceCode = null;
        for (Map.Entry<String, WebSocketSession> entry : deviceSessionMap.entrySet()) {
            if (entry.getValue() == session) {
                targetDeviceCode = entry.getKey();
                break;
            }
        }

        if (targetDeviceCode != null) {
            deviceSessionMap.remove(targetDeviceCode);
            log.info("Device disconnected: deviceCode={}, sessionId={}", targetDeviceCode, session.getId());
        }
        return targetDeviceCode;
    }
}