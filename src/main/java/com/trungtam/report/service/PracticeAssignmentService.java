package com.trungtam.report.service;

import com.trungtam.common.codegen.CodeGeneratorService;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamExercise;
import com.trungtam.exam.entity.ExamStatus;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.entity.ExamStudentSource;
import com.trungtam.exam.entity.ExamStudentStatus;
import com.trungtam.exam.entity.ExamType;
import com.trungtam.exam.event.ExamSubmittedEvent;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.repository.ExerciseRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.service.NotificationService;
import com.trungtam.report.dto.request.AssignPracticeRequest;
import com.trungtam.report.dto.response.PracticeAssignmentResponse;
import com.trungtam.report.dto.response.PracticeAssignmentResponse.DifficultyCount;
import com.trungtam.report.dto.response.PracticeAssignmentResponse.TypeCount;
import com.trungtam.report.entity.PracticeAssignment;
import com.trungtam.report.repository.PracticeAssignmentRepository;
import com.trungtam.report.repository.PracticeBankRepository;
import com.trungtam.report.repository.PracticeBankRepository.BankExercise;
import com.trungtam.report.repository.ReportAggregationRepository;
import com.trungtam.report.repository.ReportAggregationRepository.BreakdownProjection;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
import com.trungtam.topic.entity.Topic;
import com.trungtam.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Giao bai luyen tap (§10): chon 1 chuyen de, tuyen bai theo diem yeu (tren TOAN
 * CHUONG), tao de SUPPLEMENTARY, thong bao HV; khi HV nop -> thong bao nguoi giao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeAssignmentService {

    private final PracticeAssignmentRepository practiceRepository;
    private final PracticeBankRepository bankRepository;
    private final ReportAggregationRepository aggregationRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TopicRepository topicRepository;
    private final ExamRepository examRepository;
    private final ExamStudentRepository examStudentRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CodeGeneratorService codeGeneratorService;

    @Value("${app.practice.num-questions:10}")
    private int numQuestions;
    @Value("${app.practice.duration-minutes:30}")
    private int durationMinutes;
    @Value("${app.practice.deadline-days:7}")
    private int deadlineDays;
    @Value("${app.practice.weak-threshold:0.6}")
    private double weakThreshold;
    @Value("${app.practice.min-sample:3}")
    private int minSample;
    @Value("${app.practice.max-pending:3}")
    private int maxPending;
    @Value("${app.practice.max-per-day:1}")
    private int maxPerDay;
    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    // ======================= GIAO BAI =======================

    @Transactional
    public PracticeAssignmentResponse assign(Long studentId, Long classId, AssignPracticeRequest req) {
        User assigner = currentUser();
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        SchoolClass clazz = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        if (!schoolClassRepository.existsByIdAndStudents_Id(classId, studentId)) {
            throw new AppException(ErrorCode.STUDENT_NOT_IN_CLASS);
        }

        // Bai thi nguon phai thuoc khoa
        Exam sourceExam = examRepository.findById(req.examId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));
        boolean examInClass = sourceExam.getClasses().stream().anyMatch(c -> c.getId().equals(classId));
        if (!examInClass) {
            throw new AppException(ErrorCode.EXAM_NOT_IN_CLASS);
        }

        // Chong spam
        Instant startOfToday = LocalDate.now(ZoneId.of(timezone)).atStartOfDay(ZoneId.of(timezone)).toInstant();
        if (practiceRepository.countByParentIdAndStudentIdAndSchoolClassIdAndCreatedAtAfter(
                assigner.getId(), studentId, classId, startOfToday) >= maxPerDay
                || practiceRepository.countByStudentIdAndSchoolClassIdAndStatus(
                studentId, classId, "ASSIGNED") >= maxPending) {
            throw new AppException(ErrorCode.PRACTICE_LIMIT_REACHED);
        }

        // Buoc 0: chon chuyen de
        Topic topic = resolveTopic(req, sourceExam, studentId, classId, clazz);

        // Buoc 1: nguon danh gia = breakdown TOAN CHUONG
        List<BreakdownProjection> diff = aggregationRepository.difficultyBreakdown(studentId, classId, topic.getId(), null, null);
        List<BreakdownProjection> byType = aggregationRepository.typeBreakdown(studentId, classId, topic.getId(), null, null);

        int[] diffAlloc = difficultyAllocation(focus(diff));   // [easy, medium, hard]
        boolean mcHeavier = mcIsWeaker(byType);                // dang yeu hon chiem 7/10

        // Tuyen bai trong chuyen de
        List<Long> picked = pickExercises(clazz.getSubject().getId(), topic.getId(), studentId,
                diffAlloc, mcHeavier);
        if (picked.size() < 5) {
            throw new AppException(ErrorCode.PRACTICE_BANK_INSUFFICIENT);
        }

        // Dung de SUPPLEMENTARY
        Exam exam = buildExam(clazz, topic, picked);
        exam.getStudents().add(student);
        examRepository.save(exam);

        ExamStudent esRow = new ExamStudent();
        esRow.setExam(exam);
        esRow.setUser(student);
        esRow.setSource(ExamStudentSource.SUPPLEMENTARY);
        esRow.setStatus(ExamStudentStatus.CHUA_PHAT_HANH);
        examStudentRepository.save(esRow);

        PracticeAssignment pa = new PracticeAssignment();
        pa.setExam(exam);
        pa.setSourceExam(sourceExam);
        pa.setStudent(student);
        pa.setParent(assigner);
        pa.setSchoolClass(clazz);
        pa.setStatus("ASSIGNED");
        practiceRepository.save(pa);

        // Thong bao HV
        String payload = String.format("{\"examId\":%d,\"classId\":%d,\"studentId\":%d}",
                exam.getId(), classId, studentId);
        notificationService.notifyFrom(assigner.getId(), studentId, NotificationType.PRACTICE_ASSIGNED,
                "Bài luyện tập mới",
                "Bạn được giao đề luyện tập chương " + topic.getName(),
                "Đề luyện tập được giao",
                "Bạn có đề luyện tập \"" + exam.getName() + "\" (chương " + topic.getName()
                        + "). Mở khóa học để làm bài trước hạn.",
                payload, "practice-assigned:" + exam.getId());

        return toResponse(pa);
    }

    public List<PracticeAssignmentResponse> list(Long studentId, Long classId) {
        return practiceRepository.findByStudentIdAndSchoolClassIdOrderByCreatedAtDesc(studentId, classId)
                .stream().map(this::toResponse).toList();
    }

    // ======================= HV NOP -> BAO NGUOI GIAO =======================

    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExamSubmitted(ExamSubmittedEvent event) {
        ExamStudent es = examStudentRepository.findById(event.examStudentId()).orElse(null);
        if (es == null) {
            return;
        }
        PracticeAssignment pa = practiceRepository.findByExamId(es.getExam().getId()).orElse(null);
        if (pa == null || !pa.getStudent().getId().equals(es.getUser().getId())
                || "SUBMITTED".equals(pa.getStatus())) {
            return;
        }
        pa.setStatus("SUBMITTED");
        practiceRepository.save(pa);

        String score = es.getScore() == null ? "-" : es.getScore().stripTrailingZeros().toPlainString();
        String payload = String.format("{\"examId\":%d,\"classId\":%d,\"studentId\":%d}",
                es.getExam().getId(), pa.getSchoolClass().getId(), pa.getStudent().getId());
        notificationService.notify(pa.getParent().getId(), NotificationType.PRACTICE_SUBMITTED,
                "Đã nộp bài luyện tập",
                pa.getStudent().getFullName() + " đã nộp đề luyện tập (" + score + " điểm)",
                "Bài luyện tập đã hoàn thành",
                pa.getStudent().getFullName() + " vừa nộp \"" + es.getExam().getName()
                        + "\" — điểm " + score + ".",
                payload, "practice-submitted:" + es.getId());
    }

    // ======================= HELPERS =======================

    private Topic resolveTopic(AssignPracticeRequest req, Exam sourceExam, Long studentId,
                               Long classId, SchoolClass clazz) {
        Long topicId = req.topicId();
        if (topicId == null) {
            topicId = sourceExam.getTopic() == null ? null : sourceExam.getTopic().getId();
        }
        if (topicId == null) {
            topicId = bankRepository.inferTopicFromExam(studentId, req.examId());
        }
        if (topicId == null) {
            topicId = aggregationRepository.topicMastery(studentId, classId).stream()
                    .filter(t -> t.getTopicId() != null && t.getMax() != null
                            && t.getMax().signum() > 0)
                    .min(Comparator.comparingDouble(
                            t -> t.getEarned().doubleValue() / t.getMax().doubleValue()))
                    .map(com.trungtam.report.repository.ReportAggregationRepository
                            .TopicMasteryProjection::getTopicId)
                    .orElse(null);
        }
        if (topicId == null) {
            throw new AppException(ErrorCode.PRACTICE_BANK_INSUFFICIENT);
        }
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (!topic.getSubject().getId().equals(clazz.getSubject().getId())) {
            throw new AppException(ErrorCode.TOPIC_SUBJECT_MISMATCH);
        }
        return topic;
    }

    /** Muc do yeu -> quy tac "tien len". */
    private String focus(List<BreakdownProjection> diff) {
        Map<String, BreakdownProjection> m = diff.stream()
                .collect(Collectors.toMap(BreakdownProjection::getBucketKey, b -> b, (a, b) -> a));
        if (weak(m.get("EASY"))) return "EASY";
        if (weak(m.get("MEDIUM"))) return "MEDIUM";
        if (weak(m.get("HARD"))) return "HARD";
        return null;
    }

    private boolean weak(BreakdownProjection b) {
        if (b == null) return false;
        long correct = nz(b.getCorrectCount());
        long incorrect = nz(b.getIncorrectCount());
        long graded = correct + incorrect;
        return graded >= minSample && (double) correct / graded < weakThreshold;
    }

    private int[] difficultyAllocation(String focus) {
        if ("EASY".equals(focus)) return distribute(new int[]{6, 3, 1});
        if ("MEDIUM".equals(focus)) return distribute(new int[]{2, 6, 2});
        if ("HARD".equals(focus)) return distribute(new int[]{1, 3, 6});
        return distribute(new int[]{3, 4, 3});
    }

    /** Scale mau [.,.,.] (tong 10) ve numQuestions, giu tong. */
    private int[] distribute(int[] base10) {
        if (numQuestions == 10) return base10;
        int[] out = new int[3];
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            out[i] = (int) Math.round(base10[i] * numQuestions / 10.0);
            sum += out[i];
        }
        out[0] += numQuestions - sum; // bu chenh lech vao easy
        return out;
    }

    private boolean mcIsWeaker(List<BreakdownProjection> byType) {
        BreakdownProjection mc = byType.stream().filter(b -> "MULTIPLE_CHOICE".equals(b.getBucketKey()))
                .findFirst().orElse(null);
        BreakdownProjection tf = byType.stream().filter(b -> "TRUE_FALSE".equals(b.getBucketKey()))
                .findFirst().orElse(null);
        Double mcPct = correctPct(mc);
        Double tfPct = correctPct(tf);
        if (mcPct == null || tfPct == null) return true; // khong du mau -> mac dinh MC (thuong nhieu bai hon)
        return mcPct <= tfPct; // MC yeu hon (dung it hon) -> chiem nhieu
    }

    private Double correctPct(BreakdownProjection b) {
        if (b == null) return null;
        long g = nz(b.getCorrectCount()) + nz(b.getIncorrectCount());
        return g < minSample ? null : (double) nz(b.getCorrectCount()) / g;
    }

    /** Tuyen bai TRONG chuyen de theo phan bo do kho + ti le dang bai, no long dan. */
    private List<Long> pickExercises(Long subjectId, Long topicId, Long studentId,
                                     int[] diffAlloc, boolean mcHeavier) {
        List<BankExercise> bank = bankRepository.bankByTopic(subjectId, topicId);
        Set<Long> seen = new HashSet<>(bankRepository.seenExerciseIds(studentId));
        Random rnd = new Random(topicId * 31L + studentId);

        // slots do kho
        List<String> diffSlots = new ArrayList<>();
        String[] levels = {"EASY", "MEDIUM", "HARD"};
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < diffAlloc[i]; k++) diffSlots.add(levels[i]);
        }
        int total = diffSlots.size();
        // gan dang bai theo ti le 7/3 (dang yeu hon nhieu hon), spread deu
        int heavyCount = (int) Math.round(total * 0.7);
        List<String> typeSlots = new ArrayList<>();
        String heavy = mcHeavier ? "MULTIPLE_CHOICE" : "TRUE_FALSE";
        String light = mcHeavier ? "TRUE_FALSE" : "MULTIPLE_CHOICE";
        int heavyLeft = heavyCount;
        int lightLeft = total - heavyCount;
        for (int i = 0; i < total; i++) {
            boolean pickHeavy = heavyLeft * (lightLeft + 1) >= lightLeft * (heavyLeft + 1);
            if (heavyLeft == 0) pickHeavy = false;
            else if (lightLeft == 0) pickHeavy = true;
            typeSlots.add(pickHeavy ? heavy : light);
            if (pickHeavy) heavyLeft--; else lightLeft--;
        }

        Set<Long> picked = new java.util.LinkedHashSet<>();
        for (int i = 0; i < total; i++) {
            Long id = draw(bank, diffSlots.get(i), typeSlots.get(i), seen, picked, rnd);
            if (id != null) picked.add(id);
        }
        return new ArrayList<>(picked);
    }

    /** No long dan: (d,t) tuoi -> (d, dang khac) tuoi -> (d,t) da gap -> lan can do kho tuoi. */
    private Long draw(List<BankExercise> bank, String diff, String type, Set<Long> seen,
                      Set<Long> picked, Random rnd) {
        String other = "MULTIPLE_CHOICE".equals(type) ? "TRUE_FALSE" : "MULTIPLE_CHOICE";
        Long id = pick(bank, diff, type, picked, seen, true, rnd);
        if (id == null) id = pick(bank, diff, other, picked, seen, true, rnd);
        if (id == null) id = pick(bank, diff, type, picked, seen, false, rnd);
        if (id == null) id = pick(bank, diff, other, picked, seen, false, rnd);
        if (id == null) {
            for (String nb : neighbors(diff)) {
                id = pick(bank, nb, type, picked, seen, true, rnd);
                if (id == null) id = pick(bank, nb, type, picked, seen, false, rnd);
                if (id != null) break;
            }
        }
        return id;
    }

    private Long pick(List<BankExercise> bank, String diff, String type, Set<Long> picked,
                      Set<Long> seen, boolean freshOnly, Random rnd) {
        List<Long> pool = bank.stream()
                .filter(b -> diff.equals(b.getDifficulty()) && type.equals(b.getType()))
                .map(BankExercise::getId)
                .filter(id -> !picked.contains(id))
                .filter(id -> !freshOnly || !seen.contains(id))
                .collect(Collectors.toList());
        if (pool.isEmpty()) return null;
        return pool.get(rnd.nextInt(pool.size()));
    }

    private String[] neighbors(String diff) {
        return switch (diff) {
            case "EASY" -> new String[]{"MEDIUM", "HARD"};
            case "HARD" -> new String[]{"MEDIUM", "EASY"};
            default -> new String[]{"EASY", "HARD"};
        };
    }

    private Exam buildExam(SchoolClass clazz, Topic topic, List<Long> exerciseIds) {
        Instant now = Instant.now();
        String dateLabel = LocalDate.now(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("dd/MM"));
        Exam exam = new Exam();
        exam.setName("Luyện tập PH giao — " + topic.getName() + " — " + dateLabel);
        exam.setSubject(clazz.getSubject());
        exam.setTopic(topic);
        exam.setType(ExamType.SUPPLEMENTARY);
        exam.setStatus(ExamStatus.ACTIVE);
        exam.setDurationMinutes(durationMinutes);
        exam.setPublishAt(now);
        exam.setEndAt(now.plus(Duration.ofDays(deadlineDays)));
        exam.setCode(codeGeneratorService.generateExamCode(
                clazz.getSubject().getName(), clazz.getSubject().getGradeLevel(), 'B'));
        exam.getClasses().add(clazz);
        // sap xep de-kho de tao da
        List<Exercise> refs = exerciseIds.stream()
                .map(exerciseRepository::getReferenceById)
                .sorted(Comparator.comparingInt(e -> difficultyOrder(e.getDifficulty().name())))
                .collect(Collectors.toList());
        int order = 0;
        for (Exercise ex : refs) {
            ExamExercise ee = new ExamExercise();
            ee.setExam(exam);
            ee.setExercise(ex);
            ee.setPoints(BigDecimal.ONE);
            ee.setSortOrder(order++);
            exam.getExamExercises().add(ee);
        }
        return exam;
    }

    private int difficultyOrder(String d) {
        return switch (d) {
            case "EASY" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }

    private PracticeAssignmentResponse toResponse(PracticeAssignment pa) {
        Exam exam = pa.getExam();
        int easy = 0, medium = 0, hard = 0, mc = 0, tf = 0;
        for (ExamExercise ee : exam.getExamExercises()) {
            Exercise ex = ee.getExercise();
            switch (ex.getDifficulty()) {
                case EASY -> easy++;
                case MEDIUM -> medium++;
                case HARD -> hard++;
            }
            switch (ex.getType()) {
                case MULTIPLE_CHOICE -> mc++;
                case TRUE_FALSE -> tf++;
                default -> { }
            }
        }
        Topic topic = exam.getTopic();
        return new PracticeAssignmentResponse(
                pa.getId(), exam.getId(), exam.getCode(), exam.getName(),
                topic == null ? null : topic.getId(),
                topic == null ? null : topic.getName(),
                exam.getExamExercises().size(),
                new DifficultyCount(easy, medium, hard),
                new TypeCount(mc, tf),
                exam.getEndAt(),
                pa.getStatus());
    }

    private User currentUser() {
        return userRepository.findByUsername(SecurityUtils.requireCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
