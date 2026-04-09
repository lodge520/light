package com.genius.smartlight.vo.duration;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DurationRespVO {

    private Long id;
    private String deviceCode;
    private LocalDate statDate;
    private Long durationValue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}