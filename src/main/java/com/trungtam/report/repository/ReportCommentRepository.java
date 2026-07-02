package com.trungtam.report.repository;

import com.trungtam.report.entity.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {

    List<ReportComment> findByStudentIdAndSchoolClassIdOrderByCreatedAtDesc(Long studentId, Long classId);

    List<ReportComment> findByExamStudentIdOrderByCreatedAtDesc(Long examStudentId);
}
