package com.wanel.kyx.provider.model.chainalysis.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CAKyaRegisterResponse implements Serializable {
    private String updatedAt;
    private String asset;
    private String network;
    private String address;
    private String attemptIdentifier;
    private BigDecimal usdAmount;
    private BigDecimal assetAmount;
    private String externalId;
}
