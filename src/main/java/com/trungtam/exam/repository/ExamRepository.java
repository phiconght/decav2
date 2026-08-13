package com.trungtam.exam.repository;

import com.trungtam.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {

    @Query("SELECT COUNT(e) FROM Exam e JOIN e.classes c WHERE c.id = :classId")
    long countByClassId(@Param("classId") Long classId);

    /** De thi duoc gan cho 1 lop (qua exam_classes). */
    @Query("SELECT e FROM Exam e JOIN e.classes c WHERE c.id = :classId")
    List<Exam> findByClassId(@Param("classId") Long classId);

    /** De thi gan RIENG 1 buoi hoc (exam.session_id). */
    List<Exam> findBySessionId(Long sessionId);
}
