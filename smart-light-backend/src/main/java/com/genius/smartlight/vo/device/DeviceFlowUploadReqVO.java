package com.genius.smartlight.vo.device;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceFlowUploadReqVO {

    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;
}