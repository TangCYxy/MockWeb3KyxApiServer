package com.wanel.mocking.kyx.server.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wanel.mocking.kyx.server.apis.KyxProviderApi;
import com.wanel.mocking.kyx.server.bean.RiskCheckResult;
import com.wanel.mocking.kyx.server.bean.TransactionCheckRequest;
import com.wanel.mocking.kyx.server.bean.goplus.GoPlusRiskEoaAddressResponse;
import com.wanel.mocking.kyx.server.core.service.RiskCheckService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for GoPlus API endpoints
 */
@RestController
@Slf4j
@ConditionalOnProperty(name = "kyx.providers[0].enabled", havingValue = "true")
public class GoPlusController implements KyxProviderApi {

    private final RiskCheckService riskCheckService;

    @Autowired
    public GoPlusController(RiskCheckService riskCheckService) {
        this.riskCheckService = riskCheckService;
    }

    /**
     * Implementation of the KyxProviderApi interface method.
     * GoPlus doesn't actually have this endpoint, but we implement it to satisfy the interface.
     */
    @Override
    public ResponseEntity<?> checkTransaction(@Valid @RequestBody TransactionCheckRequest request) {
        log.info("Received transaction check request through interface method: {}", request);
        
        // This is not a real GoPlus endpoint, but we implement it to satisfy the interface
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Not implemented in GoPlus. Use address-based endpoints instead.");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if an address is risky (KYA - Know Your Address)
     * This endpoint follows the GoPlus EOA address risk check API format
     */
    @GetMapping("/address/{address}")
    public ResponseEntity<GoPlusRiskEoaAddressResponse> checkAddress(@PathVariable String address) {
        log.info("Received GoPlus address check request for address: {}", address);
        
        // Create a request with the address as targetAddress
        Map<String, Object> params = new HashMap<>();
        params.put("targetAddress", address);
        
        // Check if the address is risky
        RiskCheckResult result = riskCheckService.checkRisk(params);
        
        // Build the GoPlus response format
        GoPlusRiskEoaAddressResponse.Result resultData = GoPlusRiskEoaAddressResponse.Result.builder()
                .build();
        
        if (result.isInRisk()) {
            // Set one of the risk indicators based on the detail
            if (result.getRiskDetail().contains("money laundry")) {
                resultData.setMoney_laundering("1");
            } else {
                resultData.setCybercrime("1");
            }
            
            // Set data source
            resultData.setData_source("Mock KYX Server");
        } else {
            // No risk - set all values to "0"
            resultData.setCybercrime("0");
            resultData.setMoney_laundering("0");
            resultData.setNumber_of_malicious_contracts_created("0");
            resultData.setGas_abuse("0");
            resultData.setFinancial_crime("0");
            resultData.setDarkweb_transactions("0");
            resultData.setReinit("0");
            resultData.setPhishing_activities("0");
            resultData.setFake_kyc("0");
            resultData.setBlacklist_doubt("0");
            resultData.setFake_standard_interface("0");
            resultData.setStealing_attack("0");
            resultData.setBlackmail_activities("0");
            resultData.setSanctioned("0");
            resultData.setMalicious_mining_activities("0");
            resultData.setMixer("0");
            resultData.setHoneypot_related_address("0");
            resultData.setData_source("Mock KYX Server");
        }
        
        GoPlusRiskEoaAddressResponse response = GoPlusRiskEoaAddressResponse.builder()
                .code(1) // 1 = success
                .message("ok")
                .result(resultData)
                .build();
        
        return ResponseEntity.ok(response);
    }
} 