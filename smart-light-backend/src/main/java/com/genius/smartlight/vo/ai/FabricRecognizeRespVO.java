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
}