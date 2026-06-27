package com.trungtam.notification.dto.request;

import com.trungtam.notification.entity.NotificationStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Tham so loc/phan trang lich su thong bao cua nguoi dung hien tai.
 */
@Getter
@Setter
public class NotificationSearchParams {
    private NotificationStatus status;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
