package com.genius.smartlight.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.duration.DurationRespVO;
import com.genius.smartlight.vo.lux.LuxRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.genius.smartlight.vo.device.DeviceOnlineStatusRespVO;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public void pushState(DeviceRespVO data) {
        broadcast("state", data);
    }

    public void pushOnlineStatus(DeviceOnlineStatusRespVO data) {
        broadcast("onlineStatus", data);
    }

    public void pushDeviceDeleted(Long id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        broadcast("deviceDeleted", data);
    }

    public void pushLux(LuxRespVO data) {
        broadcast("lux", data);
    }

    public void pushDuration(DurationRespVO data) {
        broadcast("durationUpdate", data);
    }

    public void pushFabricRecognize(String deviceCode, String filename, FabricRecognizeRespVO result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCode", deviceCode);
        data.put("filename", filename);
        data.put("label", result.getLabel());
        data.put("confidence", result.getConfidence());
        broadcast("fabricRecognize", data);
    }

    public void pushPersonDetect(String deviceCode, String filename, PersonDetectRespVO result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCode", deviceCode);
        data.put("filename", filename);
        data.put("count", result.getCount());
        data.put("confidence", result.getConfidence());
        data.put("timestamp", result.getTimestamp());
        data.put("processingTime", result.getProcessingTime());
        data.put("annotatedImageBase64", result.getAnnotatedImageBase64());
        broadcast("personDetection", data);
    }

    public void pushAnnounce(String deviceCode, String ip, Boolean added) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCode", deviceCode);
        data.put("ip", ip);
        data.put("added", added);
        broadcast("announce", data);
    }

    private void broadcast(String type, Object data) {
        try {
            String payload = objectMapper.writeValueAsString(WsMessage.of(type, data));
            sessionManager.broadcast(payload);
        } catch (Exception e) {
            log.error("WebSocket broadcast failed, type={}", type, e);
        }
    }
}