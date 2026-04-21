package com.genius.smartlight.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "账号注册请求参数")
public class RegisterReqVO {

    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "确认密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "店铺名称", example = "智慧服装体验店", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "店铺名称不能为空")
    private String storeName;

    @Schema(description = "店铺风格", example = "简约现代")
    private String storeStyle;

    @Schema(description = "店铺面积，单位：平方米", example = "120")
    @DecimalMin(value = "0.0", inclusive = false, message = "店铺面积必须大于0")
    private BigDecimal area;

    @Schema(description = "省份", example = "湖南省")
    private String province;

    @Schema(description = "城市", example = "长沙市")
    private String city;
}