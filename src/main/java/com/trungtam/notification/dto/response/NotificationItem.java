package com.trungtam.notification.dto.response;

import com.trungtam.notification.entity.Notification;
import com.trungtam.notification.entity.NotificationStatus;
import com.trungtam.notification.entity.NotificationType;

import java.time.Instant;

/**
 * Dong lich su thong bao cho GET /notifications/me.
 */
public record NotificationItem(
        Long id,
        NotificationType type,
        String title,
        String body,
        String payload,
        NotificationStatus status,
        Instant sentAt,
        Instant createdAt
) {
    public static NotificationItem from(Notification n) {
        return new NotificationItem(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getPayload(),
                n.getStatus(),
                n.getSentAt(),
                n.getCreatedAt());
    }
}
