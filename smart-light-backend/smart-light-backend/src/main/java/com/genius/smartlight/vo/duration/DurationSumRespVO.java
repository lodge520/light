package com.genius.smartlight.vo.duration;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DurationSumRespVO {

    private String deviceCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalDuration;
}