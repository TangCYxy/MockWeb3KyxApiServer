package com.wanel.mocking.kyx.server.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.wanel.mocking.kyx.server")
public class MockKyxApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockKyxApiServerApplication.class, args);
    }
} 