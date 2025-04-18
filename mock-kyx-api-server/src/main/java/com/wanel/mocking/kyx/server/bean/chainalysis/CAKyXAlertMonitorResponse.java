package com.wanel.mocking.kyx.server.bean.chainalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response model for Chainalysis KYX alert monitor
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CAKyXAlertMonitorResponse {
    private int limit;
    private int offset;
    private int total;
    @Builder.Default
    private List<AlertResult> data = new ArrayList<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AlertResult {
        private BigDecimal alertAmountUsd;
        private String category;
        private String transactionHash;
        private String transferReference;
        private String exposureType;
        private String transferReportedAt;
        private String alertIdentifier;
        private String direction;

        public boolean isSent() {
            return "SENT".equals(direction);
        }

        public String getAddress() {
            if (transferReference != null && transferReference.contains(":")) {
                String[] split = transferReference.split(":");
                if (split.length > 1) {
                    return split[1];
                }
            }
            return transferReference;
        }
    }
} 