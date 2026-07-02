package com.trungtam.message.dto.response;

import com.trungtam.message.entity.Message;
import com.trungtam.notification.entity.NotificationType;

import java.time.Instant;

/**
 * Dong danh sach hop thu (GET /messages/me) — khong kem content day du.
 */
public record MessageItem(
        Long id,
        NotificationType type,
        String title,
        String preview,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    private static final int PREVIEW_LEN = 120;

    public static MessageItem from(Message m) {
        String content = m.getContent() == null ? "" : m.getContent();
        String preview = content.length() > PREVIEW_LEN
                ? content.substring(0, PREVIEW_LEN) + "..."
                : content;
        return new MessageItem(
                m.getId(),
                m.getType(),
                m.getTitle(),
                preview,
                m.getReadAt() != null,
                m.getReadAt(),
                m.getCreatedAt());
    }
}
