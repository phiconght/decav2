package com.trungtam.exam.repository;

import com.trungtam.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {

    @Query("SELECT COUNT(e) FROM Exam e JOIN e.classes c WHERE c.id = :classId")
    long countByClassId(@Param("classId") Long classId);
}
