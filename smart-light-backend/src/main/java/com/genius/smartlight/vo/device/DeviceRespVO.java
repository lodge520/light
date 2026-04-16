package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "设备信息响应")
@Data
public class DeviceRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "芯片唯一ID", example = "ABC123456")
    private String chipId;

    @Schema(description = "设备类型", example = "lamp")
    private String deviceType;

    @Schema(description = "店内编号", example = "1")
    private String deviceNo;

    @Schema(description = "展示名称", example = "橱窗灯1")
    private String displayName;

    @Schema(description = "设备IP地址", example = "192.168.1.10")
    private String ip;

    @Schema(description = "亮度", example = "80")
    private Integer brightness;

    @Schema(description = "色温", example = "4500")
    private Integer temp;

    @Schema(description = "是否自动模式", example = "true")
    private Boolean autoMode;

    @Schema(description = "推荐亮度", example = "75")
    private Integer recommendedBrightness;

    @Schema(description = "推荐色温", example = "5000")
    private Integer recommendedTemp;

    @Schema(description = "识别出的面料类型", example = "cotton")
    private String fabric;

    @Schema(description = "主颜色RGB值", example = "255,200,120")
    private String mainColorRgb;

    @Schema(description = "创建时间", example = "2026-04-14T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-04-14T11:00:00")
    private LocalDateTime updateTime;
}