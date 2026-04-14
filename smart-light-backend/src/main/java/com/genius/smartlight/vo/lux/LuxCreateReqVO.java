package com.genius.smartlight.vo.lux;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LuxCreateReqVO {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @NotNull(message = "光照值不能为空")
    private Double luxValue;

    private LocalDateTime collectTime;
}