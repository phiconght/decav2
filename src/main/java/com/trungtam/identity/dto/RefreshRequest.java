package com.trungtam.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token khong duoc de trong")
        String refreshToken
) {
}
