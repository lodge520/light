package com.genius.smartlight.vo.auth;

import lombok.Data;

@Data
public class LoginRespVO {
    private String token;

    private Long userId;
    private String username;

    private Long storeId;
    private String storeName;
    private String storeStyle;
    private String province;
    private String city;

    private Boolean storeConfigured;

}