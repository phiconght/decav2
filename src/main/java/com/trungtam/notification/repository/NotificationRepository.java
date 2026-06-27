package com.trungtam.notification.repository;

import com.trungtam.notification.entity.Notification;
import com.trungtam.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    /** Worker lay lo ban ghi PENDING de gui (gioi han so luong qua Pageable). */
    List<Notification> findByStatusOrderByIdAsc(NotificationStatus status, Pageable pageable);

    Page<Notification> findByRecipientIdOrderByIdDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusOrderByIdDesc(
            Long recipientId, NotificationStatus status, Pageable pageable);
}
