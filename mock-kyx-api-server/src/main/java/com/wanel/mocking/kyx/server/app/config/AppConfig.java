package com.wanel.mocking.kyx.server.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
@Slf4j
public class AppConfig {

    @Autowired
    private ResourceLoader resourceLoader;
    
    @Autowired
    private KyxConfiguration kyxConfiguration;
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
    
    @PostConstruct
    public void init() {
        try {
            // If no script path specified or file doesn't exist, copy the default script
            String scriptPath = kyxConfiguration.getPython().getScriptPath();
            File scriptFile = new File(scriptPath);
            
            if (!scriptFile.exists()) {
                log.info("Script file not found at: {}. Copying default script.", scriptPath);
                
                // Create parent directories if they don't exist
                if (!scriptFile.getParentFile().exists()) {
                    scriptFile.getParentFile().mkdirs();
                }
                
                // Copy the default script from the resources
                var resource = resourceLoader.getResource("classpath:scripts/kyx_script.py");
                Path sourcePath = resource.getFile().toPath();
                Path targetPath = scriptFile.toPath();
                
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Default script copied to: {}", targetPath);
            }
        } catch (IOException e) {
            log.error("Error copying default script", e);
        }
    }
} 