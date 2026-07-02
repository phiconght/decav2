package com.trungtam.notification.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Mot ban ghi hop thu di (outbox). NotificationWorker quet PENDING de gui push.
 * payload luu JSON dang chuoi (TEXT) de tranh bay JSONB <-> ddl validate.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Column(name = "channel", nullable = false, length = 10)
    private String channel = "PUSH";

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "dedupe_key", length = 150)
    private String dedupeKey;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error", length = 500)
    private String error;

    /** Thoi diem nguoi nhan da doc thong bao (null = chua doc). */
    @Column(name = "read_at")
    private Instant readAt;

    /** Tin nhan (noi dung day du) tuong ung; null neu su kien khong co noi dung day du. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private com.trungtam.message.entity.Message message;
}
