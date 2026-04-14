package com.genius.smartlight.vo.device;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceSaveReqVO {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    private String ip;

    @Min(value = 0, message = "亮度不能小于0")
    @Max(value = 100, message = "亮度不能大于100")
    private Integer brightness;

    private Integer temp;

    private Boolean autoMode;

    private Integer recommendedBrightness;

    private Integer recommendedTemp;

    private String fabric;

    private String mainColorRgb;
}