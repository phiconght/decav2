package com.trungtam.message.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.identity.entity.User;
import com.trungtam.notification.entity.NotificationType;
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
 * Tin nhan: noi dung DAY DU cua mot su kien (hop thu in-app).
 * Moi su kien sinh 1 Message + 1 Notification (vắn tắt) tro ve Message nay.
 * Dung chung phan loai voi NotificationType.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    /** Nguoi gui; null = he thong. Chua san cho "admin gui tay" sau nay. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "payload")
    private String payload;

    @Column(name = "read_at")
    private Instant readAt;
}
