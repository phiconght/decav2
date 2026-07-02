package com.trungtam.report.dto.response;

import java.time.Instant;

/** De luyen tap vua sinh (tra cho PH xem gom gi). topicId/topicName luon co. */
public record PracticeAssignmentResponse(
        Long assignmentId,
        Long examId,
        String examCode,
        String examName,
        Long topicId,
        String topicName,
        int numQuestions,
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
