package com.trungtam.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Huy dang ky token thiet bi (vd dang xuat, go app).
 */
public record UnregisterDeviceRequest(
        @NotBlank String token
) {
}
