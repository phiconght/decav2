package com.trungtam.schoolclass.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Cay noi dung khoa hoc: CHUYEN DE -> (buoi hoc + de thi).
 * Mot endpoint tra san cay, thay vi de client tu ghep 2 nguon roi rac.
 * Xem SPEC_KhoaHoc_NoiDung_Mobile.md §3.3.
 */
public record ClassOutlineResponse(
        Long classId,
        String code,
        String name,
        String subjectName,
        String gradeLevel,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        OutlineProgress progress,
        List<OutlineTopicGroup> groups
) {

    /**
     * Tien do khoa hoc.
     *
     * <p>{@code attendanceScope} cho biet {@code attendanceRate} dang noi ve AI —
     * neu thieu, GV se thay "Chuyen can 87%" ma tuong la cua mot hoc vien nao do:
     * <ul>
     *   <li>{@code "STUDENT"} — chuyen can cua 1 hoc vien cu the</li>
     *   <li>{@code "CLASS"} — trung binh ca lop (GV/admin khong chon hoc vien)</li>
     *   <li>{@code null} — chua tinh duoc (chua co buoi DONE nao)</li>
     * </ul>
     */
    public record OutlineProgress(
            int totalSessions,
            int doneSessions,
            Double attendanceRate,
            String attendanceScope
    ) {}

    /** {@code topicId = null} => nhom "Chua phan chuyen de", luon xep CUOI. */
    public record OutlineTopicGroup(
            Long topicId,
            String topicName,
            Integer sortOrder,
            List<OutlineSession> sessions,
            List<OutlineExam> exams
    ) {}

    public record OutlineSession(
            Long sessionId,
            Integer ordinal,
            String title,
            LocalDate date,
            String startTime,
            String endTime,
            String roomName,
            String teacherName,
            String status,
            String cancelReason,
            String attendanceStatus,
            boolean onLeave,
            int materialCount,
            /**
             * De thi cua RIENG buoi nay (exam.session_id = buoi nay) — hien
             * ngay sau buoi hoc thay vi don rieng cuoi nhom chuyen de. De
             * khong gan buoi nao (session_id null, legacy) van nam o
             * {@link OutlineTopicGroup#exams()}.
             */
            List<OutlineExam> exams
    ) {}

    public record OutlineExam(
            Long examId,
            String code,
            String name,
            String type,
            /** Trang thai DE (ACTIVE/INACTIVE) — mobile can de dung lai model Exam. */
            String status,
            Instant publishAt,
            Instant endAt,
            Integer durationMinutes,
            /** Trang thai BAI LAM cua hoc vien dang xem (null neu khong xet HV nao). */
            String studentStatus,
            BigDecimal score,
            /** Diem toi da cua de — de FE to mau the theo ti le diem (score/maxScore). */
            BigDecimal maxScore
    ) {}
}
