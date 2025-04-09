package com.smart.attendance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl;
    private String fastapiUrl;
    private String imagesDir;

    // Getters and Setters

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public String getFastapiUrl() {
        return fastapiUrl;
    }

    public void setFastapiUrl(String fastapiUrl) {
        this.fastapiUrl = fastapiUrl;
    }

    public String getImagesDir() {
        return imagesDir;
    }

    public void setImagesDir(String imagesDir) {
        this.imagesDir = imagesDir;
    }
}
