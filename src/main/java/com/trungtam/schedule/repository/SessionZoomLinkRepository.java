package com.trungtam.schedule.repository;

import com.trungtam.schedule.entity.SessionZoomLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionZoomLinkRepository extends JpaRepository<SessionZoomLink, Long> {

    List<SessionZoomLink> findBySessionIdOrderBySortOrderAsc(Long sessionId);

    long countBySessionId(Long sessionId);
}
