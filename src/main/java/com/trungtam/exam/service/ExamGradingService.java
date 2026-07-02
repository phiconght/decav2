package com.trungtam.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.dto.request.SubmitExamRequest;
import com.trungtam.exam.dto.response.ExamGradeResponse;
import com.trungtam.exam.dto.response.ExamGradeResponse.QuestionGrade;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamExercise;
import com.trungtam.exam.entity.ExamQuestionResult;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.entity.ExamTfItemScore;
import com.trungtam.exam.repository.ExamQuestionResultRepository;
import com.trungtam.exercise.entity.Exercise;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Cham diem de thi (tach tu ExamTakingService de dung chung):
 * - grade(): cham MC/TF tu dong, tu luan cho cham tay (correct = null).
 * - questionPoints(): diem toi da 1 cau (TF theo bang diem tung y neu co).
 * - parseAnswers()/toJson(): doc/ghi cot exam_student.answers (JSON).
 * - persistResults(): luu ket qua TUNG CAU vao exam_question_result — nen
 *   tang cho module bao cao (breakdown do kho / loai cau / chuyen de).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamGradingService {

    private final ObjectMapper objectMapper;
    private final ExamQuestionResultRepository examQuestionResultRepository;

    /**
     * Diem toi da cua 1 cau: TF co bang diem theo y -> tong diem cac y
     * (de hien thi khop cach cham); nguoc lai lay exam_exercises.points.
     */
    public double questionPoints(ExamExercise ee) {
        Exercise ex = ee.getExercise();
        if (ex.getType() == com.trungtam.exercise.entity.ExerciseType.TRUE_FALSE
                && !ee.getItemScores().isEmpty()) {
            return ee.getItemScores().stream()
                    .map(ExamTfItemScore::getPoints)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();
        }
        return ee.getPoints() != null ? ee.getPoints().doubleValue() : 0;
    }

    /** Cham bai: MC dung/sai theo dap an; TF theo bang diem tung y (neu co) hoac ti le y dung; tu luan 0 diem cho cham tay. */
    public ExamGradeResponse grade(Exam exam, SubmitExamRequest req) {
        double earned = 0;
        double total = 0;
        int autoCorrect = 0;
        int autoTotal = 0;
        boolean hasEssay = false;
        List<QuestionGrade> byQuestion = new ArrayList<>();

        List<ExamExercise> sorted = exam.getExamExercises().stream()
                .sorted(Comparator.comparingInt(ExamExercise::getSortOrder))
                .toList();
        for (ExamExercise ee : sorted) {
            Exercise ex = ee.getExercise();
            double max = questionPoints(ee);
            total += max;
            switch (ex.getType()) {
                case MULTIPLE_CHOICE -> {
                    autoTotal++;
                    Long picked = req.mc().get(ee.getId());
                    boolean correct = picked != null && ex.getOptions().stream()
                            .anyMatch(o -> o.getId().equals(picked) && o.isCorrect());
                    double gained = correct ? max : 0;
                    earned += gained;
                    if (correct) autoCorrect++;
                    byQuestion.add(new QuestionGrade(ee.getId(), gained, max, correct));
                }
                case TRUE_FALSE -> {
                    autoTotal++;
                    Map<Long, Boolean> picks = req.tf().getOrDefault(ee.getId(), Map.of());
                    double gained;
                    boolean allRight;
                    if (!ee.getItemScores().isEmpty()) {
                        gained = 0;
                        allRight = true;
                        for (ExamTfItemScore s : ee.getItemScores()) {
                            Boolean p = picks.get(s.getTfItem().getId());
                            if (p != null && p == s.getTfItem().isAnswer()) {
                                gained += s.getPoints().doubleValue();
                            } else {
                                allRight = false;
                            }
                        }
                    } else {
                        int items = ex.getTrueFalseItems().size();
                        long right = ex.getTrueFalseItems().stream()
                                .filter(t -> {
                                    Boolean p = picks.get(t.getId());
                                    return p != null && p == t.isAnswer();
                                })
                                .count();
                        gained = items == 0 ? 0 : max * right / items;
                        allRight = items > 0 && right == items;
                    }
                    earned += gained;
                    if (allRight) autoCorrect++;
                    byQuestion.add(new QuestionGrade(ee.getId(), gained, max, allRight));
                }
                case ESSAY -> {
                    hasEssay = true;
                    byQuestion.add(new QuestionGrade(ee.getId(), 0, max, null));
                }
            }
        }
        return new ExamGradeResponse(earned, total, autoCorrect, autoTotal, hasEssay, byQuestion);
    }

    public String toJson(SubmitExamRequest req) {
        try {
            return objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    public SubmitExamRequest parseAnswers(String json) {
        if (json == null || json.isBlank()) {
            return SubmitExamRequest.empty();
        }
        try {
            return objectMapper.readValue(json, SubmitExamRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("exam_student.answers JSON hong, coi nhu rong: {}", e.getMessage());
            return SubmitExamRequest.empty();
        }
    }

    /**
     * Luu ket qua tung cau (snapshot). Idempotent: xoa ket qua cu roi ghi lai
     * (goi khi nop bai / backfill). Map examExerciseId -> ExamExercise tu de.
     */
    @Transactional
    public void persistResults(ExamStudent es, ExamGradeResponse result) {
        examQuestionResultRepository.deleteByExamStudentId(es.getId());
        Map<Long, ExamExercise> byId = es.getExam().getExamExercises().stream()
                .collect(java.util.stream.Collectors.toMap(ExamExercise::getId, Function.identity()));
        List<ExamQuestionResult> rows = new ArrayList<>();
        for (QuestionGrade q : result.byQuestion()) {
            ExamExercise ee = byId.get(q.examExerciseId());
            if (ee == null) {
                continue;
            }
            ExamQuestionResult r = new ExamQuestionResult();
            r.setExamStudent(es);
            r.setExamExercise(ee);
            r.setEarned(BigDecimal.valueOf(q.earned()).setScale(2, RoundingMode.HALF_UP));
            r.setMaxPoints(BigDecimal.valueOf(q.max()).setScale(2, RoundingMode.HALF_UP));
            r.setCorrect(q.correct());
            rows.add(r);
        }
        examQuestionResultRepository.saveAll(rows);
    }
}
