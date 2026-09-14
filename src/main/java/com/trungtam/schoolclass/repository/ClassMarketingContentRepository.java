package com.trungtam.schoolclass.repository;

import com.trungtam.schoolclass.entity.ClassMarketingContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassMarketingContentRepository extends JpaRepository<ClassMarketingContent, Long> {
    List<ClassMarketingContent> findByClassIdIn(List<Long> classIds);
}
