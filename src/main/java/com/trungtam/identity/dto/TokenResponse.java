package com.trungtam.identity.dto;

/**
 * Cap token tra ve sau dang nhap / lam moi.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
    public static TokenResponse of(String accessToken, String refreshToken,
                                   long expiresInSeconds, UserResponse user) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
