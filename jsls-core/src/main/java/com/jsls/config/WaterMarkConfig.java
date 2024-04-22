package com.jsls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "water-mark")
@Configuration
public class WaterMarkConfig {
    public static final String DEFAULT_WATER_MARK = "JUSUANLIUSHA";
    private String imageText;
    private String imageColor;
    private String pdfText;
    private String pdfColor;
    private String wordText;
    private String wordColor;
}