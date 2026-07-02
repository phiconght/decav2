package com.trungtam.schedule.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.schedule.dto.request.SetTeacherAttendanceRequest;
import com.trungtam.schedule.dto.response.TeacherAttendanceView;
import com.trungtam.schedule.dto.response.TeacherWorkItem;
import com.trungtam.schedule.dto.response.TeacherWorkReport;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.entity.SessionTeacherAttendance;
import com.trungtam.schedule.entity.TeacherAttendanceStatus;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.repository.SessionTeacherAttendanceRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cham cong day cua giao vien bang QR TINH dan tai phong (§ SPEC_ChamCong_DiemDanh_QR).
 * Self-scoped: chi GV cua buoi (session.teacher) moi cham cong duoc; admin cham tay
 * qua {@link #setManual}. KHONG dung QrTokenService (do la QR xoay cho diem danh HV).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeacherAttendanceService {

    /** GV duoc cham cong vao som truoc gio bat dau. */
    private static final int EARLY_MINUTES = 45;
    /** GV duoc cham cong ra sau gio ket thuc. */
    private static final int CHECKOUT_LATE_MINUTES = 60;
    /** Gioi han khoang bao cao (ngay). */
    private static final long MAX_REPORT_DAYS = 92;

    private final ClassSessionRepository sessionRepository;
    private final SessionTeacherAttendanceRepository staRepository;
    private final UserRepository userRepository;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Value("${app.jobs.missing-scan.grace-minutes:15}")
    private int graceMinutes;

    // ============================ CHAM CONG (QR phong) ============================

    @Transactional
    public TeacherAttendanceView checkin(Long sessionId, String roomCode) {
        ClassSession s = requireOwnSessionWithRoom(sessionId, roomCode);
        requireWithinCheckinWindow(s);

        SessionTeacherAttendance a = staRepository.findBySessionId(sessionId).orElse(null);
        if (a != null && a.getCheckInAt() != null) {
            throw new AppException(ErrorCode.TEACHER_ALREADY_CHECKED_IN);
        }
        if (a == null) {
            a = new SessionTeacherAttendance();
            a.setSession(s);
        }
        a.setTeacher(s.getTeacher());
        a.setCheckInAt(Instant.now());
        a.setStatus(isLate(s) ? TeacherAttendanceStatus.VAO_TRE : TeacherAttendanceStatus.DUNG_GIO);
        return TeacherAttendanceView.from(staRepository.save(a));
    }

    @Transactional
    public TeacherAttendanceView checkout(Long sessionId, String roomCode) {
        ClassSession s = requireOwnSessionWithRoom(sessionId, roomCode);
        requireWithinCheckoutWindow(s);

        SessionTeacherAttendance a = staRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_NOT_CHECKED_IN));
        if (a.getCheckInAt() == null) {
            throw new AppException(ErrorCode.TEACHER_NOT_CHECKED_IN);
        }
        a.setCheckOutAt(Instant.now());
        return TeacherAttendanceView.from(staRepository.save(a));
    }

    /** Admin cham tay (khi QR hong / buoi chua xep phong). Buoi bat buoc co GV phan cong. */
    @Transactional
    public TeacherAttendanceView setManual(Long sessionId, SetTeacherAttendanceRequest req) {
        ClassSession s = findSessionOrThrow(sessionId);
        if (s.getTeacher() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Buoi chua co giao vien phan cong");
        }
        SessionTeacherAttendance a = staRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    SessionTeacherAttendance n = new SessionTeacherAttendance();
                    n.setSession(s);
                    return n;
                });
        a.setTeacher(s.getTeacher());
        a.setStatus(req.status());
        a.setNote(req.note());
        return TeacherAttendanceView.from(staRepository.save(a));
    }

    /** Trang thai cong 1 buoi; chua co dong -> CHUA_CHAM. */
    public TeacherAttendanceView getStatus(Long sessionId) {
        ClassSession s = findSessionOrThrow(sessionId);
        return staRepository.findBySessionId(sessionId)
                .map(TeacherAttendanceView::from)
                .orElseGet(() -> TeacherAttendanceView.notYet(
                        sessionId,
                        s.getTeacher() != null ? s.getTeacher().getId() : null,
                        s.getTeacher() != null ? s.getTeacher().getFullName() : null));
    }

    // ============================ BAO CAO ============================

    /** GV tu xem cong cua minh. */
    public TeacherWorkReport myReport(LocalDate from, LocalDate to) {
        User me = currentUser();
        boolean isTeacher = me.getRoles().stream().anyMatch(r -> r.getName() == RoleName.TEACHER);
        if (!isTeacher) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return report(me.getId(), from, to);
    }

    /** Admin/nhan vien xem cong cua 1 GV (CLASS:READ). */
    public TeacherWorkReport adminReport(Long teacherId, LocalDate from, LocalDate to) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return report(teacher.getId(), from, to);
    }

    private TeacherWorkReport report(Long teacherId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Khoang thoi gian khong hop le");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Khoang thoi gian toi da 92 ngay");
        }

        List<ClassSession> sessions = sessionRepository.findTimetableForTeacher(teacherId, from, to);
        List<Long> sessionIds = sessions.stream().map(ClassSession::getId).toList();
        Map<Long, SessionTeacherAttendance> bySession = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            for (SessionTeacherAttendance a : staRepository.findBySessionIdIn(sessionIds)) {
                bySession.put(a.getSession().getId(), a);
            }
        }

        List<TeacherWorkItem> items = new java.util.ArrayList<>();
        int dungGio = 0;
        int vaoTre = 0;
        int vang = 0;
        int chuaCham = 0;
        int taughtMinutes = 0;
        for (ClassSession s : sessions) {
            if (s.getStatus() == SessionStatus.CANCELLED) {
                continue; // buoi da huy: bo khoi bao cao
            }
            SessionTeacherAttendance a = bySession.get(s.getId());
            String status = resolveReportStatus(s, a);
            switch (status) {
                case "DUNG_GIO" -> {
                    dungGio++;
                    taughtMinutes += s.getDurationMinutes();
                }
                case "VAO_TRE" -> {
                    vaoTre++;
                    taughtMinutes += s.getDurationMinutes();
                }
                case "VANG" -> vang++;
                default -> chuaCham++;
            }
            items.add(new TeacherWorkItem(
                    s.getId(),
                    s.getSessionDate(),
                    s.getStartTime(),
                    s.endTime(),
                    s.getClazz().getName(),
                    s.getRoom() != null ? s.getRoom().getName() : null,
                    s.getDurationMinutes(),
                    status,
                    a != null ? a.getCheckInAt() : null,
                    a != null ? a.getCheckOutAt() : null,
                    a != null ? a.getNote() : null));
        }
        // moi nhat truoc
        items.sort((x, y) -> {
            int c = y.date().compareTo(x.date());
            return c != 0 ? c : y.startTime().compareTo(x.startTime());
        });

        TeacherWorkReport.Summary summary = new TeacherWorkReport.Summary(
                items.size(), dungGio, vaoTre, vang, chuaCham, taughtMinutes);
        return new TeacherWorkReport(summary, items);
    }

    /** Trang thai hien thi trong bao cao: co dong -> theo dong; chua co -> DONE thi VANG, con lai CHUA_CHAM. */
    private String resolveReportStatus(ClassSession s, SessionTeacherAttendance a) {
        if (a != null) {
            return a.getStatus().name();
        }
        return s.getStatus() == SessionStatus.DONE ? "VANG" : "CHUA_CHAM";
    }

    // ============================ HELPERS ============================

    private ClassSession requireOwnSessionWithRoom(Long sessionId, String roomCode) {
        ClassSession s = findSessionOrThrow(sessionId);
        if (s.getStatus() == SessionStatus.CANCELLED) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Buoi hoc da bi huy");
        }
        User me = currentUser();
        if (s.getTeacher() == null || !s.getTeacher().getId().equals(me.getId())) {
            throw new AppException(ErrorCode.NOT_SESSION_TEACHER);
        }
        if (s.getRoom() == null) {
            throw new AppException(ErrorCode.ROOM_QR_INVALID, "Buoi chua xep phong — lien he quan tri cham tay");
        }
        if (!s.getRoom().getQrCode().equals(roomCode)) {
            throw new AppException(ErrorCode.ROOM_QR_INVALID);
        }
        return s;
    }

    private void requireWithinCheckinWindow(ClassSession s) {
        LocalDate today = LocalDate.now(zone());
        LocalTime now = LocalTime.now(zone());
        LocalTime windowStart = s.getStartTime().minusMinutes(EARLY_MINUTES);
        LocalTime windowEnd = s.getStartTime().plusMinutes(s.getDurationMinutes());
        boolean ok = today.equals(s.getSessionDate())
                && !now.isBefore(windowStart)
                && !now.isAfter(windowEnd);
        if (!ok) {
            throw new AppException(ErrorCode.TEACHER_CHECKIN_TIME_INVALID);
        }
    }

    private void requireWithinCheckoutWindow(ClassSession s) {
        LocalDate today = LocalDate.now(zone());
        LocalTime now = LocalTime.now(zone());
        LocalTime windowEnd = s.getStartTime()
                .plusMinutes(s.getDurationMinutes())
                .plusMinutes(CHECKOUT_LATE_MINUTES);
        boolean ok = today.equals(s.getSessionDate())
                && !now.isBefore(s.getStartTime().minusMinutes(EARLY_MINUTES))
                && !now.isAfter(windowEnd);
        if (!ok) {
            throw new AppException(ErrorCode.TEACHER_CHECKIN_TIME_INVALID);
        }
    }

    private boolean isLate(ClassSession s) {
        LocalTime now = LocalTime.now(zone());
        return now.isAfter(s.getStartTime().plusMinutes(graceMinutes));
    }

    private ClassSession findSessionOrThrow(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
    }

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private ZoneId zone() {
        return ZoneId.of(timezone);
    }

    /** Cho CloseSessionJob: buoi DONE ma chua ai cham -> tao dong VANG. Idempotent. */
    @Transactional
    public void markAbsentIfMissing(ClassSession s) {
        if (s.getTeacher() == null || staRepository.existsBySessionId(s.getId())) {
            return;
        }
        SessionTeacherAttendance a = new SessionTeacherAttendance();
        a.setSession(s);
        a.setTeacher(s.getTeacher());
        a.setStatus(TeacherAttendanceStatus.VANG);
        staRepository.save(a);
    }

    /** Batch cho timetable view TEACHER: map sessionId -> status name. */
    public Map<Long, String> statusBySessionIds(List<Long> sessionIds) {
        Map<Long, String> map = new HashMap<>();
        if (sessionIds == null || sessionIds.isEmpty()) {
            return map;
        }
        for (SessionTeacherAttendance a : staRepository.findBySessionIdIn(sessionIds)) {
            map.put(a.getSession().getId(), a.getStatus().name());
        }
        return map;
    }
}
