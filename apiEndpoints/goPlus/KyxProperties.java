package com.wanel.kyx.provider.configurer;

import com.wanel.common.configure.async.AsyncTaskProperties;
import com.wanel.common.configure.http.HttpClientProperties;
import com.wanel.common.utils.ObjectUtils;
import com.wanel.kyx.provider.model.chainalysis.request.CAKyaRegisterPayload;
import com.wanel.kyx.provider.model.chainalysis.request.CAKytRegisterPayload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * kyx配置
 *
 * @author tc
 * @date 2024-05-28
 */
@ConfigurationProperties(prefix = "wanel.kyx.provider")
@Data
public class KyxProperties implements Serializable {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final long milliSecondsPerDay = 24 * 60 * 60 * 1000L;

    /**
     * 是否启用风险eoa地址功能
     */
    private boolean enable;

    /**
     * goPlus的配置
     */
    private GoPlus goPlus = new GoPlus();

    /**
     * chainalysis的配置
     */
    private Chainalysis chainalysis = new Chainalysis();

    /**
     * kyx的processor配置
     */
    private KyxProcessor kyx = new KyxProcessor();

    /**
     * kya的processor配置
     */
    private KyaProcessor kya = new KyaProcessor();

    /**
     * kyt的processor配置
     */
    private KytProcessor kyt = new KytProcessor();

    /**
     * 一次重试30个
     */
    private int batchKyaRetryCount = 30;

    /**
     * 一次重试30个
     */
    private int batchKytProcessCount = 30;

    /**
     * 风险地址有效天数
     */
    private int recordAliveDay = 7;

    /**
     * 非风险地址有效分钟
     */
    private int recordAliveMinuteNormal = 24 * 60;

    /**
     * kyx失败指定次数之后，就直接放弃，避免无限次尝试
     */
    private int maxKyxRetryCount = 3;

    /**
     * 固定设置为成功的token（比如ucoin正在发行中）
     */
    private List<String> fixSucceedTokens = new ArrayList<>();

    /**
     * kya重试时间戳
     *
     */
    private Integer[] kyaRetryIntervalSeconds = new Integer[]{20, 40, 60, 300, 1800, 3600};

    /**
     * kyt任务重试时间戳
     *
     */
    private Integer[] kytRetryIntervalSeconds = new Integer[]{60, 60, 60, 300, 1800, 3600};

    public Long calcNextKyaRetryMs(Long currentTimeMs, Integer retryCount) {
        if (retryCount > kyaRetryIntervalSeconds.length) {
            retryCount = kyaRetryIntervalSeconds.length - 1;
        }
        return currentTimeMs + 1000L * kyaRetryIntervalSeconds[retryCount];
    }

