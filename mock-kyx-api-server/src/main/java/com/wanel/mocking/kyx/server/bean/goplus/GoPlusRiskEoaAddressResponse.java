package com.wanel.mocking.kyx.server.bean.goplus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response model for GoPlus risk EOA address check
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoPlusRiskEoaAddressResponse {

    private int code;
    private String message;
    private Result result;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result {
        private static final String IN_RISK = "1";
        private String cybercrime;
        private String money_laundering;
        private String number_of_malicious_contracts_created;
        private String gas_abuse;
        private String financial_crime;
        private String darkweb_transactions;
        private String reinit;
        private String phishing_activities;
        private String fake_kyc;
        private String blacklist_doubt;
        private String fake_standard_interface;
        private String data_source;
        private String stealing_attack;
        private String blackmail_activities;
        private String sanctioned;
        private String malicious_mining_activities;
        private String mixer;
        private String honeypot_related_address;

        public List<String> riskItems() {
            ArrayList<String> risks = new ArrayList<>();
            if (IN_RISK.equals(cybercrime)) {
                risks.add("cybercrime");
            }
            if (IN_RISK.equals(money_laundering)) {
                risks.add("money_laundering");
            }
            if (IN_RISK.equals(number_of_malicious_contracts_created)) {
                risks.add("number_of_malicious_contracts_created");
            }
            if (IN_RISK.equals(gas_abuse)) {
                risks.add("gas_abuse");
            }
            if (IN_RISK.equals(financial_crime)) {
                risks.add("financial_crime");
            }
            if (IN_RISK.equals(darkweb_transactions)) {
                risks.add("darkweb_transactions");
            }
            if (IN_RISK.equals(reinit)) {
                risks.add("reinit");
            }
            if (IN_RISK.equals(phishing_activities)) {
                risks.add("phishing_activities");
            }
            if (IN_RISK.equals(fake_kyc)) {
                risks.add("fake_kyc");
            }
            if (IN_RISK.equals(blacklist_doubt)) {
                risks.add("blacklist_doubt");
            }
            if (IN_RISK.equals(fake_standard_interface)) {
                risks.add("fake_standard_interface");
            }
            if (IN_RISK.equals(stealing_attack)) {
                risks.add("stealing_attack");
            }
            if (IN_RISK.equals(blackmail_activities)) {
                risks.add("blackmail_activities");
            }
            if (IN_RISK.equals(sanctioned)) {
                risks.add("sanctioned");
            }
            if (IN_RISK.equals(malicious_mining_activities)) {
                risks.add("malicious_mining_activities");
            }
            if (IN_RISK.equals(mixer)) {
                risks.add("mixer");
            }
            if (IN_RISK.equals(honeypot_related_address)) {
                risks.add("honeypot_related_address");
            }
            return risks;
        }

        public boolean isInRisk() {
            return !riskItems().isEmpty();
        }

        public String riskDetail() {
            return String.join("\n", riskItems().stream()
                    .filter(v -> v != null && !v.trim().isEmpty())
                    .collect(Collectors.toList()));
        }
    }

    public boolean isInRisk() {
        return code > 0 && result != null && result.isInRisk();
    }
} 