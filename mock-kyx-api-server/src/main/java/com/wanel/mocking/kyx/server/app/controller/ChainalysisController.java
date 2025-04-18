package com.wanel.mocking.kyx.server.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
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
    
    // Map to store registration params for later risk checks when alerts are requested
    private final Map<String, Map<String, Object>> registrationParams = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    // Default expiration time is 1 hour (in milliseconds)
    @Value("${chainalysis.registration.expiration-time-ms:3600000}")
    private long expirationTimeMs;

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
     * Scheduled task to clean up expired registration entries
     * Runs every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    public void cleanupExpiredRegistrations() {
        log.info("Running scheduled cleanup of expired registrations");
        long currentTime = Instant.now().toEpochMilli();
        int removedCount = 0;
        
        Iterator<Map.Entry<String, Map<String, Object>>> iterator = registrationParams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Map<String, Object>> entry = iterator.next();
            Map<String, Object> params = entry.getValue();
            
            if (params.containsKey("expiresAt")) {
                long expiresAt = (long) params.get("expiresAt");
                if (currentTime > expiresAt) {
                    iterator.remove();
                    removedCount++;
                    log.debug("Removed expired registration with externalId: {}", entry.getKey());
                }
            }
        }
        
        if (removedCount > 0) {
            log.info("Cleaned up {} expired registration entries", removedCount);
        }
    }
    
    /**
     * Step 1: Register an address for KYA (Know Your Address) check
     * POST /api/kyt/v2/users/{userId}/withdrawal-attempts
     * 
     * This just registers the request - no risk check is performed here
     */
    @PostMapping("/api/kyt/v2/users/{userId}/withdrawal-attempts")
    public ResponseEntity<CAKyaRegisterResponse> registerKya(
            @PathVariable("userId") String userId,
            @Valid @RequestBody CAKyaRequest request) {
        log.info("Received Chainalysis KYA register request for userId: {}, request: {}", userId, request);
        
        // Generate external ID
        String externalId = UUID.randomUUID().toString();
        
        // Store the parameters for later risk check when alerts are requested
        Map<String, Object> params = new HashMap<>();
        params.put("targetAddress", request.getTargetAddress());
        params.put("chainId", request.getChainId());
        params.put("requestType", "kya"); // Add type for reference
        
        // Add expiration timestamp
        long expiresAt = Instant.now().toEpochMilli() + expirationTimeMs;
        params.put("expiresAt", expiresAt);
        
        // Generate a random delay between 0-10 seconds
        int delaySeconds = random.nextInt(11); // 0-10 seconds
        log.info("Generated random delay of {} seconds for KYA request {}", delaySeconds, externalId);
        
        // Format current time as ISO timestamp
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        // Build the response with the registration data
        CAKyaRegisterResponse.Builder responseBuilder = CAKyaRegisterResponse.builder()
                .address(request.getTargetAddress())
                .asset(request.getAssetName() != null ? request.getAssetName() : "ETH")
                .network(mapChainIdToNetwork(request.getChainId()))
                .assetAmount(request.getAssetAmount() != null ? request.getAssetAmount() : BigDecimal.ONE)
                .attemptIdentifier(request.getIdentifier())
                .externalId(externalId);
        
        // If delay is 0, set updatedAt immediately
        if (delaySeconds == 0) {
            responseBuilder.updatedAt(timestamp);
            params.put("updatedAt", timestamp);
            log.info("Immediately setting updatedAt for KYA request {}", externalId);
        } else {
            // Otherwise, compute the valid timestamp for later checks
            long validTimestamp = Instant.now().getEpochSecond() + delaySeconds;
            params.put("validTimestamp", validTimestamp);
            log.info("Setting validTimestamp {} for KYA request {}", validTimestamp, externalId);
        }
        
        // Store parameters for later use - NO risk check performed here
        registrationParams.put(externalId, params);
        
        CAKyaRegisterResponse response = responseBuilder.build();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Step 2: Check status of a KYA registration
     * GET /api/kyt/v2/withdrawal-attempts/{externalId}
     * 
     * Just returns registration status - no risk check performed
     */
    @GetMapping("/api/kyt/v2/withdrawal-attempts/{externalId}")
    public ResponseEntity<CAKyaRegisterResponse> checkKyaRegistration(@PathVariable("externalId") String externalId) {
        log.info("Received Chainalysis KYA registration check for externalId: {}", externalId);
        
        // If the registration exists, check if it's ready
        if (registrationParams.containsKey(externalId)) {
            Map<String, Object> params = registrationParams.get(externalId);
            
            // Current timestamp for checking and response
            long currentTimestamp = Instant.now().getEpochSecond();
            String currentTimeString = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            
            // Build the response
            CAKyaRegisterResponse.Builder responseBuilder = CAKyaRegisterResponse.builder()
                    .externalId(externalId);
            
            // Case 1: Already has updatedAt set
            if (params.containsKey("updatedAt")) {
                responseBuilder.updatedAt((String) params.get("updatedAt"));
                log.info("Returning existing updatedAt for KYA request {}", externalId);
            } 
            // Case 2: Has validTimestamp and current time is after it
            else if (params.containsKey("validTimestamp")) {
                long validTimestamp = (long) params.get("validTimestamp");
                
                if (currentTimestamp >= validTimestamp) {
                    responseBuilder.updatedAt(currentTimeString);
                    params.put("updatedAt", currentTimeString); // Store for future requests
                    log.info("Setting updatedAt now that validTimestamp has passed for KYA request {}", externalId);
                }
                // If current time is not after validTimestamp, leave updatedAt unset
            }
            
            return ResponseEntity.ok(responseBuilder.build());
        }
        
        // If not found, return empty response
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Step 3: Get alerts for a registered address
     * GET /api/kyt/v2/withdrawal-attempts/{externalId}/alerts
     * 
     * This is where the actual risk check is performed
     */
    @GetMapping("/api/kyt/v2/withdrawal-attempts/{externalId}/alerts")
    public ResponseEntity<CAKyXAlertResponse> getKyaAlerts(@PathVariable("externalId") String externalId) {
        log.info("Received Chainalysis KYA alerts request for externalId: {}", externalId);
        
        CAKyXAlertResponse response = new CAKyXAlertResponse();
        
        // If the registration exists, perform risk check now
        if (registrationParams.containsKey(externalId)) {
            Map<String, Object> params = registrationParams.get(externalId);
            
            // Perform risk check when alerts are requested
            RiskCheckResult result = riskCheckService.checkRisk(params);
            
            if (result.isInRisk()) {
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
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Step 1: Register a transaction for KYT (Know Your Transaction) check
     * POST /api/kyt/v2/users/{userId}/transfers
     * 
     * This just registers the request - no risk check is performed here
     */
    @PostMapping("/api/kyt/v2/users/{userId}/transfers")
    public ResponseEntity<CAKytRegisterResponse> registerKyt(
            @PathVariable("userId") String userId,
            @Valid @RequestBody CAKytRequest request) {
        log.info("Received Chainalysis KYT register request for userId: {}, request: {}", userId, request);
        
        // Generate external ID
        String externalId = UUID.randomUUID().toString();
        
        // Store the parameters for later risk check when alerts are requested
        Map<String, Object> params = new HashMap<>();
        params.put("fromAddress", request.getFromAddress());
        params.put("toAddress", request.getToAddress());
        params.put("tokenName", request.getTokenName());
        params.put("tokenAmount", request.getTokenAmount());
        params.put("chainId", request.getChainId());
        params.put("requestType", "kyt"); // Add type for reference
        
        if (request.getTxHash() != null) {
            params.put("txHash", request.getTxHash());
        }
        
        // Add expiration timestamp
        long expiresAt = Instant.now().toEpochMilli() + expirationTimeMs;
        params.put("expiresAt", expiresAt);
        
        // Generate a random delay between 0-10 seconds
        int delaySeconds = random.nextInt(11); // 0-10 seconds
        log.info("Generated random delay of {} seconds for KYT request {}", delaySeconds, externalId);
        
        // Format current time as ISO timestamp
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        // Calculate asset amount for response
        BigDecimal assetAmount = BigDecimal.valueOf(
                request.getTokenAmount() != null ? request.getTokenAmount() : 0.0
        );
        
        // Build the response with the registration data
        CAKytRegisterResponse.Builder responseBuilder = CAKytRegisterResponse.builder()
                .asset(request.getTokenName())
                .network(mapChainIdToNetwork(request.getChainId()))
                .transferReference("tx:" + request.getToAddress())
                .tx(request.getTxHash() != null ? request.getTxHash() : UUID.randomUUID().toString())
                .idx(BigInteger.ZERO)
                .usdAmount(assetAmount.multiply(BigDecimal.valueOf(1000))) // Mock USD conversion
                .assetAmount(assetAmount)
                .timestamp(timestamp)
                .outputAddress(request.getToAddress())
                .externalId(externalId);
        
        // If delay is 0, set updatedAt immediately
        if (delaySeconds == 0) {
            responseBuilder.updatedAt(timestamp);
            params.put("updatedAt", timestamp);
            log.info("Immediately setting updatedAt for KYT request {}", externalId);
        } else {
            // Otherwise, compute the valid timestamp for later checks
            long validTimestamp = Instant.now().getEpochSecond() + delaySeconds;
            params.put("validTimestamp", validTimestamp);
            log.info("Setting validTimestamp {} for KYT request {}", validTimestamp, externalId);
        }
        
        // Store parameters for later use - NO risk check performed here
        registrationParams.put(externalId, params);
        
        CAKytRegisterResponse response = responseBuilder.build();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Step 2: Check status of a KYT registration
     * GET /api/kyt/v2/transfers/{externalId}
     * 
     * Just returns registration status - no risk check performed
     */
    @GetMapping("/api/kyt/v2/transfers/{externalId}")
    public ResponseEntity<CAKytRegisterResponse> checkKytRegistration(@PathVariable("externalId") String externalId) {
        log.info("Received Chainalysis KYT registration check for externalId: {}", externalId);
        
        // If the registration exists, check if it's ready
        if (registrationParams.containsKey(externalId)) {
            Map<String, Object> params = registrationParams.get(externalId);
            
            // Current timestamp for checking and response
            long currentTimestamp = Instant.now().getEpochSecond();
            String currentTimeString = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            
            // Build the response
            CAKytRegisterResponse.Builder responseBuilder = CAKytRegisterResponse.builder()
                    .externalId(externalId);
            
            // Case 1: Already has updatedAt set
            if (params.containsKey("updatedAt")) {
                responseBuilder.updatedAt((String) params.get("updatedAt"));
                log.info("Returning existing updatedAt for KYT request {}", externalId);
            } 
            // Case 2: Has validTimestamp and current time is after it
            else if (params.containsKey("validTimestamp")) {
                long validTimestamp = (long) params.get("validTimestamp");
                
                if (currentTimestamp >= validTimestamp) {
                    responseBuilder.updatedAt(currentTimeString);
                    params.put("updatedAt", currentTimeString); // Store for future requests
                    log.info("Setting updatedAt now that validTimestamp has passed for KYT request {}", externalId);
                }
                // If current time is not after validTimestamp, leave updatedAt unset
            }
            
            return ResponseEntity.ok(responseBuilder.build());
        }
        
        // If not found, return empty response
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Step 3: Get alerts for a registered transaction
     * GET /api/kyt/v2/transfers/{externalId}/alerts
     * 
     * This is where the actual risk check is performed
     */
    @GetMapping("/api/kyt/v2/transfers/{externalId}/alerts")
    public ResponseEntity<CAKyXAlertResponse> getKytAlerts(@PathVariable("externalId") String externalId) {
        log.info("Received Chainalysis KYT alerts request for externalId: {}", externalId);
        
        CAKyXAlertResponse response = new CAKyXAlertResponse();
        
        // If the registration exists, perform risk check now
        if (registrationParams.containsKey(externalId)) {
            Map<String, Object> params = registrationParams.get(externalId);
            
            // Perform risk check when alerts are requested
            RiskCheckResult result = riskCheckService.checkRisk(params);
            
            if (result.isInRisk()) {
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
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Monitor alerts for registered entities
     * GET /api/kyt/v1/alerts
     */
    @GetMapping("/api/kyt/v1/alerts")
    public ResponseEntity<CAKyXAlertMonitorResponse> monitorAlerts(
            @RequestParam(name = "createdAt_lte", required = false) String endTime,
            @RequestParam(name = "createdAt_gte", required = false) String startTime,
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        log.info("Received Chainalysis monitoring request with params: start={}, end={}, limit={}, offset={}", 
                startTime, endTime, limit, offset);
        
        CAKyXAlertMonitorResponse response = CAKyXAlertMonitorResponse.builder()
                .limit(limit)
                .offset(offset)
                .total(0)
                .data(new ArrayList<>())
                .build();
        
        // For each registered entity, perform a risk check if not done already
        for (Map.Entry<String, Map<String, Object>> entry : registrationParams.entrySet()) {
            String externalId = entry.getKey();
            Map<String, Object> params = entry.getValue();
            
            // Perform risk check for monitoring
            RiskCheckResult result = riskCheckService.checkRisk(params);
            
            if (result.isInRisk()) {
                CAKyXAlertMonitorResponse.AlertResult alert = CAKyXAlertMonitorResponse.AlertResult.builder()
                        .alertAmountUsd(BigDecimal.valueOf(1000))
                        .category("money_laundering_fraud")
                        .transactionHash(UUID.randomUUID().toString())
                        .transferReference("tx:" + (params.containsKey("toAddress") ? params.get("toAddress") : "0x1234567890"))
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