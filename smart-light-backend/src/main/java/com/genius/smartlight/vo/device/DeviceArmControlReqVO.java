package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "设备云台控制请求")
@Data
public class DeviceArmControlReqVO {

    @Schema(description = "云台转动方向", example = "left")
    @NotBlank(message = "方向不能为空")
    private String direction;
}