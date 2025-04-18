package com.wanel.mocking.kyx.server.core.service;

import java.util.Map;

import com.wanel.mocking.kyx.server.bean.RiskCheckResult;
import com.wanel.mocking.kyx.server.bean.TransactionCheckRequest;

/**
 * Service interface for performing risk checks
 */
public interface RiskCheckService {

    /**
     * Check if a transaction is risky
     * 
     * @param request Transaction check request
     * @return Risk check result
     */
    RiskCheckResult checkRisk(TransactionCheckRequest request);
    
    /**
     * Check if a transaction or address is risky using generic parameters
     * 
     * @param params Map of parameters
     * @return Risk check result
     */
    RiskCheckResult checkRisk(Map<String, Object> params);
} 