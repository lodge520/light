package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "设备状态同步请求")
@Data
public class DeviceStateSyncReqVO {

    @Schema(description = "亮度", example = "80")
    private Integer brightness;

    @Schema(description = "色温", example = "4500")
    private Integer temp;

    @Schema(description = "是否自动模式", example = "true")
    private Boolean autoMode;

    @Schema(description = "推荐亮度", example = "75")
    private Integer recommendedBrightness;

    @Schema(description = "推荐色温", example = "5000")
    private Integer recommendedTemp;

    @Schema(description = "识别出的面料类型", example = "cotton")
    private String fabric;

    @Schema(description = "主颜色RGB值", example = "255,200,120")
    private String mainColorRgb;
}