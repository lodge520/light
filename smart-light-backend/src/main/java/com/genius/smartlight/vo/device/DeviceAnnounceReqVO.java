package com.genius.smartlight.vo.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceAnnounceReqVO {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    private String ip;
}