package com.trungtam.notification.dto.request;

import com.trungtam.notification.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dang ky token thiet bi de nhan push.
 */
public record RegisterDeviceRequest(
        @NotBlank String token,
        @NotNull DevicePlatform platform
) {
}
