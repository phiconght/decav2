package com.trungtam.report.dto.response;

import com.trungtam.report.entity.ReportComment;

import java.time.Instant;

/**
 * Nhan xet bao cao tra ve FE.
 */
public record CommentItem(
        Long id,
        Long studentId,
        Long classId,
        Long examStudentId,
        Long authorId,
        String authorName,
        String authorRole,
        String content,
        boolean visibleToStudent,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentItem from(ReportComment c) {
        return new CommentItem(
                c.getId(),
                c.getStudent().getId(),
                c.getSchoolClass().getId(),
                c.getExamStudent() == null ? null : c.getExamStudent().getId(),
                c.getAuthor().getId(),
                c.getAuthor().getFullName(),
                c.getAuthorRole(),
                c.getContent(),
                c.isVisibleToStudent(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
