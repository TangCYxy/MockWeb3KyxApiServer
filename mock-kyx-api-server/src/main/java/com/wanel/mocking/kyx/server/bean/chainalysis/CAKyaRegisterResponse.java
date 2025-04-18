package com.wanel.mocking.kyx.server.bean.chainalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response model for Chainalysis KYA (Know Your Address) registration
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CAKyaRegisterResponse {
    private String updatedAt;
    private String asset;
    private String network;
    private String address;
    private String attemptIdentifier;
    private BigDecimal usdAmount;
    private BigDecimal assetAmount;
    private String externalId;
} 