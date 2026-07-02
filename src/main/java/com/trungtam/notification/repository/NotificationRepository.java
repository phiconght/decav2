package com.trungtam.notification.repository;

import com.trungtam.notification.entity.Notification;
import com.trungtam.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    /** Worker lay lo ban ghi PENDING de gui (gioi han so luong qua Pageable). */
    List<Notification> findByStatusOrderByIdAsc(NotificationStatus status, Pageable pageable);

    Page<Notification> findByRecipientIdOrderByIdDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusOrderByIdDesc(
            Long recipientId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadAtIsNullOrderByIdDesc(Long recipientId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipient.id = :recipientId AND n.readAt IS NULL")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);
}
