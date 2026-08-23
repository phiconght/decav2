package com.trungtam.exercise.repository;

import com.trungtam.exercise.entity.ImportBatch;
import com.trungtam.exercise.entity.ImportBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    List<ImportBatch> findByCreatedByOrderByIdDesc(String createdBy);

    long countByCreatedByAndStatus(String createdBy, ImportBatchStatus status);
}
