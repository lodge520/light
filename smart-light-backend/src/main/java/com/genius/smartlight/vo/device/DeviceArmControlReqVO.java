package com.genius.smartlight.vo.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceArmControlReqVO {

    @NotBlank(message = "方向不能为空")
    private String direction;
}