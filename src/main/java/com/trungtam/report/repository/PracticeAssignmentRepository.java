package com.trungtam.report.repository;

import com.trungtam.report.entity.PracticeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PracticeAssignmentRepository extends JpaRepository<PracticeAssignment, Long> {

    Optional<PracticeAssignment> findByExamId(Long examId);

    List<PracticeAssignment> findByStudentIdAndSchoolClassIdOrderByCreatedAtDesc(Long studentId, Long classId);

    /** So de PH giao cho HV trong lop hom nay (chong max-per-day). */
    long countByParentIdAndStudentIdAndSchoolClassIdAndCreatedAtAfter(
            Long parentId, Long studentId, Long classId, Instant since);

    /** So de chua nop (max-pending). */
    long countByStudentIdAndSchoolClassIdAndStatus(Long studentId, Long classId, String status);
}
