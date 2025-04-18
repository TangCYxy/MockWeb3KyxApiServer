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
 * Request model for Chainalysis KYA (Know Your Address)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CAKyaRequest {
    
    @NotBlank(message = "targetAddress cannot be empty")
    private String targetAddress;
    
    @NotNull(message = "chainId cannot be null")
    private Integer chainId;
    
    @Builder.Default
    private String requestHash = UUID.randomUUID().toString();
    
    @Builder.Default
    private String identifier = UUID.randomUUID().toString();
    
    private String chainName;
    private String assetName;
    private BigDecimal assetAmount;
} 