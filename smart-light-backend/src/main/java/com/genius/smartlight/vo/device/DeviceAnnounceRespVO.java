package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "设备上线通告响应")
@Data
public class DeviceAnnounceRespVO {

    @Schema(description = "设备是否已存在于系统中", example = "true")
    private Boolean added;
}