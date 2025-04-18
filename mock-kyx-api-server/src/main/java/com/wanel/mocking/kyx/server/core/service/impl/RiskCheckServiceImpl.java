package com.wanel.mocking.kyx.server.core.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wanel.mocking.kyx.server.app.config.KyxConfiguration;
import com.wanel.mocking.kyx.server.bean.RiskCheckResult;
import com.wanel.mocking.kyx.server.bean.TransactionCheckRequest;
import com.wanel.mocking.kyx.server.core.service.PythonScriptExecutor;
import com.wanel.mocking.kyx.server.core.service.RiskCheckService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RiskCheckServiceImpl implements RiskCheckService {

    private final PythonScriptExecutor pythonScriptExecutor;
    private final KyxConfiguration kyxConfiguration;

    @Autowired
    public RiskCheckServiceImpl(PythonScriptExecutor pythonScriptExecutor, KyxConfiguration kyxConfiguration) {
        this.pythonScriptExecutor = pythonScriptExecutor;
        this.kyxConfiguration = kyxConfiguration;
    }

    @Override
    public RiskCheckResult checkRisk(TransactionCheckRequest request) {
        log.info("Checking risk for transaction: {}", request);
        
        Map<String, Object> params = new HashMap<>();
        params.put("fromAddress", request.getFromAddress());
        params.put("toAddress", request.getToAddress());
        params.put("tokenName", request.getTokenName());
        params.put("tokenAmount", request.getTokenAmount());
        params.put("chainId", request.getChainId());
        
        if (request.getTxHash() != null) {
            params.put("txHash", request.getTxHash());
        }
        
        return executeRiskCheck(params);
    }
    
    @Override
    public RiskCheckResult checkRisk(Map<String, Object> params) {
        log.info("Checking risk with parameters: {}", params);
        return executeRiskCheck(params);
    }
    
    private RiskCheckResult executeRiskCheck(Map<String, Object> params) {
        Map<String, Object> result = pythonScriptExecutor.executeFunction(
            kyxConfiguration.getPython().getFunctionName(), 
            params
        );
        
        return RiskCheckResult.builder()
            .inRisk(Boolean.TRUE.equals(result.get("inRisk")))
            .riskDetail(result.get("riskDetail") != null ? result.get("riskDetail").toString() : "")
            .build();
    }
} 