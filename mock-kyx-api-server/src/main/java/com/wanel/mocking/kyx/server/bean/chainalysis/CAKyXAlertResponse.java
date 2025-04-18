package com.wanel.mocking.kyx.server.bean.chainalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response model for Chainalysis KYX alert
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CAKyXAlertResponse {
    @Builder.Default
    private List<Alert> alerts = new ArrayList<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Alert {
        private String alertLevel;
        private String category;
        private String service;
        private String externalId;
        private BigDecimal alertAmount;
        private String exposureType;
    }
} 