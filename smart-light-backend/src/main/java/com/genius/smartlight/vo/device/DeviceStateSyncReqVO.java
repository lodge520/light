package com.genius.smartlight.vo.device;

import lombok.Data;

@Data
public class DeviceStateSyncReqVO {

    private Integer brightness;

    private Integer temp;

    private Boolean autoMode;

    private Integer recommendedBrightness;

    private Integer recommendedTemp;

    private String fabric;

    private String mainColorRgb;
}