    public Long calcNextKytRetryMs(Long currentTimeMs, Integer retryCount) {
        if (retryCount > kytRetryIntervalSeconds.length) {
            retryCount = kytRetryIntervalSeconds.length - 1;
        }
        return currentTimeMs + 1000L * kytRetryIntervalSeconds[retryCount];
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class KyxProcessor {
        /**
         *
         */
        private int schedulingBatchQueryCount = 10;

        /**
         *
         */
        private Long schedulingFixedDelayMs = 30_000L;
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class KyaProcessor {
        /**
         * 超时2s, 然后直接返回
         */
        private int timeoutSec = 3;

        /**
         * 异步任务相关（要求2s内返回等）
         */
        private AsyncTaskProperties task = new AsyncTaskProperties();

        private boolean testKyaDirectlyTimeout;

        private boolean testKyaNormalTimeout;

        /**
         * 最大重试次数
         */
        private Integer maxRetryCount = 2;
    }


    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class KytProcessor {
        /**
         * 超时2s, 然后直接返回
         */
        private int timeoutSec = 3;

        /**
         * 异步任务相关（要求2s内返回等）
         */
        private AsyncTaskProperties task = new AsyncTaskProperties();

        /**
         * 最大重试次数(kyt操作永不失败)
         */
        private Integer maxRetryCount = 99999999;
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class GoPlus {
        /**
         * 暂停通过chainalysis进行kya
         */
        private boolean pauseKya = false;

        /**
         * 默认屏蔽chainalysis的kyt功能（不实际执行，直接返回无风险）
         */
        private boolean pauseKyt = true;
        /**
         * goPlus的url
         */
        String url = "https://api.gopluslabs.io/api/v1/address_security/";

        /**
         * http连接相关参数
         */
        private HttpClientProperties http = new HttpClientProperties();

        /**
         * 超时1.5s, 然后直接返回
         */
        private int timeoutMillSec = 1000;

        /**
         * 异步任务相关（要求2s内返回等）
         */
        private AsyncTaskProperties task = new AsyncTaskProperties();
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class Chainalysis {
        public static final String HEADER_TOKEN = "Token";
        /**
         * 暂停通过chainalysis进行kya
         */
        private boolean pauseKya = false;

        /**
         * 默认屏蔽chainalysis的kyt功能（不实际执行，直接返回无风险）
         */
        private boolean pauseKyt = true;

        /**
         * 不进行kyt的周期性检查
         */
        private boolean pauseKytScheduling = false;
        /**
         * chainalysis
         */
        String host = "https://api.chainalysis.com";

        /**
         *
         */
        String token = "b57c05a970c164279819cbab38968f48e1ac7cd4cb504a30bff9b4114ca4f65b";

        /**
         * http连接相关参数
         */
        private HttpClientProperties kyaHttp = new HttpClientProperties();

        /**
         * http连接相关参数
         */
        private HttpClientProperties kytHttp = new HttpClientProperties();

        /**
         * 超时1.5s, 然后直接返回
         */
        private int timeoutMillSec = 1000;

        /**
         * kya最大重试3次（如果没有拿到updateAt）
         */
        private int kyaRetryLimit = 3;

        /**
         * kyt最大重试3次（如果没有拿到updateAt）
         */
        private int kytRetryLimit = 3;

        /**
         * 每次kytRequest的最大返回结果数量，比如100
         */
        private int kytAlertMonitorCountPerRequest = 100;

        /**
         * 异步任务相关（要求2s内返回等）
         */
        private AsyncTaskProperties task = new AsyncTaskProperties();

        private String getUserIdInUrl(String userId) {
            return "WA_CA_" + userId;
        }

        /**
         * post
         *
         * @param userId
         * @return
         */
        public String buildKyaRegisterUrl(String userId) {
            return String.format("%s/api/kyt/v2/users/%s/withdrawal-attempts", host, getUserIdInUrl(userId));
        }

        public RequestBody buildKyaRegisterBody(CAKyaRegisterPayload payload) {
            return RequestBody.create(ObjectUtils.toJson(payload), JSON);
        }

        /**
         * get
         *
         * @param externalId
         * @return
         */
        public String buildKyaRegisterCheckUrl(String externalId) {
            return String.format("%s/api/kyt/v2/withdrawal-attempts/%s", host, externalId);
        }

        /**
         * get
         * response see {@link CAKyXAlertResponse}
         *
         * @param externalId
         * @return
         */
        public String buildKyaAlertUrl(String externalId) {
            return String.format("%s/api/kyt/v2/withdrawal-attempts/%s/alerts", host, externalId);
        }

        /**
         * post
         *
         * @param userId
         * @return
         */
        public String buildKytRegisterUrl(String userId) {
            return String.format("%s/api/kyt/v2/users/%s/transfers", host, getUserIdInUrl(userId));
        }

        public RequestBody buildKytRegisterBody(CAKytRegisterPayload payload) {
            return RequestBody.create(ObjectUtils.toJson(payload), JSON);
        }

        /**
         * get
         *
         * @param externalId
         * @return
         */
        public String buildKytRegisterCheckUrl(String externalId) {
            return String.format("%s/api/kyt/v2/transfers/%s", host, externalId);
        }

        /**
         * get
         * response see {@link CAKyXAlertResponse}
         *
         * @param externalId
         * @return
         */
        public String buildKytAlertUrl(String externalId) {
            return String.format("%s/api/kyt/v2/transfers/%s/alerts", host, externalId);
        }

        /**
         * get
         *
         * @param startTimeStr
         * @param endTimeStr
         * @param offset
         * @param limit
         * @return
         */
        public String buildKytAlertMonitorUrl(String startTimeStr, String endTimeStr, int offset, int limit) {
            return String.format("%s/api/kyt/v1/alerts/?createdAt_lte=%s&createdAt_gte=%s&limit=%d&offset=%d",
                    host, endTimeStr, startTimeStr, limit, offset);
        }
    }

    /**
     * 生成风险地址信息过期时间
     *
     * @return
     */
    public long generateRiskKyxExpireAtUtc() {
        return System.currentTimeMillis() + recordAliveDay * milliSecondsPerDay;
    }

    /**
     * 非风险地址信息过期时间
     *
     * @return
     */
    public long generateNormalKyxExpireAtUtc() {
        return System.currentTimeMillis() + recordAliveMinuteNormal * 60 * 1000L;
    }

    /**
     * 是否某个token被设置为固定通过kyx校验
     * @param assetName
     * @return
     */
    public boolean isFixedSucceedToken(String assetName) {
        for (String fixSucceedToken : fixSucceedTokens) {
            if (StringUtils.equalsIgnoreCase(fixSucceedToken, assetName)) {
                return true;
            }
            return false;
        }
        return false;
    }
}
