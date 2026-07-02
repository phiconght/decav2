package com.trungtam.payment.repository;

import com.trungtam.payment.entity.ClassStudentId;
import com.trungtam.payment.entity.ClassStudentPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassStudentPricingRepository
        extends JpaRepository<ClassStudentPricing, ClassStudentId> {

    @Query("""
        SELECT p FROM ClassStudentPricing p
        WHERE p.clazz.id = :classId AND p.student.id = :studentId
        """)
    Optional<ClassStudentPricing> findByClassAndStudent(@Param("classId") Long classId,
                                                        @Param("studentId") Long studentId);
}
