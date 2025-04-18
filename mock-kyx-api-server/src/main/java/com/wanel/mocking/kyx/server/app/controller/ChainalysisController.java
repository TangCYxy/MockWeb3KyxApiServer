package com.wanel.mocking.kyx.server.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.wanel.mocking.kyx.server.apis.KyxProviderApi;
import com.wanel.mocking.kyx.server.bean.RiskCheckResult;
import com.wanel.mocking.kyx.server.bean.TransactionCheckRequest;
import com.wanel.mocking.kyx.server.bean.chainalysis.CAKyXAlertMonitorResponse;
import com.wanel.mocking.kyx.server.bean.chainalysis.CAKyXAlertResponse;
import com.wanel.mocking.kyx.server.bean.chainalysis.CAKyaRegisterResponse;
import com.wanel.mocking.kyx.server.bean.chainalysis.CAKyaRequest;
import com.wanel.mocking.kyx.server.bean.chainalysis.CAKytRegisterResponse;
import com.wanel.mocking.kyx.server.bean.chainalysis.CAKytRequest;
import com.wanel.mocking.kyx.server.core.service.RiskCheckService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for Chainalysis API endpoints
 */
@RestController
@Slf4j
@ConditionalOnProperty(name = "kyx.providers[1].enabled", havingValue = "true")
public class ChainalysisController implements KyxProviderApi {

    private final RiskCheckService riskCheckService;
    private final Map<String, RiskCheckResult> registrationResults = new ConcurrentHashMap<>();

    @Autowired
    public ChainalysisController(RiskCheckService riskCheckService) {
        this.riskCheckService = riskCheckService;
    }

