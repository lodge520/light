package com.genius.smartlight.vo.store;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StoreSaveReqVO {
    private String storeName;
    private String storeStyle;
    private BigDecimal area;
    private String province;
    private String city;
}