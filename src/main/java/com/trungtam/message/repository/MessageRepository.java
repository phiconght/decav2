package com.trungtam.message.repository;

import com.trungtam.message.entity.Message;
import com.trungtam.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByRecipientIdOrderByIdDesc(Long recipientId, Pageable pageable);

    Page<Message> findByRecipientIdAndTypeOrderByIdDesc(
            Long recipientId, NotificationType type, Pageable pageable);

    Page<Message> findByRecipientIdAndReadAtIsNullOrderByIdDesc(Long recipientId, Pageable pageable);

    Page<Message> findByRecipientIdAndTypeAndReadAtIsNullOrderByIdDesc(
            Long recipientId, NotificationType type, Pageable pageable);

    Optional<Message> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now WHERE m.recipient.id = :recipientId AND m.readAt IS NULL")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);
}
