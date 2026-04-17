package com.genius.smartlight.service.device;

import com.genius.smartlight.vo.device.DeviceRespVO;
import com.genius.smartlight.vo.device.DeviceSaveReqVO;

import java.util.List;

public interface DeviceService {

    Long createDevice(DeviceSaveReqVO reqVO);

    void updateDevice(Long id, DeviceSaveReqVO reqVO);

    void deleteDevice(Long id);

    DeviceRespVO getDevice(Long id);

    List<DeviceRespVO> getDeviceList(); // 管理员看全量

    DeviceRespVO getDeviceByChipId(String chipId);

    List<DeviceRespVO> getCurrentUserDeviceList();

    void bindDeviceToCurrentStore(String chipId, String displayName);
}