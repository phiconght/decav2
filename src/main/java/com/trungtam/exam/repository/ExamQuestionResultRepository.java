package com.trungtam.exam.repository;

import com.trungtam.exam.entity.ExamQuestionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ExamQuestionResultRepository extends JpaRepository<ExamQuestionResult, Long> {

    @Modifying
    @Query("DELETE FROM ExamQuestionResult r WHERE r.examStudent.id = :examStudentId")
    void deleteByExamStudentId(@Param("examStudentId") Long examStudentId);

    @Query("SELECT COALESCE(SUM(r.maxPoints), 0) FROM ExamQuestionResult r WHERE r.examStudent.id = :examStudentId")
    BigDecimal sumMaxPointsByExamStudentId(@Param("examStudentId") Long examStudentId);

    /**
     * Cac bai DA_LAM chua co ket qua tung cau (phuc vu backfill bai nop
     * truoc khi co bang exam_question_result). Chi lay de con cau hoi.
     */
    @Query(value = """
            SELECT es.id FROM exam_student es
            WHERE es.status = 'DA_LAM'
              AND NOT EXISTS (
                  SELECT 1 FROM exam_question_result r WHERE r.exam_student_id = es.id)
              AND EXISTS (
                  SELECT 1 FROM exam_exercises ee WHERE ee.exam_id = es.exam_id)
            ORDER BY es.id
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Long> findBackfillCandidateIds(@Param("batchSize") int batchSize);
}
