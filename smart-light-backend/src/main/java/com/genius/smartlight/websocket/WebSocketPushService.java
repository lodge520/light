package com.genius.smartlight.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import com.genius.smartlight.vo.device.DeviceOnlineStatusRespVO;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.duration.DurationRespVO;
import com.genius.smartlight.vo.lux.LuxRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final DeviceSessionManager deviceSessionManager;
    public void pushState(DeviceRespVO data) {
        broadcast("state", data);
    }

    public void pushStateToDevice(String chipId, DeviceRespVO data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("chipId", chipId);
            payload.put("brightness", data.getBrightness());
            payload.put("temp", data.getTemp());
            payload.put("autoMode", data.getAutoMode());
            payload.put("recommendedBrightness", data.getRecommendedBrightness());
            payload.put("recommendedTemp", data.getRecommendedTemp());
            payload.put("fabric", data.getFabric());
            payload.put("mainColorRgb", data.getMainColorRgb());

            Map<String, Object> message = new HashMap<>();
            message.put("type", "state");
            message.put("data", payload);

            String json = objectMapper.writeValueAsString(message);

            boolean sent = deviceSessionManager.sendToDevice(chipId, json);

            if (!sent) {
                log.warn("设备状态下发失败，设备不在线或连接不可用，chipId={}, payload={}", chipId, json);
            } else {
                log.info("设备状态已下发，chipId={}, payload={}", chipId, json);
            }
        } catch (Exception e) {
            log.error("设备状态下发异常，chipId={}", chipId, e);
        }
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

    public void pushFabricRecognize(String chipId, String filename, FabricRecognizeRespVO result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chipId", chipId);
        data.put("filename", filename);
        data.put("label", result.getLabel());
        data.put("confidence", result.getConfidence());
        broadcast("fabricRecognize", data);
    }

    public void pushPersonDetect(String chipId, String filename, PersonDetectRespVO result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chipId", chipId);
        data.put("filename", filename);
        data.put("count", result.getCount());
        data.put("confidence", result.getConfidence());
        data.put("timestamp", result.getTimestamp());
        data.put("processingTime", result.getProcessingTime());
        data.put("annotatedImageBase64", result.getAnnotatedImageBase64());
        broadcast("personDetection", data);
    }

    public void pushAnnounce(String chipId, String ip, String deviceType, Boolean added) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chipId", chipId);
        data.put("ip", ip);
        data.put("deviceType", deviceType);
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