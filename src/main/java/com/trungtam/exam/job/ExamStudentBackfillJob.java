package com.trungtam.exam.job;

import com.trungtam.exam.repository.ExamStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job chay cung du an, dinh ky quet va bu cac map (De thi BY_CLASS - Hoc vien)
 * con thieu, lay tu map Hoc vien - Khoa hoc (class_students).
 *
 * Xu ly cac truong hop bo sot (vd: tao de truoc khi co bang, them HS hang loat,
 * import du lieu...). Idempotent: chi chen dong chua ton tai.
 *
 * Cau hinh tai app.jobs.exam-backfill (enabled / fixed-delay / initial-delay).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.exam-backfill",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ExamStudentBackfillJob {

    private final ExamStudentRepository examStudentRepository;

    /** Chu ky lay tu config; fixedDelay tranh chong lan (chay lai sau khi xong). */
    @Scheduled(
            fixedDelayString = "${app.jobs.exam-backfill.fixed-delay:60000}",
            initialDelayString = "${app.jobs.exam-backfill.initial-delay:30000}")
    @Transactional
    public void backfillMissing() {
        int inserted = examStudentRepository.backfillByClassMissing();
        if (inserted > 0) {
            log.info("[exam-student-backfill] da bu {} dong exam_student con thieu", inserted);
        }
    }
}
