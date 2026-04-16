package com.genius.smartlight.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.convert.device.DeviceConvert;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.service.device.DeviceReportService;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceStateReportReqVO;
import com.genius.smartlight.websocket.DeviceSessionManager;
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceReportServiceImpl implements DeviceReportService {

    private final DeviceMapper deviceMapper;
    private final WebSocketPushService webSocketPushService;
    private final DeviceSessionManager deviceSessionManager;

    @Override
    public void reportState(DeviceStateReportReqVO reqVO) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, reqVO.getChipId())
        );

        if (device == null) {
            throw new ServiceException("设备不存在，请先添加设备");
        }

        if (reqVO.getIp() != null) {
            device.setIp(reqVO.getIp());
        }
        if (reqVO.getDeviceType() != null) {
            device.setDeviceType(reqVO.getDeviceType());
        }
        if (reqVO.getBrightness() != null) {
            device.setBrightness(reqVO.getBrightness());
        }
        if (reqVO.getTemp() != null) {
            device.setTemp(reqVO.getTemp());
        }
        if (reqVO.getAutoMode() != null) {
            device.setAutoMode(reqVO.getAutoMode());
        }
        if (reqVO.getRecommendedBrightness() != null) {
            device.setRecommendedBrightness(reqVO.getRecommendedBrightness());
        }
        if (reqVO.getRecommendedTemp() != null) {
            device.setRecommendedTemp(reqVO.getRecommendedTemp());
        }
        if (reqVO.getFabric() != null) {
            device.setFabric(reqVO.getFabric());
        }
        if (reqVO.getMainColorRgb() != null) {
            device.setMainColorRgb(reqVO.getMainColorRgb());
        }

        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        // 设备已经走 ws/device 注册过，这里刷新 lastSeen
        deviceSessionManager.touch(reqVO.getChipId());

        DeviceRespVO respVO = DeviceConvert.convert(device);
        webSocketPushService.pushState(respVO);
    }
}