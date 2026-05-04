package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Firmware channel update request")
@Data
public class DeviceFirmwareChannelReqVO {

    @Schema(description = "Firmware channel stable/test", example = "stable")
    @NotBlank(message = "Firmware channel cannot be empty")
    private String channel;
}
