package com.genius.smartlight.service.device;

import com.genius.smartlight.vo.device.DeviceOtaFirmwareRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DeviceOtaFirmwareService {

    DeviceOtaFirmwareRespVO uploadFirmware(
            MultipartFile file,
            String deviceType,
            String channel,
            String version,
            Integer versionCode,
            String changelog,
            String md5,
            String host
    );

    List<DeviceOtaFirmwareRespVO> listFirmware(String deviceType, String channel);
}
