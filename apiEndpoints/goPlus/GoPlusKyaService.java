package com.wanel.kyx.provider.service.kya.impl.goplus;

import com.wanel.common.utils.ObjectUtils;
import com.wanel.kyx.provider.configurer.KyxProperties;
import com.wanel.kyx.provider.consts.WanelKyxConst;
import com.wanel.kyx.provider.dto.dto.RiskEoaAddressDTO;
import com.wanel.kyx.provider.dto.response.GoPlusRiskEoaAddressResponse;
import com.wanel.kyx.provider.exception.KyaByGoPlusInvalidHttpResponseException;
import com.wanel.kyx.provider.exception.KyaByGoPlusParseResponseException;
import com.wanel.kyx.provider.model.request.KyaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@Component
public class GoPlusKyaService {
    private final KyxProperties kyxProperties;

    final @Qualifier("kyaGoPlusHttpClient") OkHttpClient httpClient;

    public RiskEoaAddressDTO queryRiskEoaAddressByGoPlus(KyaRequest queryRequest) throws Exception {
        Request request = new Request.Builder()
                .url(kyxProperties.getGoPlus().getUrl() + queryRequest.getTargetAddress())
                .get()
                .build();
        Response response = httpClient.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            return parseGoPlusEoaAddressResponse(response.body().string(), queryRequest.getChainId(), queryRequest.getTargetAddress());
        } else {
            String errorMsg = response.body() == null ? "null" : response.body().string();
            log.error("queryRiskEoaAddressByGoPlus returns {}", errorMsg);
            throw new KyaByGoPlusInvalidHttpResponseException(errorMsg, ObjectUtils.toJson(request));
        }
    }

    private RiskEoaAddressDTO parseGoPlusEoaAddressResponse(String data, long chainId, String targetAddress) {
        if (!StringUtils.isBlank(data)) {
            GoPlusRiskEoaAddressResponse result = ObjectUtils.fromJson(data, GoPlusRiskEoaAddressResponse.class);
            if (result != null) {
                // 进行风险等级分类
                int riskLevel = result.isInRisk() ? WanelKyxConst.KyxRiskLevel.HIGH_RISK : WanelKyxConst.KyxRiskLevel.NO_RISK;
                BigDecimal riskScore = WanelKyxConst.KyxRiskLevel.isInRisk(riskLevel) ? new BigDecimal("100") : BigDecimal.ZERO;
                // 返回对应的值
                return RiskEoaAddressDTO.builder()
                        .addFromType(WanelKyxConst.RiskEoaAddressRecordAddFromType.GOPLUS)
                        .riskScore(riskScore)
                        .riskLevel(riskLevel)
                        .detail(result.getResult().riskDetail())
                        .expireAt(kyxProperties.generateRiskKyxExpireAtUtc())
                        .chainId(chainId)
                        .address(targetAddress).build();
            }
        }
        // 无法拿到有效值，就抛异常
        throw new KyaByGoPlusParseResponseException(data, chainId, targetAddress);
    }
}
