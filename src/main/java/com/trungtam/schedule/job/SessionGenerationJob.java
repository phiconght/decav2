package com.trungtam.schedule.job;

import com.trungtam.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job sinh buoi (mac dinh 01:00 hang ngay): voi moi quy tac active, goi
 * generate(dryRun=false) mo rong chan troi (horizon-weeks). Idempotent theo uq.
 * Cau hinh app.jobs.schedule-gen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.schedule-gen",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SessionGenerationJob {

    private final ScheduleService scheduleService;

    @Value("${app.jobs.schedule-gen.horizon-weeks:8}")
    private int horizonWeeks;

    @Scheduled(cron = "${app.jobs.schedule-gen.cron:0 0 1 * * *}")
    public void generate() {
        int created = scheduleService.generateHorizon(horizonWeeks);
        if (created > 0) {
            log.info("[schedule-gen] da bao dam {} buoi (idempotent) tu cac quy tac active", created);
        }
    }
}
