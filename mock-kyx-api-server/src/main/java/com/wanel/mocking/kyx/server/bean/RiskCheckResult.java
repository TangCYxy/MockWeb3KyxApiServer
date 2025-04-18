package com.wanel.mocking.kyx.server.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common response model for risk check result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckResult {

    private boolean inRisk;
    private String riskDetail;
    
    // Additional properties might be needed based on actual provider responses
} 