package com.trungtam.schoolclass.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamStatus;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.report.repository.ReportAggregationRepository;
import com.trungtam.report.service.ClassReportService;
import com.trungtam.report.service.StudentReportService;
import com.trungtam.schedule.entity.AttendanceStatus;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionAttendance;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.repository.SessionAttendanceRepository;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse.OutlineExam;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse.OutlineProgress;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse.OutlineSession;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse.OutlineTopicGroup;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityService;
import com.trungtam.guardian.repository.StudentParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dung cay noi dung khoa hoc cho man Chi tiet khoa hoc (Mobile):
 * CHUYEN DE -> (buoi hoc + de thi).
 *
 * <p>Xem SPEC_KhoaHoc_NoiDung_Mobile.md §3.3.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassOutlineService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final SchoolClassRepository classRepository;
    private final ClassSessionRepository sessionRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final ExamRepository examRepository;
    private final ExamStudentRepository examStudentRepository;
    private final StudentParentRepository studentParentRepository;
    private final SecurityService securityService;
    // Dung LAI phep tinh chuyen can cua module Bao cao — KHONG viet truy van
    // rieng. Neu tu tinh lai, he thong se co 2 duong tinh doc lap va man Khoa
    // hoc co the hien % khac man Bao cao cho cung 1 lop (SPEC §3.3 C2).
    private final StudentReportService studentReportService;
    private final ClassReportService classReportService;
    private final ReportAggregationRepository aggregationRepository;

    public ClassOutlineResponse outline(Long classId, Long studentIdParam) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SchoolClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        if (!securityService.canViewClassContent(classId, auth)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Long studentId = resolveStudentId(auth, classId, studentIdParam);
        boolean seesUnpublished = hasAuthority(auth, "EXAM:READ");

        // ---- buoi hoc: lay TOAN BO (ke ca da qua va da huy) ----
        List<ClassSession> sessions = new ArrayList<>(sessionRepository.findByClazzId(classId));
        sessions.sort(Comparator.comparing(ClassSession::getSessionDate)
                .thenComparing(ClassSession::getStartTime));

        // Diem danh cua hoc vien dang xem (1 query, khong hoi tung buoi).
        Map<Long, SessionAttendance> attBySession = studentId == null
                ? Map.of()
                : attendanceRepository.findByUserIdAndClassId(studentId, classId).stream()
                        .collect(Collectors.toMap(a -> a.getSession().getId(), a -> a, (a, b) -> a));

        // ---- de thi cua lop ----
        List<Exam> exams = examRepository.findByClassId(classId).stream()
                .filter(e -> seesUnpublished || isPublished(e))
                .sorted(Comparator.comparing(Exam::getPublishAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<Long, ExamStudent> esByExam = studentId == null
                ? Map.of()
                : examStudentRepository.findActiveForStudentInClass(studentId, classId).stream()
                        .collect(Collectors.toMap(es -> es.getExam().getId(), es -> es, (a, b) -> a));

        // Diem toi da cua MOI de trong 1 query (tranh N+1) — de FE to mau
        // the theo ti le diem, giong RecentExamCards dang dung o cac man khac.
        Map<Long, java.math.BigDecimal> maxScoreByExam = exams.isEmpty()
                ? Map.of()
                : aggregationRepository.examMaxScoresForExams(exams.stream().map(Exam::getId).toList()).stream()
                        .collect(Collectors.toMap(
                                ReportAggregationRepository.ExamMaxScoreProjection::getExamId,
                                ReportAggregationRepository.ExamMaxScoreProjection::getMaxScore));

        // ---- gom nhom theo chuyen de ----
        // LinkedHashMap giu thu tu chen; sap xep lai o buoc cuoi.
        Map<Long, GroupAcc> byTopic = new LinkedHashMap<>();
        int ordinal = 1;
        for (ClassSession s : sessions) {
            Long topicId = s.getTopic() != null ? s.getTopic().getId() : null;
            GroupAcc g = byTopic.computeIfAbsent(topicId, k -> new GroupAcc(
                    topicId,
                    s.getTopic() != null ? s.getTopic().getName() : null,
                    s.getTopic() != null ? s.getTopic().getSortOrder() : null));
            // Danh so TOAN BO buoi theo thoi gian, KE CA buoi da huy: neu bo
            // buoi huy ra thi moi lan huy se doi so cua tat ca buoi phia sau
            // (hoc vien dang nho "Buoi 5" hom sau thanh "Buoi 4").
            g.sessions.add(toSession(s, ordinal++, attBySession.get(s.getId())));
        }
        for (Exam e : exams) {
            Long topicId = e.getTopic() != null ? e.getTopic().getId() : null;
            GroupAcc g = byTopic.computeIfAbsent(topicId, k -> new GroupAcc(
                    topicId,
                    e.getTopic() != null ? e.getTopic().getName() : null,
                    e.getTopic() != null ? e.getTopic().getSortOrder() : null));
            g.exams.add(toExam(e, esByExam.get(e.getId()), maxScoreByExam.get(e.getId())));
        }

        List<OutlineTopicGroup> groups = byTopic.values().stream()
                .sorted(GROUP_ORDER)
                .map(g -> new OutlineTopicGroup(
                        g.topicId, g.topicName, g.sortOrder, g.sessions, g.exams))
                .toList();

        return new ClassOutlineResponse(
                clazz.getId(), clazz.getCode(), clazz.getName(),
                clazz.getSubject() != null ? clazz.getSubject().getName() : null,
                clazz.getSubject() != null ? clazz.getSubject().getGradeLevel() : null,
                clazz.getStartDate(), clazz.getEndDate(),
                clazz.getStatus() != null ? clazz.getStatus().name() : null,
                progress(classId, studentId, sessions),
                groups);
    }

    // ============================ quyen / pham vi ============================

    /**
     * Xac dinh xem dang hien diem danh cua AI.
     * STUDENT -> luon la chinh minh (bo qua tham so, chong leo thang quyen).
     * PARENT  -> BAT BUOC truyen studentId va phai la con minh + dang hoc lop nay.
     * Nhan vien/GV -> tuy chon; co truyen thi HV do phai thuoc lop.
     */
    private Long resolveStudentId(Authentication auth, Long classId, Long studentIdParam) {
        Set<String> roles = roles(auth);
        Long myId = securityService.currentUserId(auth);

        if (roles.contains("ROLE_STUDENT")
                && classRepository.existsByIdAndStudents_Id(classId, myId)) {
            return myId;
        }
        if (roles.contains("ROLE_PARENT")
                && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_EMPLOYEE")) {
            // Phu huynh co the co NHIEU con, thanh chi 1 lop -> khong the doan.
            if (studentIdParam == null) {
                throw new AppException(ErrorCode.ACCESS_DENIED,
                        "Vui long chon hoc vien (studentId) de xem noi dung khoa hoc");
            }
            boolean isMyChild = studentParentRepository
                    .findByStudentIdAndParentId(studentIdParam, myId).isPresent();
            if (!isMyChild || !classRepository.existsByIdAndStudents_Id(classId, studentIdParam)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            return studentIdParam;
        }
        if (studentIdParam != null) {
            if (!classRepository.existsByIdAndStudents_Id(classId, studentIdParam)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            return studentIdParam;
        }
        return null;
    }

    // ============================ tien do ============================

    private OutlineProgress progress(Long classId, Long studentId, List<ClassSession> sessions) {
        int total = sessions.size();
        int done = (int) sessions.stream().filter(s -> s.getStatus() == SessionStatus.DONE).count();

        Double rate;
        String scope;
        if (studentId != null) {
            rate = studentReportService.attendance(studentId, classId, null).summary().attendanceRate();
            scope = "STUDENT";
        } else {
            rate = classReportService.attendance(classId, null).summary().attendanceRate();
            scope = "CLASS";
        }
        // Chua tinh duoc (chua co buoi DONE nao) -> tra null ca hai, KHONG tra
        // 0% vi 0% co nghia la "di hoc 0 buoi", khac han "chua bat dau".
        if (rate == null) {
            scope = null;
        }
        return new OutlineProgress(total, done, rate, scope);
    }

    // ============================ mapping ============================

    private OutlineSession toSession(ClassSession s, int ordinal, SessionAttendance a) {
        AttendanceStatus st = a != null ? a.getStatus() : null;
        return new OutlineSession(
                s.getId(),
                ordinal,
                s.getTitle(),
                s.getSessionDate(),
                fmt(s.getStartTime()),
                fmt(s.endTime()),
                s.getRoom() != null ? s.getRoom().getName() : null,
                s.getTeacher() != null ? s.getTeacher().getFullName() : null,
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getCancelReason(),
                st != null ? st.name() : null,
                st == AttendanceStatus.CO_PHEP,
                // Tai lieu buoi hoc thuoc DOT 2 — chua co bang session_materials.
                // Giu san field de dot sau khong phai doi hop dong API.
                0);
    }

    private OutlineExam toExam(Exam e, ExamStudent es, java.math.BigDecimal maxScore) {
        return new OutlineExam(
                e.getId(),
                e.getCode(),
                e.getName(),
                e.getType() != null ? e.getType().name() : null,
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getPublishAt(),
                e.getEndAt(),
                e.getDurationMinutes(),
                es != null && es.getStatus() != null ? es.getStatus().name() : null,
                es != null ? es.getScore() : null,
                maxScore);
    }

    private static boolean isPublished(Exam e) {
        return e.getStatus() == ExamStatus.ACTIVE
                && e.getPublishAt() != null
                && !e.getPublishAt().isAfter(Instant.now());
    }

    private static String fmt(LocalTime t) {
        return t == null ? null : HHMM.format(t);
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Set<String> roles(Authentication auth) {
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /** Nhom "Chua phan chuyen de" (sortOrder null) luon xep CUOI. */
    private static final Comparator<GroupAcc> GROUP_ORDER = (a, b) -> {
        if (a.topicId == null && b.topicId == null) return 0;
        if (a.topicId == null) return 1;
        if (b.topicId == null) return -1;
        int c = Integer.compare(
                a.sortOrder != null ? a.sortOrder : Integer.MAX_VALUE,
                b.sortOrder != null ? b.sortOrder : Integer.MAX_VALUE);
        return c != 0 ? c : Long.compare(a.topicId, b.topicId);
    };

    /** Bo gom tam trong luc dung cay. */
    private static final class GroupAcc {
        private final Long topicId;
        private final String topicName;
        private final Integer sortOrder;
        private final List<OutlineSession> sessions = new ArrayList<>();
        private final List<OutlineExam> exams = new ArrayList<>();

        private GroupAcc(Long topicId, String topicName, Integer sortOrder) {
            this.topicId = topicId;
            this.topicName = topicName;
            this.sortOrder = sortOrder;
        }
    }
}
