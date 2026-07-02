package com.trungtam.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.dto.request.SubmitExamRequest;
import com.trungtam.exam.dto.response.ExamGradeResponse;
import com.trungtam.exam.dto.response.ExamGradeResponse.QuestionGrade;
import com.trungtam.exam.dto.response.ExamPaperResponse;
import com.trungtam.exam.dto.response.ExamPaperResponse.PaperOption;
import com.trungtam.exam.dto.response.ExamPaperResponse.PaperQuestion;
import com.trungtam.exam.dto.response.ExamPaperResponse.PaperTfItem;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamExercise;
import com.trungtam.exam.entity.ExamStatus;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.entity.ExamStudentStatus;
import com.trungtam.exam.entity.ExamTfItemScore;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseType;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Luong hoc vien LAM BAI de thi (self-scoped theo user dang dang nhap):
 * lay de + cau hoi, luu nhap, nop bai va cham diem MC/TF (tu luan cham tay).
 *
 * Quyen truy cap dua tren dong exam_student cua chinh hoc vien:
 * - Khong co dong / DA_XOA -> EXAM_NOT_AVAILABLE.
 * - CHUA_PHAT_HANH: tu dong chuyen DA_PHAT_HANH khi de ACTIVE va da toi
 *   publishAt (khop voi danh sach "de da/dang phat" o man Khoa hoc mobile).
 *   Admin muon chan rieng 1 hoc vien thi dung DA_XOA.
 * - Qua han (deadline) ma chua nop -> chi xem (QUA_HAN, lo dap an).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamTakingService {

    /** Do tre cho phep khi nop bai (mang cham, client tu nop khi het gio). */
    private static final Duration SUBMIT_GRACE = Duration.ofSeconds(60);

    private final ExamRepository examRepository;
    private final ExamStudentRepository examStudentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ExamPaperResponse getPaper(Long examId) {
        Instant now = Instant.now();
        Exam exam = findExam(examId);
        ExamStudent es = findAvailableRow(exam, now);

        String effective;
        Instant deadline = null;
        if (es.getStatus() == ExamStudentStatus.DA_LAM) {
            effective = "DA_LAM";
        } else {
            Instant dl = deadline(exam, es);
            if (dl != null && now.isAfter(dl)) {
                effective = "QUA_HAN";
            } else {
                if (es.getStartedAt() == null) {
                    es.setStartedAt(now);
                }
                es.setStatus(ExamStudentStatus.DANG_KIEM_TRA);
                effective = "DANG_KIEM_TRA";
                deadline = deadline(exam, es);
            }
        }
        boolean reveal = "DA_LAM".equals(effective) || "QUA_HAN".equals(effective);

        SubmitExamRequest answers = parseAnswers(es.getAnswers());
        ExamGradeResponse result = reveal ? grade(exam, answers) : null;

        return new ExamPaperResponse(
                exam.getId(),
                exam.getCode(),
                exam.getName(),
                exam.getDurationMinutes(),
                deadline,
                effective,
                buildQuestions(exam, reveal),
                es.getAnswers() == null ? null : answers,
                es.getScore(),
                result
        );
    }

    @Transactional
    public ExamGradeResponse submit(Long examId, SubmitExamRequest req) {
        Instant now = Instant.now();
        Exam exam = findExam(examId);
        ExamStudent es = findAvailableRow(exam, now);

        if (es.getStatus() == ExamStudentStatus.DA_LAM) {
            throw new AppException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }
        Instant dl = deadline(exam, es);
        if (dl != null && now.isAfter(dl.plus(SUBMIT_GRACE))) {
            throw new AppException(ErrorCode.EXAM_TIME_OVER);
        }

        ExamGradeResponse result = grade(exam, req);
        if (es.getStartedAt() == null) {
            es.setStartedAt(now);
        }
        es.setAnswers(toJson(req));
        es.setScore(BigDecimal.valueOf(result.earned()).setScale(2, RoundingMode.HALF_UP));
        es.setSubmittedAt(now);
        es.setStatus(ExamStudentStatus.DA_LAM);
        return result;
    }

    @Transactional
    public void saveDraft(Long examId, SubmitExamRequest req) {
        Instant now = Instant.now();
        Exam exam = findExam(examId);
        ExamStudent es = findAvailableRow(exam, now);

        if (es.getStatus() == ExamStudentStatus.DA_LAM) {
            throw new AppException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }
        if (es.getStartedAt() == null) {
            es.setStartedAt(now);
        }
        es.setStatus(ExamStudentStatus.DANG_KIEM_TRA);
        es.setAnswers(toJson(req));
    }

    // ---- helpers ----

    private Exam findExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));
    }

    /**
     * Dong exam_student cua user hien tai; nem EXAM_NOT_AVAILABLE neu chua
     * duoc phat (xem javadoc class). Tu dong phat de khi toi publishAt.
     */
    private ExamStudent findAvailableRow(Exam exam, Instant now) {
        User user = userRepository.findByUsername(SecurityUtils.requireCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        ExamStudent es = examStudentRepository.findByExamIdAndUserId(exam.getId(), user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_AVAILABLE));

        if (es.getStatus() == ExamStudentStatus.DA_XOA) {
            throw new AppException(ErrorCode.EXAM_NOT_AVAILABLE);
        }
        if (es.getStatus() == ExamStudentStatus.CHUA_PHAT_HANH) {
            boolean published = exam.getStatus() == ExamStatus.ACTIVE
                    && exam.getPublishAt() != null
                    && !exam.getPublishAt().isAfter(now);
            if (!published) {
                throw new AppException(ErrorCode.EXAM_NOT_AVAILABLE);
            }
            es.setStatus(ExamStudentStatus.DA_PHAT_HANH);
        }
        return es;
    }

    /**
     * Han lam bai = som nhat cua (startedAt + durationMinutes) va exam.endAt.
     * Chua bat dau -> chi tinh theo endAt. Null = khong gioi han.
     */
    private Instant deadline(Exam exam, ExamStudent es) {
        Instant byDuration = es.getStartedAt() != null && exam.getDurationMinutes() != null
                ? es.getStartedAt().plus(Duration.ofMinutes(exam.getDurationMinutes()))
                : null;
        Instant end = exam.getEndAt();
        if (byDuration == null) return end;
        if (end == null) return byDuration;
        return byDuration.isBefore(end) ? byDuration : end;
    }

    private List<PaperQuestion> buildQuestions(Exam exam, boolean reveal) {
        return exam.getExamExercises().stream()
                .sorted(Comparator.comparingInt(ExamExercise::getSortOrder))
                .map(ee -> toQuestion(ee, reveal))
                .toList();
    }

    private PaperQuestion toQuestion(ExamExercise ee, boolean reveal) {
        Exercise ex = ee.getExercise();
        List<PaperOption> options = null;
        List<PaperTfItem> tfItems = null;
        String essayAnswer = null;
        String essayAnswerImage = null;

        if (ex.getType() == ExerciseType.MULTIPLE_CHOICE) {
            options = ex.getOptions().stream()
                    .map(o -> new PaperOption(
                            o.getId(),
                            o.getSortOrder(),
                            o.getText(),
                            o.getImage(),
                            reveal ? o.isCorrect() : null))
                    .toList();
        } else if (ex.getType() == ExerciseType.TRUE_FALSE) {
            tfItems = ex.getTrueFalseItems().stream()
                    .map(t -> new PaperTfItem(
                            t.getId(),
                            t.getSortOrder(),
                            t.getText(),
                            t.getImage(),
                            reveal ? t.isAnswer() : null))
                    .toList();
        } else if (ex.getType() == ExerciseType.ESSAY && reveal) {
            essayAnswer = ex.getEssayAnswer();
            essayAnswerImage = ex.getEssayAnswerImage();
        }

        return new PaperQuestion(
                ee.getId(),
                ex.getId(),
                ex.getType().name(),
                questionPoints(ee),
                ex.getQuestionText(),
                ex.getQuestionImage(),
                options,
                tfItems,
                essayAnswer,
                essayAnswerImage
        );
    }

    /**
     * Diem toi da cua 1 cau: TF co bang diem theo y -> tong diem cac y
     * (de hien thi khop cach cham); nguoc lai lay exam_exercises.points.
     */
    private double questionPoints(ExamExercise ee) {
        Exercise ex = ee.getExercise();
        if (ex.getType() == ExerciseType.TRUE_FALSE && !ee.getItemScores().isEmpty()) {
            return ee.getItemScores().stream()
                    .map(ExamTfItemScore::getPoints)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();
        }
        return ee.getPoints() != null ? ee.getPoints().doubleValue() : 0;
    }

    /** Cham bai: MC dung/sai theo dap an; TF theo bang diem tung y (neu co) hoac ti le y dung; tu luan 0 diem cho cham tay. */
    private ExamGradeResponse grade(Exam exam, SubmitExamRequest req) {
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

    private String toJson(SubmitExamRequest req) {
        try {
            return objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private SubmitExamRequest parseAnswers(String json) {
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
}
