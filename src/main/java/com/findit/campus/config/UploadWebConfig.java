package com.findit.campus.config;

import com.findit.campus.service.UploadStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadWebConfig implements WebMvcConfigurer {
    private final UploadStorage uploadStorage;

    public UploadWebConfig(UploadStorage uploadStorage) {
        this.uploadStorage = uploadStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadStorage.directory().toUri().toString());
    }
}
