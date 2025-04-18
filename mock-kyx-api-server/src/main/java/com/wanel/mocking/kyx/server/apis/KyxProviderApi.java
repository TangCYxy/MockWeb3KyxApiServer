package com.wanel.mocking.kyx.server.apis;

import org.springframework.http.ResponseEntity;

import com.wanel.mocking.kyx.server.bean.TransactionCheckRequest;

/**
 * Common interface for KYX provider APIs
 */
public interface KyxProviderApi {

    /**
     * Check if a transaction is risky
     * 
     * @param request Transaction check request
     * @return Response containing risk check result
     */
    ResponseEntity<?> checkTransaction(TransactionCheckRequest request);
} 