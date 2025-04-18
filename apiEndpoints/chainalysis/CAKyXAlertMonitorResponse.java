package com.wanel.kyx.provider.model.chainalysis.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CAKyXAlertMonitorResponse implements Serializable {
    private int limit;
    private int offset;
    private int total;
    private List<AlertResult> data = new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
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
            return StringUtils.equals(direction, "SENT");
        }

        public String getAddress() {
            String[] split = StringUtils.split(transferReference, ":");
            if (split.length > 1) {
                return split[1];
            } else {
                return transferReference;
            }
        }
    }
}
