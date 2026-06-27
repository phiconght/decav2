package com.trungtam.notification.dto.request;

import com.trungtam.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Mot dong tuy chon nhan thong bao theo loai (PUT /notification-preferences/me).
 */
public record UpdatePreferenceRequest(
        @NotNull NotificationType type,
        @NotNull Boolean enabled,
        LocalTime quietFrom,
        LocalTime quietTo
) {
}
