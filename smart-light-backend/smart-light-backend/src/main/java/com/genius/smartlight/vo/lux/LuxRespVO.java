package com.genius.smartlight.vo.lux;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LuxRespVO {

    private Long id;
    private String deviceCode;
    private Double luxValue;
    private LocalDateTime collectTime;
    private LocalDateTime createTime;
}