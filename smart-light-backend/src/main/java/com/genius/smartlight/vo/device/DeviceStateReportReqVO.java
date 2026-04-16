package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "设备状态上报请求")
@Data
public class DeviceStateReportReqVO {

    @Schema(description = "芯片唯一ID", example = "ABC123456")
    @NotBlank(message = "芯片ID不能为空")
    private String chipId;

    @Schema(description = "设备类型 lamp/camlamp", example = "lamp")
    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    @Schema(description = "设备IP地址", example = "192.168.1.10")
    private String ip;

    @Schema(description = "当前亮度", example = "80")
    private Integer brightness;

    @Schema(description = "当前色温", example = "4500")
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