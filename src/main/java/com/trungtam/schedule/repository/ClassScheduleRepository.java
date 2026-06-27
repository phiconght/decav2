package com.trungtam.schedule.repository;

import com.trungtam.schedule.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findByClazzIdAndActiveTrue(Long classId);

    List<ClassSchedule> findByActiveTrue();
}
