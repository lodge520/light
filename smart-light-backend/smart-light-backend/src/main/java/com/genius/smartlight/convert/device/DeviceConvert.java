package com.genius.smartlight.convert.device;

import com.genius.smartlight.dal.dataobject.DeviceDO;
import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceSaveReqVO;

public class DeviceConvert {

    public static DeviceDO convert(DeviceSaveReqVO reqVO) {
        DeviceDO device = new DeviceDO();
        device.setDeviceCode(reqVO.getDeviceCode());
        device.setIp(reqVO.getIp());
        device.setBrightness(reqVO.getBrightness());
        device.setTemp(reqVO.getTemp());
        device.setAutoMode(reqVO.getAutoMode());
        device.setRecommendedBrightness(reqVO.getRecommendedBrightness());
        device.setRecommendedTemp(reqVO.getRecommendedTemp());
        device.setFabric(reqVO.getFabric());
        device.setMainColorRgb(reqVO.getMainColorRgb());
        return device;
    }

    public static DeviceRespVO convert(DeviceDO device) {
        DeviceRespVO respVO = new DeviceRespVO();
        respVO.setId(device.getId());
        respVO.setDeviceCode(device.getDeviceCode());
        respVO.setIp(device.getIp());
        respVO.setBrightness(device.getBrightness());
        respVO.setTemp(device.getTemp());
        respVO.setAutoMode(device.getAutoMode());
        respVO.setRecommendedBrightness(device.getRecommendedBrightness());
        respVO.setRecommendedTemp(device.getRecommendedTemp());
        respVO.setFabric(device.getFabric());
        respVO.setMainColorRgb(device.getMainColorRgb());
        respVO.setCreateTime(device.getCreateTime());
        respVO.setUpdateTime(device.getUpdateTime());
        return respVO;
    }
}