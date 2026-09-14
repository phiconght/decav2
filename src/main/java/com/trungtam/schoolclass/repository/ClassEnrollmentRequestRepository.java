package com.trungtam.schoolclass.repository;

import com.trungtam.schoolclass.entity.ClassEnrollmentRequest;
import com.trungtam.schoolclass.entity.EnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRequestRepository extends JpaRepository<ClassEnrollmentRequest, Long> {

    Optional<ClassEnrollmentRequest> findBySchoolClass_IdAndStudent_IdAndStatus(
            Long classId, Long studentId, EnrollmentRequestStatus status);

    List<ClassEnrollmentRequest> findByStatusOrderByCreatedAtDesc(EnrollmentRequestStatus status);

    List<ClassEnrollmentRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByRegistrationCode(String registrationCode);
}
