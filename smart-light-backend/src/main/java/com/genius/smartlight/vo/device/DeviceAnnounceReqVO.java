package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "设备上线通告请求")
@Data
public class DeviceAnnounceReqVO {

    @Schema(description = "芯片ID", example = "ABC123456")
    @NotBlank(message = "芯片ID不能为空")
    private String chipId;

    @Schema(description = "设备IP地址", example = "192.168.1.10")
    private String ip;

    @Schema(description = "设备類型", example = "lamp")
    private String deviceType;
}