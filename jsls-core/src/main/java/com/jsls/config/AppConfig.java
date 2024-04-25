package com.jsls.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    @Value("${spring.application.name}")
    private String appName;

    public String getSpringAppName() {
        return appName;
    }
}
