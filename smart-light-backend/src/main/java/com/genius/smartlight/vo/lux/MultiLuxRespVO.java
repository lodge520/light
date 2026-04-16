package com.genius.smartlight.vo.lux;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "多设备光照趋势响应")
@Data
public class MultiLuxRespVO {

    @Schema(description = "时间标签")
    private List<String> labels;

    @Schema(description = "多设备数据集")
    private List<Dataset> datasets;

    @Data
    @Schema(description = "单个设备光照数据集")
    public static class Dataset {

        @Schema(description = "芯片ID", example = "ABC123456")
        private String label;

        @Schema(description = "光照序列")
        private List<Double> data;
    }
}