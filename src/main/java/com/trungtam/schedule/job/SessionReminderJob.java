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
import java.util.HashSet;
import java.util.Set;

/**
 * Job nhac buoi sap dien ra: buoi PLANNED bat dau trong [now, now + remind-before-minutes]
 * -> SESSION_REMINDER cho HV trong roster + GIAO VIEN cua buoi.
 * Phu huynh CHI nhan neu bat tuong minh preference (mac dinh TAT).
 * Idempotent qua dedupeKey. Cau hinh app.jobs.session-remind (mac dinh 15 phut truoc gio hoc).
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

    @Value("${app.jobs.session-remind.remind-before-minutes:15}")
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
        Long classId = s.getClazz().getId();
        String title = "Sap den gio hoc";
        String body = "Buoi hoc sap bat dau luc " + s.getStartTime() + ".";
        String content = body + "\n\n" + describeSession(s);
        String payload = "{\"sessionId\":" + sessionId + "}";
        int n = 0;

        // Hoc vien trong roster
        for (Long studentId : rosterRepository.findStudentIdsByClassId(classId)) {
            if (notificationService.notify(studentId, NotificationType.SESSION_REMINDER, title, body,
                    title, content, payload, "SESSION_REMINDER:" + sessionId + ":" + studentId).isPresent()) {
                n++;
            }
            // Phu huynh: chi khi bat tuong minh (mac dinh TAT loai nhac buoi).
            for (Long parentId : studentParentRepository.findParentIdsByStudentId(studentId)) {
                if (notificationService.isOptedIn(parentId, NotificationType.SESSION_REMINDER)
                        && notificationService.notify(parentId, NotificationType.SESSION_REMINDER, title, body,
                        title, content, payload, "SESSION_REMINDER:" + sessionId + ":" + parentId).isPresent()) {
                    n++;
                }
            }
        }

        // Giao vien: giao vien gan cho buoi + giao vien cua lop (gop, khong trung).
        Set<Long> teacherIds = new HashSet<>(rosterRepository.findTeacherIdsByClassId(classId));
        if (s.getTeacher() != null) {
            teacherIds.add(s.getTeacher().getId());
        }
        for (Long teacherId : teacherIds) {
            if (notificationService.notify(teacherId, NotificationType.SESSION_REMINDER, title, body,
                    title, content, payload, "SESSION_REMINDER:" + sessionId + ":T:" + teacherId).isPresent()) {
                n++;
            }
        }
        return n;
    }

    /** Mo ta buoi hoc (lop, ngay, gio, phong) cho noi dung tin nhan day du. */
    private String describeSession(ClassSession s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Lop: ").append(s.getClazz().getName());
        sb.append("\nNgay: ").append(s.getSessionDate());
        sb.append("\nGio: ").append(s.getStartTime());
        if (s.endTime() != null) {
            sb.append(" - ").append(s.endTime());
        }
        if (s.getRoom() != null) {
            sb.append("\nPhong: ").append(s.getRoom().getName());
        }
        return sb.toString();
    }
}
