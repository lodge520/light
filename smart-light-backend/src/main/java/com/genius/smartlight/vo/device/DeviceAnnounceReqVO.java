package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "设备上线通告请求")
@Data
public class DeviceAnnounceReqVO {

    @Schema(description = "设备编码", example = "device001")
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @Schema(description = "设备IP地址", example = "192.168.1.10")
    private String ip;
}