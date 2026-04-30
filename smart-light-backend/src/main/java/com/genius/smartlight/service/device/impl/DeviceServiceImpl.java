package com.genius.smartlight.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.convert.device.DeviceConvert;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.dataobject.StoreDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.dal.mysql.StoreMapper;
import com.genius.smartlight.security.SecurityUtils;
import com.genius.smartlight.service.device.DeviceService;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceSaveReqVO;
import com.genius.smartlight.vo.device.LightEffectReqVO;
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final WebSocketPushService webSocketPushService;
    private final DeviceMapper deviceMapper;
    private final StoreMapper storeMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(DeviceSaveReqVO reqVO) {
        Long userId = SecurityUtils.getCurrentUserId();

        StoreDO store = storeMapper.selectOne(
                new LambdaQueryWrapper<StoreDO>()
                        .eq(StoreDO::getUserId, userId)
        );
        if (store == null) {
            throw new ServiceException("当前用户未绑定店铺");
        }

        DeviceDO exist = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, reqVO.getChipId())
        );

        LocalDateTime now = LocalDateTime.now();

        if (exist != null) {
            if (exist.getStoreId() != null && !exist.getStoreId().equals(store.getId())) {
                throw new ServiceException("该设备已被其他店铺绑定");
            }

            if (exist.getStoreId() != null && exist.getStoreId().equals(store.getId())) {
                throw new ServiceException("该设备已添加到当前店铺");
            }

            exist.setStoreId(store.getId());
            exist.setDeviceType(reqVO.getDeviceType());
            exist.setDeviceNo(reqVO.getDeviceNo());
            exist.setDisplayName(reqVO.getDisplayName());
            exist.setIp(reqVO.getIp());
            exist.setBrightness(reqVO.getBrightness());
            exist.setTemp(reqVO.getTemp());
            exist.setAutoMode(reqVO.getAutoMode());
            exist.setRecommendedBrightness(reqVO.getRecommendedBrightness());
            exist.setRecommendedTemp(reqVO.getRecommendedTemp());
            exist.setFabric(reqVO.getFabric());
            exist.setMainColorRgb(reqVO.getMainColorRgb());
            exist.setUpdateTime(now);

            deviceMapper.updateById(exist);

            DeviceRespVO respVO = DeviceConvert.convert(exist);
            webSocketPushService.pushState(respVO);

            return exist.getId();
        }

        DeviceDO device = DeviceConvert.convert(reqVO);
        device.setStoreId(store.getId());
        device.setCreateTime(now);
        device.setUpdateTime(now);

        deviceMapper.insert(device);

        DeviceRespVO respVO = DeviceConvert.convert(device);
        webSocketPushService.pushState(respVO);

        return device.getId();
    }

    @Override
    public void updateDevice(Long id, DeviceSaveReqVO reqVO) {
        DeviceDO device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }

        if (!device.getChipId().equals(reqVO.getChipId())) {
            DeviceDO exist = deviceMapper.selectOne(
                    new LambdaQueryWrapper<DeviceDO>()
                            .eq(DeviceDO::getChipId, reqVO.getChipId())
            );
            if (exist != null) {
                throw new ServiceException("芯片ID已存在");
            }
        }

        DeviceDO updateObj = DeviceConvert.convert(reqVO);
        updateObj.setId(id);
        updateObj.setCreateTime(device.getCreateTime());
        updateObj.setUpdateTime(LocalDateTime.now());
        updateObj.setStoreId(device.getStoreId());

        // 改这里：不要用旧 displayName 覆盖
        updateObj.setDisplayName(reqVO.getDisplayName());

        deviceMapper.updateById(updateObj);

        DeviceRespVO respVO = DeviceConvert.convert(updateObj);

        webSocketPushService.pushState(respVO);
        webSocketPushService.pushStateToDevice(updateObj.getChipId(), respVO);
    }

    @Override
    public void deleteDevice(Long id) {
        DeviceDO device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }

        notifyDeviceResumeBroadcast(device);
        deviceMapper.deleteById(id);
        webSocketPushService.pushDeviceDeleted(id);
    }

    private void notifyDeviceResumeBroadcast(DeviceDO device) {
        String ip = device.getIp();

        if (ip == null || ip.trim().isEmpty()) {
            System.out.println("设备 IP 为空，跳过恢复广播指令");
            return;
        }

        try {
            String baseUrl = ip.startsWith("http://") || ip.startsWith("https://")
                    ? ip
                    : "http://" + ip;
            String url = baseUrl + "/resumeBroadcast";

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            System.out.println("已通知设备恢复广播：" + url + "，响应：" + response.body());

        } catch (Exception e) {
            System.out.println("通知设备恢复广播失败：" + e.getMessage());
        }
    }

    @Override
    public DeviceRespVO getDevice(Long id) {
        DeviceDO device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return DeviceConvert.convert(device);
    }

    @Override
    public List<DeviceRespVO> getDeviceList() {
        List<DeviceDO> list = deviceMapper.selectList(null);
        return list.stream().map(DeviceConvert::convert).toList();
    }

    @Override
    public DeviceRespVO getDeviceByChipId(String chipId) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
        );
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return DeviceConvert.convert(device);
    }

    @Override
    public List<DeviceRespVO> getCurrentUserDeviceList() {
        Long userId = SecurityUtils.getCurrentUserId();

        StoreDO store = storeMapper.selectOne(
                new LambdaQueryWrapper<StoreDO>()
                        .eq(StoreDO::getUserId, userId)
        );
        if (store == null) {
            throw new ServiceException("当前用户未绑定店铺");
        }

        List<DeviceDO> list = deviceMapper.selectList(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getStoreId, store.getId())
                        .orderByDesc(DeviceDO::getId)
        );

        return list.stream().map(DeviceConvert::convert).toList();
    }

    @Override
    public void bindDeviceToCurrentStore(String chipId, String displayName) {
        Long userId = SecurityUtils.getCurrentUserId();

        StoreDO store = storeMapper.selectOne(
                new LambdaQueryWrapper<StoreDO>()
                        .eq(StoreDO::getUserId, userId)
        );
        if (store == null) {
            throw new ServiceException("当前用户未绑定店铺");
        }

        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
        );
        if (device == null) {
            throw new ServiceException("设备不存在");
        }

        device.setStoreId(store.getId());

        if (displayName != null && !displayName.isBlank()) {
            device.setDisplayName(displayName);
        }

        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        webSocketPushService.pushState(DeviceConvert.convert(device));
    }

    @Override
    public boolean locateDevice(String chipId) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "locate");
        msg.put("times", 3);
        msg.put("duration", 1200);

        boolean sent = webSocketPushService.pushRawToDevice(chipId, msg.toString());

        if (!sent) {
            throw new ServiceException("设备离线，无法定位");
        }

        return true;
    }

    @Override
    public void sendLightEffect(String chipId, LightEffectReqVO reqVO) {
        ObjectNode msg = objectMapper.createObjectNode();

        msg.put("type", "lightEffect");
        msg.put("effect", reqVO.getEffect() == null ? "wave" : reqVO.getEffect());
        msg.put("enabled", reqVO.getEnabled() == null || reqVO.getEnabled());

        if (reqVO.getBaseTemp() != null) {
            msg.put("baseTemp", reqVO.getBaseTemp());
        }
        if (reqVO.getRange() != null) {
            msg.put("range", reqVO.getRange());
        }
        if (reqVO.getSpeed() != null) {
            msg.put("speed", reqVO.getSpeed());
        }
        if (reqVO.getBrightness() != null) {
            msg.put("brightness", reqVO.getBrightness());
        }
        if (reqVO.getPhaseIndex() != null) {
            msg.put("phaseIndex", reqVO.getPhaseIndex());
        }
        if (reqVO.getPhaseGap() != null) {
            msg.put("phaseGap", reqVO.getPhaseGap());
        }

        boolean sent = webSocketPushService.pushRawToDevice(chipId, msg.toString());

        if (!sent) {
            throw new ServiceException("设备离线，灯效下发失败");
        }
    }
}