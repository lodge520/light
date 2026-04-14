package com.genius.smartlight.vo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PersonDetectRespVO {

    private Integer count;
    private Double confidence;
    private String timestamp;

    @JsonProperty("processing_time")
    private Double processingTime;

    @JsonProperty("annotated_image_base64")
    private String annotatedImageBase64;
}