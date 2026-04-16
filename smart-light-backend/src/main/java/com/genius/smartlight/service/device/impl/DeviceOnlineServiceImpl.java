package com.genius.smartlight.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.service.device.DeviceOnlineService;
import com.genius.smartlight.vo.device.DeviceOnlineStatusRespVO;
import com.genius.smartlight.websocket.DeviceSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceOnlineServiceImpl implements DeviceOnlineService {

    private final DeviceMapper deviceMapper;
    private final DeviceSessionManager deviceSessionManager;

    @Override
    public DeviceOnlineStatusRespVO getOnlineStatus(String chipId) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getChipId, chipId)
        );
        if (device == null) {
            throw new ServiceException("设备不存在");
        }

        DeviceOnlineStatusRespVO respVO = new DeviceOnlineStatusRespVO();
        respVO.setChipId(device.getChipId());
        respVO.setIp(device.getIp());
        respVO.setOnline(deviceSessionManager.isOnline(chipId));
        respVO.setLastSeen(deviceSessionManager.getLastSeen(chipId));
        return respVO;
    }

    @Override
    public List<DeviceOnlineStatusRespVO> getOnlineStatusList() {
        List<DeviceDO> devices = deviceMapper.selectList(null);

        return devices.stream().map(device -> {
            DeviceOnlineStatusRespVO respVO = new DeviceOnlineStatusRespVO();
            respVO.setChipId(device.getChipId());
            respVO.setIp(device.getIp());
            respVO.setOnline(deviceSessionManager.isOnline(device.getChipId()));
            respVO.setLastSeen(deviceSessionManager.getLastSeen(device.getChipId()));
            return respVO;
        }).toList();
    }
}