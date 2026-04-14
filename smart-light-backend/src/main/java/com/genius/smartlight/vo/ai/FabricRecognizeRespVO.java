package com.genius.smartlight.vo.ai;

import lombok.Data;

@Data
public class FabricRecognizeRespVO {

    private String label;
    private Double confidence;
}