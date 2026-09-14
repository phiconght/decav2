package com.trungtam.schedule.job;

import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.leave.entity.LeaveStatus;
import com.trungtam.leave.repository.LeaveRequestRepository;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.service.NotificationService;
import com.trungtam.schedule.entity.AttendanceStatus;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionAttendance;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.repository.SessionAttendanceRepository;
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
import java.util.EnumSet;
import java.util.List;

/**
 * Job quet thieu check-in / check-out cho cac buoi PLANNED hoac IN_PROGRESS trong ngay
 * (buoi da bat dau nhung SessionStateJob chua kip dong sang DONE).
 * <ul>
 *   <li>HV con CHUA_CHECKIN, da qua (start + grace) va KHONG nghi phep -> MISSING_CHECKIN cho phu huynh.</li>
 *   <li>HV da check-in nhung chua check-out, da qua (end + grace) -> MISSING_CHECKOUT cho phu huynh.</li>
 * </ul>
 * Idempotent qua dedupeKey. Cau hinh app.jobs.missing-scan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.missing-scan",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MissingCheckScanJob {

    private final ClassSessionRepository sessionRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Value("${app.jobs.missing-scan.grace-minutes:15}")
    private int graceMinutes;

    @Scheduled(fixedDelayString = "${app.jobs.missing-scan.fixed-delay:300000}")
    @Transactional
    public void scan() {
        ZoneId zone = ZoneId.of(timezone);
        LocalDate today = LocalDate.now(zone);
        LocalTime now = LocalTime.now(zone);

        int missingCheckin = 0;
        int missingCheckout = 0;
        for (ClassSession s : sessionRepository.findBySessionDateAndStatusIn(
                today, EnumSet.of(SessionStatus.PLANNED, SessionStatus.IN_PROGRESS))) {
            LocalTime start = s.getStartTime();
            Integer duration = s.getDurationMinutes();
            if (start == null || duration == null) {
                continue;
            }
            boolean pastCheckinDeadline = now.isAfter(start.plusMinutes(graceMinutes));
            boolean pastCheckoutDeadline = now.isAfter(start.plusMinutes(duration).plusMinutes(graceMinutes));
            if (!pastCheckinDeadline && !pastCheckoutDeadline) {
                continue;
            }

            for (SessionAttendance a : attendanceRepository.findBySessionId(s.getId())) {
                Long studentId = a.getUser().getId();
                AttendanceStatus status = a.getStatus();

                // Thieu check-in: chua check-in, qua han va khong nghi phep.
                if (pastCheckinDeadline
                        && status == AttendanceStatus.CHUA_CHECKIN
                        && a.getCheckInAt() == null
                        && !leaveRequestRepository.isOnLeave(studentId, s.getId(), s.getSessionDate(),
                                s.getClazz().getId(), LeaveStatus.APPROVED)) {
                    missingCheckin += notifyParents(studentId, s.getId(), NotificationType.MISSING_CHECKIN,
                            "Chưa check-in", "Học viên chưa check-in buổi học.",
                            "MISSING_CHECKIN:" + s.getId());
                    continue;
                }

                // Thieu check-out: da check-in, chua check-out, qua han ket thuc.
                if (pastCheckoutDeadline
                        && a.getCheckInAt() != null
                        && a.getCheckOutAt() == null
                        && status != AttendanceStatus.CO_PHEP) {
                    missingCheckout += notifyParents(studentId, s.getId(), NotificationType.MISSING_CHECKOUT,
                            "Chưa check-out", "Học viên chưa check-out khỏi buổi học.",
                            "MISSING_CHECKOUT:" + s.getId());
                }
            }
        }
        if (missingCheckin > 0 || missingCheckout > 0) {
            log.info("[missing-scan] enqueue MISSING_CHECKIN={} MISSING_CHECKOUT={} ngay {}",
                    missingCheckin, missingCheckout, today);
        }
    }

    /** @return so thong bao thuc su enqueue (sau dedupe/tuy chon). */
    private int notifyParents(Long studentId, Long sessionId, NotificationType type,
                              String title, String body, String keyPrefix) {
        String payload = "{\"sessionId\":" + sessionId + ",\"studentId\":" + studentId + "}";
        int n = 0;
        for (Long parentId : studentParentRepository.findParentIdsByStudentId(studentId)) {
            if (notificationService.enqueue(parentId, type, title, body, payload,
                    keyPrefix + ":" + parentId).isPresent()) {
                n++;
            }
        }
        return n;
    }
}
