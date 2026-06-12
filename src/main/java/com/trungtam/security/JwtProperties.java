package com.trungtam.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh JWT, map tu khoa "app.jwt" trong application.yml.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}
