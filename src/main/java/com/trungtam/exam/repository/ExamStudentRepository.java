package com.trungtam.exam.repository;

import com.trungtam.exam.entity.ExamStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamStudentRepository extends JpaRepository<ExamStudent, Long> {

    Optional<ExamStudent> findByExamIdAndUserId(Long examId, Long userId);

    boolean existsByExamIdAndUserId(Long examId, Long userId);

    /**
     * Bu cac dong exam_student con thieu cho de BY_CLASS, lay tu map hoc vien - khoa
     * (class_students). Idempotent nho NOT EXISTS + unique(exam_id,user_id).
     * Tra ve so dong vua chen.
     */
    @Modifying
    @Query(value = """
            INSERT INTO exam_student (exam_id, user_id, source, status, created_at, created_by)
            SELECT DISTINCT ec.exam_id, cs.user_id, 'CLASS', 'CHUA_PHAT_HANH', now(), 'system'
            FROM exam_classes ec
            JOIN exams e          ON e.id = ec.exam_id AND e.type = 'BY_CLASS'
            JOIN class_students cs ON cs.class_id = ec.class_id
            WHERE NOT EXISTS (
                SELECT 1 FROM exam_student es
                WHERE es.exam_id = ec.exam_id AND es.user_id = cs.user_id
            )
            """, nativeQuery = true)
    int backfillByClassMissing();

    /**
     * Cac de (qua exam_student) cua 1 hoc vien trong 1 khoa, da loai DA_XOA.
     * Chi lay de co gan voi khoa classId (qua exam_classes).
     */
    @Query("""
            SELECT es FROM ExamStudent es
            JOIN es.exam e
            WHERE es.user.id = :userId
              AND es.status <> com.trungtam.exam.entity.ExamStudentStatus.DA_XOA
              AND EXISTS (SELECT 1 FROM e.classes c WHERE c.id = :classId)
            ORDER BY e.publishAt ASC NULLS LAST, e.id DESC
            """)
    List<ExamStudent> findActiveForStudentInClass(@Param("userId") Long userId,
                                                  @Param("classId") Long classId);

    /**
     * Bai da nop (DA_LAM) nhung CHUA duoc nhan vien/GV xac nhan — hang cho
     * xac nhan truoc khi tinh vao bao cao (yeu cau nguoi dung 13/08/2026).
     */
    @Query("""
            SELECT es FROM ExamStudent es
            JOIN es.exam e
            WHERE es.status = com.trungtam.exam.entity.ExamStudentStatus.DA_LAM
              AND es.confirmedAt IS NULL
              AND (:examId IS NULL OR e.id = :examId)
              AND (:classId IS NULL OR EXISTS (SELECT 1 FROM e.classes c WHERE c.id = :classId))
            ORDER BY es.submittedAt ASC
            """)
    Page<ExamStudent> findPendingConfirmation(@Param("examId") Long examId,
                                              @Param("classId") Long classId,
                                              Pageable pageable);

    List<ExamStudent> findByIdIn(List<Long> ids);
}
