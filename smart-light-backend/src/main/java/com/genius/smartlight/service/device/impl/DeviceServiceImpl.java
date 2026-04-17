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
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final WebSocketPushService webSocketPushService;
    private final DeviceMapper deviceMapper;
    private final StoreMapper storeMapper;

    @Override
    public Long createDevice(DeviceSaveReqVO reqVO) {
        DeviceDO exist = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, reqVO.getChipId())
        );
        if (exist != null) {
            throw new ServiceException("芯片ID已存在");
        }

        DeviceDO device = DeviceConvert.convert(reqVO);
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());

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

        // 关键：保留原 storeId，避免后台更新设备时把店铺归属冲掉
        updateObj.setStoreId(device.getStoreId());

        // 如果你 DeviceDO 有 displayName，也顺便保留
        updateObj.setDisplayName(device.getDisplayName());

        deviceMapper.updateById(updateObj);

        webSocketPushService.pushState(DeviceConvert.convert(updateObj));
    }

    @Override
    public void deleteDevice(Long id) {
        DeviceDO device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }

        deviceMapper.deleteById(id);
        webSocketPushService.pushDeviceDeleted(id);
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
}