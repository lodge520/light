package com.genius.smartlight.vo.device;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceRespVO {

    private Long id;
    private String deviceCode;
    private String ip;
    private Integer brightness;
    private Integer temp;
    private Boolean autoMode;
    private Integer recommendedBrightness;
    private Integer recommendedTemp;
    private String fabric;
    private String mainColorRgb;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}