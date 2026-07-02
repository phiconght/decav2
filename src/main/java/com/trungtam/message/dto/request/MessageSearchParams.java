package com.trungtam.message.dto.request;

import com.trungtam.notification.entity.NotificationType;
import lombok.Getter;
import lombok.Setter;

/**
 * Tham so loc/phan trang hop thu cua nguoi dung hien tai.
 */
@Getter
@Setter
public class MessageSearchParams {
    private NotificationType type;
    private Boolean unread;
    private int current = 1;
    private int pageSize = 10;
}
