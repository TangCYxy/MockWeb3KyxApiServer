package com.wanel.kyx.provider.model.chainalysis.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CAKyXAlertResponse implements Serializable {
    private List<Alert> alerts = new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Alert {
        private String alertLevel;
        private String category;
        private String service;
        private String externalId;
        private BigDecimal alertAmount;
        private String exposureType;
    }
}
