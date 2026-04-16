package com.genius.smartlight.vo.lux;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "光照新增请求")
@Data
public class LuxCreateReqVO {

    @Schema(description = "芯片ID", example = "ABC123456")
    @NotBlank(message = "芯片ID不能为空")
    private String chipId;

    @Schema(description = "光照值", example = "356.5")
    @NotNull(message = "光照值不能为空")
    private Double luxValue;

    @Schema(description = "采集时间", example = "2026-04-14T10:30:00")
    private LocalDateTime collectTime;
}