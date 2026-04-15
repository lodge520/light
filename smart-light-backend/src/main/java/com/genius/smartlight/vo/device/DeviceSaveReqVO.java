package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "设备新增/更新请求")
@Data
public class DeviceSaveReqVO {

    @Schema(description = "设备编码", example = "device001")
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @Schema(description = "设备IP地址", example = "192.168.1.10")
    private String ip;

    @Schema(description = "亮度，范围 0-100", example = "80")
    @Min(value = 0, message = "亮度不能小于0")
    @Max(value = 100, message = "亮度不能大于100")
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