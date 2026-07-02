package com.trungtam.payment.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.payment.dto.response.StudentSessionReport;
import com.trungtam.schedule.dto.response.TeacherWorkReport;
import com.trungtam.schedule.entity.AttendanceStatus;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionAttendance;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.repository.SessionAttendanceRepository;
import com.trungtam.schedule.service.TeacherAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bao cao buoi hoc HV + cong GV (SPEC_ThanhToan §2.8). GV tai dung TeacherWorkReport co san.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentReportService {

    private static final long MAX_REPORT_DAYS = 366;

    private final ClassSessionRepository sessionRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final TeacherAttendanceService teacherAttendanceService;

    /** Bao cao chuyen can 1 HV trong ky (group theo lop trong tung item, sort ngay). */
    public StudentSessionReport studentSessions(Long studentId, LocalDate from, LocalDate to) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        validatePeriod(from, to);

        List<ClassSession> sessions = sessionRepository.findTimetableForStudent(
                student.getId(), from, to);

        // Map sessionId -> attendance status
        Map<Long, AttendanceStatus> statusBySession = new HashMap<>();
        for (SessionAttendance a : attendanceRepository.findByUserIdAndDateRange(
                student.getId(), from, to)) {
            statusBySession.put(a.getSession().getId(), a.getStatus());
        }

        List<StudentSessionReport.Item> items = new ArrayList<>();
        int total = 0;
        int coMat = 0;
        int tre = 0;
        int vang = 0;
        int coPhep = 0;
        int chuaCheckin = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ClassSession s : sessions) {
            if (s.getStatus() != SessionStatus.DONE) {
                continue; // chi buoi da hoc vao bao cao chuyen can (dong bo voi tinh phi)
            }
            total++;
            AttendanceStatus st = statusBySession.getOrDefault(s.getId(), AttendanceStatus.CHUA_CHECKIN);
            switch (st) {
                case CO_MAT -> coMat++;
                case TRE -> tre++;
                case VANG -> vang++;
                case CO_PHEP -> coPhep++;
                default -> chuaCheckin++;
            }
            // Tien tinh phi: cac buoi khong phai CO_PHEP (dong bo quyet dinh §5)
            if (st != AttendanceStatus.CO_PHEP && s.getPrice() != null) {
                totalAmount = totalAmount.add(s.getPrice());
            }
            items.add(new StudentSessionReport.Item(
                    s.getSessionDate(),
                    s.getClazz() != null ? s.getClazz().getName() : null,
                    s.getStartTime(),
                    s.endTime(),
                    st.name(),
                    s.getPrice()));
        }
        // moi nhat truoc
        items.sort((x, y) -> {
            int c = y.date().compareTo(x.date());
            return c != 0 ? c : (y.startTime() != null && x.startTime() != null
                    ? y.startTime().compareTo(x.startTime()) : 0);
        });

        StudentSessionReport.Summary summary = new StudentSessionReport.Summary(
                total, coMat, tre, vang, coPhep, chuaCheckin, totalAmount);
        return new StudentSessionReport(summary, items);
    }

    /** Bao cao cong GV (tai dung logic da code o TeacherAttendanceService). */
    public TeacherWorkReport teacherSessions(Long teacherId, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return teacherAttendanceService.adminReport(teacherId, from, to);
    }

    User findStudentOrThrow(Long studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Khoang thoi gian khong hop le");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Khoang thoi gian toi da 366 ngay");
        }
    }
}
