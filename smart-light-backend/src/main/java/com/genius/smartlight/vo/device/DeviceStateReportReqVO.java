package com.genius.smartlight.vo.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceStateReportReqVO {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    private String ip;

    private Integer brightness;

    private Integer temp;

    private Boolean autoMode;

    private Integer recommendedBrightness;

    private Integer recommendedTemp;

    private String fabric;

    private String mainColorRgb;
}