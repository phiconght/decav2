package com.trungtam.message.dto.response;

import com.trungtam.message.entity.Message;
import com.trungtam.notification.entity.NotificationType;

import java.time.Instant;

/**
 * Chi tiet tin nhan (GET /messages/{id}) — kem content day du va payload deep-link.
 */
public record MessageDetail(
        Long id,
        NotificationType type,
        String title,
        String content,
        String payload,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static MessageDetail from(Message m) {
        return new MessageDetail(
                m.getId(),
                m.getType(),
                m.getTitle(),
                m.getContent(),
                m.getPayload(),
                m.getReadAt() != null,
                m.getReadAt(),
                m.getCreatedAt());
    }
}
