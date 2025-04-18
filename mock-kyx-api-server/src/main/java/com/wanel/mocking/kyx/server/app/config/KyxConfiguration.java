package com.wanel.mocking.kyx.server.app.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "kyx")
@Data
public class KyxConfiguration {

    private Python python;
    private List<Provider> providers;

    @Data
    public static class Python {
        private String scriptPath;
        private String functionName;
    }

    @Data
    public static class Provider {
        private String name;
        private boolean enabled;
    }
} 