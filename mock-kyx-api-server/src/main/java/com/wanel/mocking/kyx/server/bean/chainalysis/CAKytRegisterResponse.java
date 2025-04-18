package com.wanel.mocking.kyx.server.bean.chainalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Response model for Chainalysis KYT (Know Your Transaction) registration
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CAKytRegisterResponse {
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