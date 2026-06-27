package com.trungtam.notification.job;

import com.trungtam.notification.entity.DeviceToken;
import com.trungtam.notification.entity.Notification;
import com.trungtam.notification.entity.NotificationStatus;
import com.trungtam.notification.push.PushResult;
import com.trungtam.notification.push.PushSender;
import com.trungtam.notification.repository.DeviceTokenRepository;
import com.trungtam.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Job gui outbox: dinh ky lay cac thong bao PENDING, gui qua moi token active cua nguoi nhan
 * roi cap nhat SENT/FAILED. Token "unregistered" -> set active=false.
 * Cau hinh tai app.jobs.notify-worker (enabled / fixed-delay / max-retry).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.notify-worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationWorker {

    private static final int BATCH_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;

    @Value("${app.jobs.notify-worker.max-retry:5}")
    private int maxRetry;

    @Scheduled(fixedDelayString = "${app.jobs.notify-worker.fixed-delay:60000}")
    @Transactional
    public void dispatchPending() {
        List<Notification> pending = notificationRepository.findByStatusOrderByIdAsc(
                NotificationStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        if (pending.isEmpty()) {
            return;
        }
        int sent = 0;
        int failed = 0;
        for (Notification n : pending) {
            if (processOne(n)) {
                sent++;
            } else {
                failed++;
            }
        }
        log.info("[notify-worker] xu ly {} thong bao: SENT={} FAILED={}", pending.size(), sent, failed);
    }

    /** @return true neu ket qua cuoi cung la SENT. */
    private boolean processOne(Notification n) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(n.getRecipient().getId());
        if (tokens.isEmpty()) {
            // Khong co thiet bi: coi nhu that bai tam thoi, het luot retry thi FAILED.
            return markRetryOrFailed(n, "khong co token active");
        }

        boolean anySuccess = false;
        String lastError = null;
        for (DeviceToken token : tokens) {
            PushResult result = pushSender.send(n, token);
            switch (result.status()) {
                case SUCCESS -> anySuccess = true;
                case UNREGISTERED -> {
                    token.setActive(false);
                    deviceTokenRepository.save(token);
                    lastError = result.message();
                }
                case FAILED -> lastError = result.message();
            }
        }

        if (anySuccess) {
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(Instant.now());
            n.setError(null);
            notificationRepository.save(n);
            return true;
        }
        return markRetryOrFailed(n, lastError);
    }

    /** Tang retry_count; vuot max-retry -> FAILED, con lai -> giu PENDING de thu lai. */
    private boolean markRetryOrFailed(Notification n, String error) {
        n.setRetryCount(n.getRetryCount() + 1);
        n.setError(error);
        if (n.getRetryCount() >= maxRetry) {
            n.setStatus(NotificationStatus.FAILED);
        }
        notificationRepository.save(n);
        return false;
    }
}
