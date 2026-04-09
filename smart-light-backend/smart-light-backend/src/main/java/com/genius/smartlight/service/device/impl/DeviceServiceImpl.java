package com.genius.smartlight.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.convert.device.DeviceConvert;
import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.dal.mysql.DeviceMapper;
import com.genius.smartlight.service.device.DeviceService;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceSaveReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    @Override
    public Long createDevice(DeviceSaveReqVO reqVO) {
        DeviceDO exist = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getDeviceCode, reqVO.getDeviceCode())
        );
        if (exist != null) {
            throw new ServiceException("设备编码已存在");
        }

        DeviceDO device = DeviceConvert.convert(reqVO);
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());

        deviceMapper.insert(device);
        return device.getId();
    }

    @Override
    public void updateDevice(Long id, DeviceSaveReqVO reqVO) {
        DeviceDO device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }

        DeviceDO updateObj = DeviceConvert.convert(reqVO);
        updateObj.setId(id);
        updateObj.setCreateTime(device.getCreateTime());
        updateObj.setUpdateTime(LocalDateTime.now());

        deviceMapper.updateById(updateObj);
    }

    @Override
    public void deleteDevice(Long id) {
        DeviceDO device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        deviceMapper.deleteById(id);
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
    public DeviceRespVO getDeviceByCode(String deviceCode) {
        DeviceDO device = deviceMapper.selectOne(
                new LambdaQueryWrapper<DeviceDO>()
                        .eq(DeviceDO::getDeviceCode, deviceCode)
        );
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return DeviceConvert.convert(device);
    }
}