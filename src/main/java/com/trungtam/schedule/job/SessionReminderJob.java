package com.trungtam.schedule.job;

import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.service.NotificationService;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.repository.ClassRosterRepository;
import com.trungtam.schedule.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Job nhac buoi sap dien ra: buoi PLANNED bat dau trong [now, now + remind-before-minutes]
 * -> SESSION_REMINDER cho HV trong roster + phu huynh cua ho.
 * Idempotent qua dedupeKey. Cau hinh app.jobs.session-remind.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.session-remind",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SessionReminderJob {

    private final ClassSessionRepository sessionRepository;
    private final ClassRosterRepository rosterRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Value("${app.jobs.session-remind.remind-before-minutes:60}")
    private int remindBeforeMinutes;

    @Scheduled(fixedDelayString = "${app.jobs.session-remind.fixed-delay:300000}")
    @Transactional
    public void remind() {
        ZoneId zone = ZoneId.of(timezone);
        LocalDate today = LocalDate.now(zone);
        LocalTime now = LocalTime.now(zone);
        LocalTime until = now.plusMinutes(remindBeforeMinutes);

        int reminded = 0;
        for (ClassSession s : sessionRepository.findBySessionDateAndStatus(today, SessionStatus.PLANNED)) {
            LocalTime start = s.getStartTime();
            if (start == null) {
                continue;
            }
            // Buoi bat dau trong [now, until]. Bo qua truong hop until vat qua nua dem.
            if (start.isBefore(now) || start.isAfter(until)) {
                continue;
            }
            reminded += notifyRoster(s);
        }
        if (reminded > 0) {
            log.info("[session-remind] enqueue SESSION_REMINDER={} ngay {}", reminded, today);
        }
    }

    /** @return so thong bao thuc su enqueue (sau dedupe/tuy chon). */
    private int notifyRoster(ClassSession s) {
        Long sessionId = s.getId();
        String title = "Sap den gio hoc";
        String body = "Buoi hoc sap bat dau luc " + s.getStartTime() + ".";
        String payload = "{\"sessionId\":" + sessionId + "}";
        int n = 0;
        for (Long studentId : rosterRepository.findStudentIdsByClassId(s.getClazz().getId())) {
            if (notificationService.enqueue(studentId, NotificationType.SESSION_REMINDER, title, body, payload,
                    "SESSION_REMINDER:" + sessionId + ":" + studentId).isPresent()) {
                n++;
            }
            for (Long parentId : studentParentRepository.findParentIdsByStudentId(studentId)) {
                if (notificationService.enqueue(parentId, NotificationType.SESSION_REMINDER, title, body, payload,
                        "SESSION_REMINDER:" + sessionId + ":" + parentId).isPresent()) {
                    n++;
                }
            }
        }
        return n;
    }
}
