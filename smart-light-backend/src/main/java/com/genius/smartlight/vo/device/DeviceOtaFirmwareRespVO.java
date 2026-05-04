package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Uploaded OTA firmware response")
@Data
public class DeviceOtaFirmwareRespVO {

    private Long id;

    private String deviceType;

    private String channel;

    private String version;

    private Integer versionCode;

    private String fileUrl;

    private String md5;

    private String changelog;

    private Boolean enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
