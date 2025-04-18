package com.wanel.kyx.provider.service.kya.impl.chainalysis;

import com.wanel.common.utils.DateUtils;
import com.wanel.common.utils.ObjectUtils;
import com.wanel.kyx.provider.configurer.KyxProperties;
import com.wanel.kyx.provider.consts.WanelKyxConst;
import com.wanel.kyx.provider.dto.dto.RiskEoaAddressDTO;
import com.wanel.kyx.provider.model.chainalysis.request.CAKyaRegisterPayload;
import com.wanel.kyx.provider.model.chainalysis.request.CAKyaRequest;
import com.wanel.kyx.provider.model.chainalysis.response.CAKyXAlertResponse;
import com.wanel.kyx.provider.model.chainalysis.response.CAKyaRegisterResponse;
import com.wanel.kyx.provider.model.request.KyaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Component
public class ChainalysisKyaService {
    private final KyxProperties kyxProperties;

    final @Qualifier("kyaChainalysisHttpClient") OkHttpClient httpClient;

    /**
     * 如果失败，直接抛出异常
     * 不能直接返回null, 可能会导致无效query
     *
     * @param queryRequest
     * @return
     */
    private CAKyaRegisterResponse registerKya(CAKyaRequest queryRequest) throws IOException {
        CAKyaRegisterPayload payload = CAKyaRegisterPayload.builder()
                .address(queryRequest.getTargetAddress())
                .asset(queryRequest.getAssetName())
                .network(queryRequest.getChainName())
                .assetAmount(queryRequest.getAssetAmount())
                .attemptTimestamp(DateUtils.formatTime2(LocalDateTime.now().minusHours(8)))
                .attemptIdentifier(queryRequest.getIdentifier()).build();
        //
        Request request = new Request.Builder()
                .url(kyxProperties.getChainalysis().buildKyaRegisterUrl(queryRequest.getRequestHash()))
                .post(kyxProperties.getChainalysis().buildKyaRegisterBody(payload))
                .addHeader(KyxProperties.Chainalysis.HEADER_TOKEN, kyxProperties.getChainalysis().getToken())
                .build();
        Response response = httpClient.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            return ObjectUtils.fromJson(response.body().string(), CAKyaRegisterResponse.class);
        } else {
            log.error("registerKya returns {}", response.body() == null ? "null" : response.body().string());
            return null;
        }
    }

    public RiskEoaAddressDTO doKya(CAKyaRequest request) throws Exception {
        // register kya
        CAKyaRegisterResponse registerResponse = registerKya(request);
        if (registerResponse == null || StringUtils.isEmpty(registerResponse.getExternalId())) {
            log.error("chainalysis doKya got invalid response for request {} on chain {}", request.getTargetAddress(), request.getChainId());
            return null;
        }
        log.info("chainalysis doKya get externalId {} for address {} on chain {}",
                registerResponse.getExternalId(), request.getTargetAddress(), request.getChainId());
        if (StringUtils.isEmpty(registerResponse.getUpdatedAt())) {
            // 反复check
            registerResponse.setUpdatedAt(blockedCheckKya(request, registerResponse.getExternalId()));
            if (StringUtils.isEmpty(registerResponse.getUpdatedAt())) {
                log.error("chainalysis doKya can not get valid register updatedAt for request {} on chain {}", request.getTargetAddress(), request.getChainId());
                return null;
            }
        }
        // check alert
        return kyaAlert(request, registerResponse.getExternalId());
    }

    private RiskEoaAddressDTO kyaAlert(CAKyaRequest queryRequest, String externalId) {
        try {
            //
            Request request = new Request.Builder()
                    .url(kyxProperties.getChainalysis().buildKyaAlertUrl(externalId))
                    .addHeader(KyxProperties.Chainalysis.HEADER_TOKEN, kyxProperties.getChainalysis().getToken())
                    .get()
                    .build();
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return parseKyaAlertResponse(queryRequest, response.body().string());
            } else {
                log.error("kyaAlert returns {}", response.body() == null ? "null" : response.body().string());
                return null;
            }
        } catch (Exception e) {
            log.error("kyaAlert exception for request {}", queryRequest.getTargetAddress(), e);
            return null;
        }
    }

    private RiskEoaAddressDTO parseKyaAlertResponse(CAKyaRequest queryRequest, String responseStr) {
        CAKyXAlertResponse response = ObjectUtils.fromJson(responseStr, CAKyXAlertResponse.class);
        if (response == null) {
            return null;
        }
        RiskEoaAddressDTO result = RiskEoaAddressDTO.builder()
                .chainId(queryRequest.getChainId())
                .addFromType(WanelKyxConst.RiskEoaAddressRecordAddFromType.CHINALYSIS)
                .address(queryRequest.getTargetAddress())
                .expireAt(kyxProperties.generateRiskKyxExpireAtUtc())
                .riskScore(BigDecimal.ZERO)
                .build();
        if (response.getAlerts().size() <= 0) {
            // 无风险
            result.setRiskLevel(WanelKyxConst.KyxRiskLevel.NO_RISK);
            return result;
        } else {
            // 有风险
            List<String> details = response.getAlerts().stream().map(CAKyXAlertResponse.Alert::getCategory).collect(Collectors.toList());
            result.setRiskLevel(WanelKyxConst.KyxRiskLevel.HIGH_RISK);
            result.setRiskScore(new BigDecimal("100.0"));
            result.setDetail(StringUtils.join(details, ","));
            return result;
        }
    }

    /**
     * @param request
     * @param externalId
     * @return updatedAt
     * @throws Exception
     */
    private String blockedCheckKya(KyaRequest request, String externalId) throws Exception {
        int i = 0;
        while (i++ < kyxProperties.getChainalysis().getKyaRetryLimit()) {
            log.info("Chainalysis blockedCheckKya retry count {} for address {} on chain {}, externalId is {}",
                    i, request.getTargetAddress(), request.getChainId(), externalId);
            Thread.sleep(1000L);
            String updatedAt = checkKya(request, externalId);
            if (StringUtils.isNotBlank(externalId)) {
                return updatedAt;
            }
        }
        return null;
    }

    private String checkKya(KyaRequest queryRequest, String externalId) {
        try {
            //
            Request request = new Request.Builder()
                    .url(kyxProperties.getChainalysis().buildKyaRegisterCheckUrl(externalId))
                    .addHeader(KyxProperties.Chainalysis.HEADER_TOKEN, kyxProperties.getChainalysis().getToken())
                    .get()
                    .build();
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                CAKyaRegisterResponse checkResponse = ObjectUtils.fromJson(response.body().string(), CAKyaRegisterResponse.class);
                if (checkResponse != null && StringUtils.isNotEmpty(checkResponse.getUpdatedAt())) {
                    log.info("chainalysis checkKya updated at received {} for externalId {}", checkResponse.getUpdatedAt(), externalId);
                    return checkResponse.getUpdatedAt();
                } else {
                    return null;
                }
            } else {
                log.error("checkKya returns {}", response.body() == null ? "null" : response.body().string());
                return null;
            }
        } catch (Exception e) {
            log.error("checkKya exception for request {}", queryRequest.getTargetAddress(), e);
            return null;
        }
    }
}
