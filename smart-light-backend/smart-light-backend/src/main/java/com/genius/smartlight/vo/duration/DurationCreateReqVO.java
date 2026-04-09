package com.genius.smartlight.vo.duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DurationCreateReqVO {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @NotNull(message = "统计日期不能为空")
    private LocalDate statDate;

    @NotNull(message = "停留时长不能为空")
    private Long durationValue;
}