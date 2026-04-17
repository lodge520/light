package com.genius.smartlight.vo.auth;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RegisterReqVO {
    private String username;
    private String password;
    private String confirmPassword;

    private String storeName;
    private String storeStyle;
    private BigDecimal area;
    private String province;
    private String city;
}