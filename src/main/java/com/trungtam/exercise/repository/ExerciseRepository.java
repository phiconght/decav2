package com.trungtam.exercise.repository;

import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, JpaSpecificationExecutor<Exercise> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsBySubjectEntityId(Long subjectId);

    List<Exercise> findByImportBatchIdOrderByOrderIndexAsc(Long importBatchId);

    boolean existsByImportBatchIdAndStatus(Long importBatchId, ExerciseStatus status);
}
