package com.jsls.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

@Configurable
public class FeignConfig {
    @Value("${resource.baseUrl}")
    private String resourceBaseUrl;
}
