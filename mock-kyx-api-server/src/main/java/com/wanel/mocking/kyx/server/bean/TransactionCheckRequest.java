package com.wanel.mocking.kyx.server.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Common request model for transaction risk check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCheckRequest {

    @NotBlank(message = "fromAddress cannot be empty")
    private String fromAddress;
    
    @NotBlank(message = "toAddress cannot be empty")
    private String toAddress;
    
    @NotBlank(message = "tokenName cannot be empty")
    private String tokenName;
    
    @NotNull(message = "tokenAmount cannot be null")
    private Double tokenAmount;
    
    @NotNull(message = "chainId cannot be null")
    private Integer chainId;
    
    private String txHash;
} 