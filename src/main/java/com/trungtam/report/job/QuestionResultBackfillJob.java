package com.trungtam.report.job;

import com.trungtam.exam.dto.response.ExamGradeResponse;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.repository.ExamQuestionResultRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.exam.service.ExamGradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bu ket qua cham tung cau (exam_question_result) cho cac bai DA_LAM da nop
 * TRUOC khi co bang nay. Cham lai tu answers JSON (logic Java, khong SQL duoc).
 *
 * Idempotent, xu ly theo lo (batch), tu het viec khi khong con candidate.
 * Cau hinh tai app.jobs.question-result-backfill (enabled / fixed-delay / initial-delay).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.question-result-backfill",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class QuestionResultBackfillJob {

    private static final int BATCH_SIZE = 50;

    private final ExamQuestionResultRepository examQuestionResultRepository;
    private final ExamStudentRepository examStudentRepository;
    private final ExamGradingService gradingService;

    @Scheduled(
            fixedDelayString = "${app.jobs.question-result-backfill.fixed-delay:60000}",
            initialDelayString = "${app.jobs.question-result-backfill.initial-delay:45000}")
    @Transactional
    public void backfillMissing() {
        List<Long> ids = examQuestionResultRepository.findBackfillCandidateIds(BATCH_SIZE);
        if (ids.isEmpty()) {
            return;
        }
        int done = 0;
        for (Long esId : ids) {
            ExamStudent es = examStudentRepository.findById(esId).orElse(null);
            if (es == null) {
                continue;
            }
            ExamGradeResponse result = gradingService.grade(es.getExam(),
                    gradingService.parseAnswers(es.getAnswers()));
            gradingService.persistResults(es, result);
            done++;
        }
        log.info("[question-result-backfill] da bu ket qua cau cho {} bai nop", done);
    }
}
