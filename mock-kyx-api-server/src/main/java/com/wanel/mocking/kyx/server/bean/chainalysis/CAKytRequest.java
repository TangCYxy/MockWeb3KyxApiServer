package com.wanel.mocking.kyx.server.bean.chainalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request model for Chainalysis KYT (Know Your Transaction)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CAKytRequest {
    
    @NotBlank(message = "fromAddress cannot be empty")
    private String fromAddress;
    
    @NotBlank(message = "toAddress cannot be empty")
    private String toAddress;
    
    @NotNull(message = "chainId cannot be null")
    private Integer chainId;
    
    @NotBlank(message = "tokenName cannot be empty")
    private String tokenName;
    
    @NotNull(message = "tokenAmount cannot be null")
    private Double tokenAmount;
    
    private String txHash;
    
    @Builder.Default
    private String requestHash = UUID.randomUUID().toString();
    
    private String chainName;
    private BigDecimal assetAmount;
} 