    /**
     * Legacy endpoint for simple transaction checks
     */
    @PostMapping("/check")
    @Override
    public ResponseEntity<?> checkTransaction(@Valid @RequestBody TransactionCheckRequest request) {
        log.info("Received Chainalysis check request: {}", request);
        
        RiskCheckResult result = riskCheckService.checkRisk(request);
        
        // Convert to Chainalysis specific response format
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        
        Map<String, Object> result_data = new HashMap<>();
        result_data.put("risk_detected", result.isInRisk());
        result_data.put("risk_details", result.getRiskDetail());
        response.put("result", result_data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Step 1: Register an address for KYA (Know Your Address) check
     */
    @PostMapping("/kya/register")
    public ResponseEntity<CAKyaRegisterResponse> registerKya(@Valid @RequestBody CAKyaRequest request) {
        log.info("Received Chainalysis KYA register request: {}", request);
        
        // Generate external ID
        String externalId = UUID.randomUUID().toString();
        
        // Store the registration for later checks
        Map<String, Object> params = new HashMap<>();
        params.put("targetAddress", request.getTargetAddress());
        params.put("chainId", request.getChainId());
        
        // Execute risk check and store result
        RiskCheckResult result = riskCheckService.checkRisk(params);
        registrationResults.put(externalId, result);
        
        // Format current time as ISO timestamp
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        CAKyaRegisterResponse response = CAKyaRegisterResponse.builder()
                .updatedAt(timestamp)
                .address(request.getTargetAddress())
                .asset(request.getAssetName() != null ? request.getAssetName() : "ETH")
                .network(mapChainIdToNetwork(request.getChainId()))
                .assetAmount(request.getAssetAmount() != null ? request.getAssetAmount() : BigDecimal.ONE)
                .attemptIdentifier(request.getIdentifier())
                .externalId(externalId)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Step 2: Check status of a KYA registration
     */
    @GetMapping("/kya/register/{externalId}")
    public ResponseEntity<CAKyaRegisterResponse> checkKyaRegistration(@PathVariable String externalId) {
        log.info("Received Chainalysis KYA registration check for externalId: {}", externalId);
        
        // If the registration exists, return an updated timestamp
        if (registrationResults.containsKey(externalId)) {
            // Format current time as ISO timestamp
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            
            CAKyaRegisterResponse response = CAKyaRegisterResponse.builder()
                    .updatedAt(timestamp)
                    .externalId(externalId)
                    .build();
            
            return ResponseEntity.ok(response);
        }
        
        // If not found, return empty response
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Step 3: Get alerts for a registered address
     */
    @GetMapping("/kya/alerts/{externalId}")
    public ResponseEntity<CAKyXAlertResponse> getKyaAlerts(@PathVariable String externalId) {
        log.info("Received Chainalysis KYA alerts request for externalId: {}", externalId);
        
        CAKyXAlertResponse response = new CAKyXAlertResponse();
        
        // If the registration exists and is risky, return alerts
        if (registrationResults.containsKey(externalId) && registrationResults.get(externalId).isInRisk()) {
            RiskCheckResult result = registrationResults.get(externalId);
            
            CAKyXAlertResponse.Alert alert = CAKyXAlertResponse.Alert.builder()
                    .alertLevel("HIGH")
                    .category("money_laundering_fraud")
                    .service("Mock KYX Server")
                    .externalId(externalId)
                    .alertAmount(BigDecimal.valueOf(1000))
                    .exposureType("DIRECT")
                    .build();
            
            response.setAlerts(Collections.singletonList(alert));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Step 1: Register a transaction for KYT (Know Your Transaction) check
     */
    @PostMapping("/kyt/register")
    public ResponseEntity<CAKytRegisterResponse> registerKyt(@Valid @RequestBody CAKytRequest request) {
        log.info("Received Chainalysis KYT register request: {}", request);
        
        // Generate external ID
        String externalId = UUID.randomUUID().toString();
        
        // Store the registration for later checks
        Map<String, Object> params = new HashMap<>();
        params.put("fromAddress", request.getFromAddress());
        params.put("toAddress", request.getToAddress());
        params.put("tokenName", request.getTokenName());
        params.put("tokenAmount", request.getTokenAmount());
        params.put("chainId", request.getChainId());
        
        if (request.getTxHash() != null) {
            params.put("txHash", request.getTxHash());
        }
        
        // Execute risk check and store result
        RiskCheckResult result = riskCheckService.checkRisk(params);
        registrationResults.put(externalId, result);
        
        // Format current time as ISO timestamp
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        BigDecimal assetAmount = BigDecimal.valueOf(
                request.getTokenAmount() != null ? request.getTokenAmount() : 0.0
        );
        
        CAKytRegisterResponse response = CAKytRegisterResponse.builder()
                .updatedAt(timestamp)
                .asset(request.getTokenName())
                .network(mapChainIdToNetwork(request.getChainId()))
                .transferReference("tx:" + request.getToAddress())
                .tx(request.getTxHash() != null ? request.getTxHash() : UUID.randomUUID().toString())
                .idx(BigInteger.ZERO)
                .usdAmount(assetAmount.multiply(BigDecimal.valueOf(1000))) // Mock USD conversion
                .assetAmount(assetAmount)
                .timestamp(timestamp)
                .outputAddress(request.getToAddress())
                .externalId(externalId)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Step 2: Get alerts for a registered transaction
     */
    @GetMapping("/kyt/alerts/{externalId}")
    public ResponseEntity<CAKyXAlertResponse> getKytAlerts(@PathVariable String externalId) {
        log.info("Received Chainalysis KYT alerts request for externalId: {}", externalId);
        
        return getKyaAlerts(externalId); // Reuse the KYA alerts method as they have the same response format
    }
    
    /**
     * Step 3: Monitor alerts for registered entities
     */
    @GetMapping("/monitoring")
    public ResponseEntity<CAKyXAlertMonitorResponse> monitorAlerts() {
        log.info("Received Chainalysis monitoring request");
        
        CAKyXAlertMonitorResponse response = CAKyXAlertMonitorResponse.builder()
                .limit(10)
                .offset(0)
                .total(0)
                .data(new ArrayList<>())
                .build();
        
        // Add alert results for any risky transactions/addresses
        for (Map.Entry<String, RiskCheckResult> entry : registrationResults.entrySet()) {
            if (entry.getValue().isInRisk()) {
                String externalId = entry.getKey();
                
                CAKyXAlertMonitorResponse.AlertResult alert = CAKyXAlertMonitorResponse.AlertResult.builder()
                        .alertAmountUsd(BigDecimal.valueOf(1000))
                        .category("money_laundering_fraud")
                        .transactionHash(UUID.randomUUID().toString())
                        .transferReference("tx:0x1234567890")
                        .exposureType("DIRECT")
                        .transferReportedAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                        .alertIdentifier(UUID.randomUUID().toString())
                        .direction("SENT")
                        .build();
                
                response.getData().add(alert);
                response.setTotal(response.getTotal() + 1);
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Map chain ID to network name
     */
    private String mapChainIdToNetwork(Integer chainId) {
        if (chainId == null) {
            return "ethereum";
        }
        
        switch (chainId) {
            case 1:
                return "ethereum";
            case 56:
                return "bsc";
            case 137:
                return "polygon";
            default:
                return "ethereum";
        }
    }
} 