package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "设备在线状态响应")
@Data
public class DeviceOnlineStatusRespVO {

    @Schema(description = "设备编码", example = "device001")
    private String deviceCode;

    @Schema(description = "设备IP地址", example = "192.168.1.10")
    private String ip;

    @Schema(description = "是否在线", example = "true")
    private Boolean online;

    @Schema(description = "最近一次心跳时间戳，毫秒", example = "1713062400000")
    private Long lastSeen;
}