package com.trungtam.notification.dto.response;

import com.trungtam.notification.entity.NotificationPreference;
import com.trungtam.notification.entity.NotificationType;

import java.time.LocalTime;

/**
 * Mot dong tuy chon nhan thong bao theo loai.
 */
public record PreferenceItem(
        NotificationType type,
        boolean enabled,
        LocalTime quietFrom,
        LocalTime quietTo
) {
    public static PreferenceItem from(NotificationPreference p) {
        return new PreferenceItem(p.getType(), p.isEnabled(), p.getQuietFrom(), p.getQuietTo());
    }
}
