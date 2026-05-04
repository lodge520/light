package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "设备云台控制请求")
@Data
public class DeviceArmControlReqVO {

    @Schema(description = "云台动作", example = "left")
    private String action;

    @Schema(description = "旧字段：云台转动方向，action 为空时使用", example = "left")
    private String direction;

    @Schema(description = "动作速度 slow/normal/fast", example = "normal")
    private String speed;

    @Schema(description = "滑轨位置，单位 mm，仅 slider_position 使用", example = "120")
    private Integer position;
}
