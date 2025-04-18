package com.wanel.kyx.provider.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {"code":1,"message":"ok","result":{"cybercrime":"0","money_laundering":"0","number_of_malicious_contracts_created":"0","gas_abuse":"0","financial_crime":"0","darkweb_transactions":"0","reinit":"0","phishing_activities":"0","fake_kyc":"0","blacklist_doubt":"0","fake_standard_interface":"0","data_source":"SlowMist,BlockSec","stealing_attack":"1","blackmail_activities":"0","sanctioned":"0","malicious_mining_activities":"0","mixer":"0","honeypot_related_address":"0"}}
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class GoPlusRiskEoaAddressResponse {

    private int code;
    private String message;
    private Result result;

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
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
            if (StringUtils.equals(IN_RISK, cybercrime)) {
                risks.add("cybercrime");
            }
            if (StringUtils.equals(IN_RISK, money_laundering)) {
                risks.add("money_laundering");
            }
            if (StringUtils.equals(IN_RISK, number_of_malicious_contracts_created)) {
                risks.add("number_of_malicious_contracts_created");
            }
            if (StringUtils.equals(IN_RISK, gas_abuse)) {
                risks.add("gas_abuse");
            }
            if (StringUtils.equals(IN_RISK, financial_crime)) {
                risks.add("financial_crime");
            }
            if (StringUtils.equals(IN_RISK, darkweb_transactions)) {
                risks.add("darkweb_transactions");
            }
            if (StringUtils.equals(IN_RISK, reinit)) {
                risks.add("reinit");
            }
            if (StringUtils.equals(IN_RISK, phishing_activities)) {
                risks.add("phishing_activities");
            }
            if (StringUtils.equals(IN_RISK, fake_kyc)) {
                risks.add("fake_kyc");
            }
            if (StringUtils.equals(IN_RISK, blacklist_doubt)) {
                risks.add("blacklist_doubt");
            }
            if (StringUtils.equals(IN_RISK, fake_standard_interface)) {
                risks.add("fake_standard_interface");
            }
//            if (StringUtils.equals(IN_RISK, data_source) ) {risks.add("data_source");}
            if (StringUtils.equals(IN_RISK, stealing_attack)) {
                risks.add("stealing_attack");
            }
            if (StringUtils.equals(IN_RISK, blackmail_activities)) {
                risks.add("blackmail_activities");
            }
            if (StringUtils.equals(IN_RISK, sanctioned)) {
                risks.add("sanctioned");
            }
            if (StringUtils.equals(IN_RISK, malicious_mining_activities)) {
                risks.add("malicious_mining_activities");
            }
            if (StringUtils.equals(IN_RISK, mixer)) {
                risks.add("mixer");
            }
            if (StringUtils.equals(IN_RISK, honeypot_related_address)) {
                risks.add("honeypot_related_address");
            }
            return risks;
        }

        public boolean isInRisk() {
            return riskItems().size() > 0;
        }

        public String riskDetail() {
            return StringUtils.join(riskItems().stream().filter((v) -> StringUtils.isNotEmpty(StringUtils.trim(v))).collect(Collectors.toList()), "\n");
        }
    }

    public boolean isInRisk() {
        return code > 0 && result != null && result.isInRisk();
    }
}
