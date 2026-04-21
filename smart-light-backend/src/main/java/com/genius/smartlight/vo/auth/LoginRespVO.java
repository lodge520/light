package com.genius.smartlight.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "账号登录响应结果")
public class LoginRespVO {

    @Schema(description = "登录令牌 Token", example = "eyJhbGciOiJIUzI1NiJ9.xxx")
    private String token;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "店铺ID", example = "1001")
    private Long storeId;

    @Schema(description = "店铺名称", example = "智慧服装体验店")
    private String storeName;

    @Schema(description = "店铺风格", example = "简约现代")
    private String storeStyle;

    @Schema(description = "省份", example = "湖南省")
    private String province;

    @Schema(description = "城市", example = "长沙市")
    private String city;

    @Schema(description = "是否已完成店铺配置", example = "true")
    private Boolean storeConfigured;
}