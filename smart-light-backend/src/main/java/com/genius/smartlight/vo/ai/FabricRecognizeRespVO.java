package com.genius.smartlight.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "面料识别响应")
@Data
public class FabricRecognizeRespVO {

    @Schema(description = "识别结果标签", example = "cotton")
    private String label;

    @Schema(description = "识别置信度", example = "0.92")
    private Double confidence;

    @Schema(description = "服装主色 RGB 值", example = "120,80,60")
    private String mainColorRgb;

    @Schema(description = "推荐亮度", example = "72")
    private Integer recommendedBrightness;

    @Schema(description = "推荐色温", example = "4300")
    private Integer recommendedTemp;

    @Schema(description = "是否检测到服装区域")
    private Boolean clothDetected;

    @Schema(description = "服装框x坐标")
    private Integer clothX;

    @Schema(description = "服装框y坐标")
    private Integer clothY;

    @Schema(description = "服装框宽度")
    private Integer clothW;

    @Schema(description = "服装框高度")
    private Integer clothH;

    @Schema(description = "服装透明背景PNG Base64，用于主色提取")
    private String clothMaskedPngBase64;

    @Schema(description = "带SegFormer分割结果的标注图Base64")
    private String annotatedImageBase64;
}