package com.genius.smartlight.vo.device;

import lombok.Data;

@Data
public class DeviceOnlineStatusRespVO {

    private String deviceCode;
    private String ip;
    private Boolean online;
    private Long lastSeen;
}