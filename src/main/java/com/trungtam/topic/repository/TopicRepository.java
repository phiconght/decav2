package com.trungtam.topic.repository;

import com.trungtam.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    /** Chuyen de cua 1 mon, sap theo so thu tu roi ten. */
    List<Topic> findBySubjectIdOrderBySortOrderAscNameAsc(Long subjectId);
}
