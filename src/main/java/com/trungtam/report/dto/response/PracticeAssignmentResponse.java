package com.trungtam.report.dto.response;

import java.time.Instant;

/**
 * De luyen tap vua sinh (tra cho PH xem gom gi) — cung dung cho danh sach
 * "Bai phu huynh giao" cua HV (11/08/2026, xem §10.11): topicId/topicName co
 * the null (de toan khoa). classId/className/durationMinutes de HV biet de
 * thuoc lop nao va lam bai (khong can goi them API de lay durationMinutes).
 */
public record PracticeAssignmentResponse(
        Long assignmentId,
        Long examId,
        String examCode,
        String examName,
        Long classId,
        String className,
        Long topicId,
        String topicName,
        int numQuestions,
        Integer durationMinutes,
        DifficultyCount byDifficulty,
        TypeCount byType,
        Instant deadline,
        String status
) {
    public record DifficultyCount(int easy, int medium, int hard) {
    }

    public record TypeCount(int multipleChoice, int trueFalse) {
    }
}
