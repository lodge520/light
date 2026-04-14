package com.genius.smartlight.service.device;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.vo.device.DeviceOnlineStatusRespVO;
import com.genius.smartlight.websocket.DeviceSessionManager;
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DeviceOnlinePushService {

    private final DeviceMapper deviceMapper;
    private final DeviceSessionManager deviceSessionManager;
    private final WebSocketPushService webSocketPushService;

    /**
     * 记录上一次已推送的在线状态，避免重复推送
     */
    private final Map<String, Boolean> lastPushedStatusMap = new ConcurrentHashMap<>();

    public void pushIfChanged(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return;
        }

        boolean currentOnline = deviceSessionManager.isOnline(deviceCode);
        Boolean lastPushed = lastPushedStatusMap.get(deviceCode);

        if (lastPushed == null || lastPushed != currentOnline) {
            DeviceOnlineStatusRespVO respVO = buildOnlineStatus(deviceCode);
            webSocketPushService.pushOnlineStatus(respVO);
            lastPushedStatusMap.put(deviceCode, currentOnline);
        }
    }

    public void scanAndPushOfflineChanges() {
        for (String deviceCode : deviceSessionManager.getTrackedDeviceCodes()) {
            pushIfChanged(deviceCode);
        }
    }

    private DeviceOnlineStatusRespVO buildOnlineStatus(String deviceCode) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getDeviceCode, deviceCode)
        );

        DeviceOnlineStatusRespVO respVO = new DeviceOnlineStatusRespVO();
        respVO.setDeviceCode(deviceCode);
        respVO.setIp(device != null ? device.getIp() : null);
        respVO.setOnline(deviceSessionManager.isOnline(deviceCode));
        respVO.setLastSeen(deviceSessionManager.getLastSeen(deviceCode));
        return respVO;
    }
}