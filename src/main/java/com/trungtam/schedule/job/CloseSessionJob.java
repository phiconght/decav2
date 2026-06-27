package com.trungtam.schedule.job;

import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Job dong buoi (mac dinh 23:30): buoi PLANNED da qua gio ket thuc -> DONE.
 * Cau hinh app.jobs.close-session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.close-session",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class CloseSessionJob {

    private final ClassSessionRepository sessionRepository;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Scheduled(cron = "${app.jobs.close-session.cron:0 30 23 * * *}")
    @Transactional
    public void close() {
        ZoneId zone = ZoneId.of(timezone);
        LocalDate today = LocalDate.now(zone);
        LocalTime nowTime = LocalTime.now(zone);
        List<ClassSession> past = sessionRepository.findPlannedPastEnd(today, nowTime);
        for (ClassSession s : past) {
            s.setStatus(SessionStatus.DONE);
        }
        if (!past.isEmpty()) {
            sessionRepository.saveAll(past);
            log.info("[close-session] da dong {} buoi PLANNED qua gio -> DONE", past.size());
        }
    }
}
