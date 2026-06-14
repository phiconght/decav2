package com.trungtam.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String provider,
        String localRoot,
        String publicBaseUrl,
        List<String> allowedContentTypes,
        long maxFileSizeBytes
) {}
