package com.trungtam.notification.repository;

import com.trungtam.notification.entity.NotificationPreference;
import com.trungtam.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserIdOrderByTypeAsc(Long userId);

    Optional<NotificationPreference> findByUserIdAndType(Long userId, NotificationType type);
}
