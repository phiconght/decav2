package com.trungtam.exercise.repository;

import com.trungtam.exercise.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, JpaSpecificationExecutor<Exercise> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
