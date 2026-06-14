package com.trungtam.common.codegen;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CodeSequence s WHERE s.subjectCode = :subjectCode AND s.gradeCode = :gradeCode AND s.yearOffset = :yearOffset")
    Optional<CodeSequence> findForUpdate(
            @Param("subjectCode") String subjectCode,
            @Param("gradeCode") String gradeCode,
            @Param("yearOffset") int yearOffset);
}
