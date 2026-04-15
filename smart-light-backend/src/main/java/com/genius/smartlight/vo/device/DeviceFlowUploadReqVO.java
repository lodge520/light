package com.genius.smartlight.vo.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "人流上传开关请求")
@Data
public class DeviceFlowUploadReqVO {

    @Schema(description = "是否开启人流上传", example = "true")
    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;
}