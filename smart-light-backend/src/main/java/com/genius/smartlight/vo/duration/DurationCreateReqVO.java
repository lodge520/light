package com.genius.smartlight.vo.duration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "停留时长新增请求")
@Data
public class DurationCreateReqVO {

    @Schema(description = "芯片ID", example = "ABC123456")
    @NotBlank(message = "芯片ID不能为空")
    private String chipId;

    @Schema(description = "统计日期", example = "2026-04-14")
    @NotNull(message = "统计日期不能为空")
    private LocalDate statDate;

    @Schema(description = "停留时长，单位毫秒", example = "12000")
    @NotNull(message = "停留时长不能为空")
    private Long durationValue;
}