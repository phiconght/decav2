package com.trungtam.schedule.job;

import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.service.TeacherAttendanceService;
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
 * Job quet trang thai buoi hoc, chay moi phut (mac dinh): tu chuyen
 * PLANNED -> IN_PROGRESS khi den gio bat dau, va PLANNED/IN_PROGRESS -> DONE
 * khi qua gio ket thuc. Day la nguon xac dinh DUY NHAT cho trang thai buoi —
 * cac API (vd ClassOutlineService) chi doc lai cot status, KHONG tu tinh gio.
 * Cau hinh app.jobs.session-state.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.session-state",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SessionStateJob {

    private final ClassSessionRepository sessionRepository;
    private final TeacherAttendanceService teacherAttendanceService;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Scheduled(fixedDelayString = "${app.jobs.session-state.fixed-delay:60000}")
    @Transactional
    public void scan() {
        ZoneId zone = ZoneId.of(timezone);
        LocalDate today = LocalDate.now(zone);
        LocalTime nowTime = LocalTime.now(zone);

        // 1) PLANNED, da den gio bat dau -> IN_PROGRESS.
        List<ClassSession> starting = sessionRepository.findPlannedStarted(today, nowTime);
        for (ClassSession s : starting) {
            s.setStatus(SessionStatus.IN_PROGRESS);
        }
        if (!starting.isEmpty()) {
            sessionRepository.saveAll(starting);
        }

        // 2) PLANNED/IN_PROGRESS, da qua gio ket thuc -> DONE. Chay SAU buoc 1
        // trong cung 1 lan quet nen buoi ngan (bat dau va ket thuc trong cung
        // 1 phut) van duoc dong dung, khong bi ket o IN_PROGRESS.
        List<ClassSession> ending = sessionRepository.findOpenPastEnd(today, nowTime);
        for (ClassSession s : ending) {
            s.setStatus(SessionStatus.DONE);
            // GV khong cham cong buoi da ket thuc -> danh VANG (idempotent)
            teacherAttendanceService.markAbsentIfMissing(s);
        }
        if (!ending.isEmpty()) {
            sessionRepository.saveAll(ending);
        }

        if (!starting.isEmpty() || !ending.isEmpty()) {
            log.info("[session-state] IN_PROGRESS={} DONE={}", starting.size(), ending.size());
        }
    }
}
