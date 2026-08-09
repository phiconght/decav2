package com.trungtam.report.dto.response;

import java.util.List;

/** Gop toan bo du lieu bao cao 1 HV trong 1 lop (cho MAU IN — 1 call). */
public record StudentClassSummaryResponse(
        StudentInfo student,
        ClassInfo clazz,
        List<RecentExamItem> exams,
        List<ScoreTrendPoint> trend,
        BreakdownResponse breakdown,
        List<TopicMasteryItem> topicMastery,
        StudentAttendanceReport attendance,
        List<CommentItem> comments,
        ReportAnalysisResponse analysis
) {
    public record StudentInfo(
            Long id,
            String fullName,
            String username,
            String email,
            String phone
    ) {
    }

    public record ClassInfo(
            Long id,
            String code,
            String name,
            String subjectName,
            String teacherNames
    ) {
    }
}
