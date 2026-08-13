package com.trungtam.schedule.repository;

import com.trungtam.schedule.entity.SessionVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionVideoRepository extends JpaRepository<SessionVideo, Long> {

    List<SessionVideo> findBySessionIdOrderBySortOrderAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);

    boolean existsByVideoId(Long videoId);
}
