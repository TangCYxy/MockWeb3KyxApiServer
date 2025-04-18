package com.wanel.kyx.provider.model.chainalysis.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CAKytRegisterResponse implements Serializable {
    private String updatedAt;
    private String asset;
    private String network;
    private String transferReference;
    private String tx;
    private BigInteger idx;
    private BigDecimal usdAmount;
    private BigDecimal assetAmount;
    private String timestamp;
    private String outputAddress;
    private String externalId;
}
