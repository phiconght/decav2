package com.trungtam.schedule.job;

import com.trungtam.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Job dau ngay (mac dinh 00:05): tao session_attendance cho roster cua moi buoi
 * PLANNED trong ngay, doi chieu leave da duyet -> CO_PHEP.
 * Cau hinh app.jobs.attendance-act.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.attendance-act",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AttendanceActivationJob {

    private final ScheduleService scheduleService;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Scheduled(cron = "${app.jobs.attendance-act.cron:0 5 0 * * *}")
    public void activate() {
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        int created = scheduleService.activateAttendance(today);
        if (created > 0) {
            log.info("[attendance-act] da tao {} dong diem danh cho ngay {}", created, today);
        }
    }
}
