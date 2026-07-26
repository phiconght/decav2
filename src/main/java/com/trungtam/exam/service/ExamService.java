package com.trungtam.exam.service;

import com.trungtam.common.codegen.CodeGeneratorService;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.dto.request.CreateExamRequest;
import com.trungtam.exam.dto.request.ExamExerciseRequest;
import com.trungtam.exam.dto.request.ExamSearchParams;
import com.trungtam.exam.dto.request.UpdateExamStatusRequest;
import com.trungtam.exam.dto.request.UpdateExamStudentStatusRequest;
import com.trungtam.exam.dto.response.ExamDetailResponse;
import com.trungtam.exam.dto.response.ExamListItem;
import com.trungtam.exam.dto.response.ExamPageResponse;
import com.trungtam.exam.dto.response.StudentExamItem;
import com.trungtam.exam.dto.response.StudentOptionResponse;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamExercise;
import com.trungtam.exam.entity.ExamStatus;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.entity.ExamStudentSource;
import com.trungtam.exam.entity.ExamStudentStatus;
import com.trungtam.exam.entity.ExamTfItemScore;
import com.trungtam.exam.entity.ExamType;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.exam.repository.ExamSpec;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseType;
import com.trungtam.exercise.entity.TrueFalseItem;
import com.trungtam.exercise.repository.ExerciseRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityService;
import com.trungtam.subject.entity.Subject;
import com.trungtam.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ExerciseRepository exerciseRepository;
    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final ExamStudentRepository examStudentRepository;
    private final com.trungtam.exam.repository.ExamQuestionResultRepository examQuestionResultRepository;
    private final com.trungtam.topic.repository.TopicRepository topicRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final SecurityService securityService;

    public ExamPageResponse search(ExamSearchParams params) {
        Specification<Exam> spec = ExamSpec.build(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExamListItem> result = examRepository.findAll(spec, pageable).map(ExamListItem::from);
        return ExamPageResponse.of(result);
    }

    /**
     * Danh sach de thi cua 1 lop, sap xep theo thoi diem phat de (publishAt)
     * tang dan; de chua dat lich (null) xep cuoi. Dung cho mobile hoc vien.
     */
    /**
     * De thi cua 1 lop, GIOI HAN theo nguoi goi.
     *
     * <p>Truoc day ham nay khong scope gi ca (du javadoc ghi "self-scoped"):
     * bat ky nguoi dung da dang nhap nao cung liet ke duoc de thi cua BAT KY
     * lop nao, ke ca de con nhap / chua toi gio phat. App mobile chi che o
     * client nen API van lo. Xem SPEC_KhoaHoc_NoiDung_Mobile.md §3.5.
     *
     * <p>Nay chan 2 tang:
     * <ol>
     *   <li>Phai co quan he voi lop ({@code canViewClassContent}) — cung ham
     *       guard dung cho {@code /classes/&#123;id&#125;/outline}.</li>
     *   <li>Khong co {@code EXAM:READ} (HV/PH) thi chi thay de DA PHAT.</li>
     * </ol>
     */
    public List<ExamListItem> listExamsByClass(Long classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!securityService.canViewClassContent(classId, auth)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        boolean seesUnpublished = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "EXAM:READ".equals(a.getAuthority()));

        return examRepository.findByClassId(classId).stream()
                .filter(e -> seesUnpublished || isPublishedForStudent(e))
                .sorted(Comparator.comparing(Exam::getPublishAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ExamListItem::from)
                .toList();
    }

    /** De HV/PH duoc thay: da ACTIVE va da toi moc phat hanh. */
    private static boolean isPublishedForStudent(Exam e) {
        return e.getStatus() == ExamStatus.ACTIVE
                && e.getPublishAt() != null
                && !e.getPublishAt().isAfter(Instant.now());
    }

    public ExamDetailResponse getById(Long id) {
        return ExamDetailResponse.from(findOrThrow(id));
    }

    @Transactional
    public ExamDetailResponse create(CreateExamRequest req) {
        Subject subject = findSubjectOrThrow(req.subjectId());
        char typeChar = req.type() == ExamType.BY_CLASS ? 'L' : 'B';
        String code = codeGeneratorService.generateExamCode(subject.getName(), subject.getGradeLevel(), typeChar);
        Exam exam = new Exam();
        exam.setCode(code);
        buildExam(exam, req, subject);
        validate(req);
        Exam saved = examRepository.save(exam);
        materializeExamStudents(saved, req);
        return ExamDetailResponse.from(saved);
    }

    @Transactional
    public ExamDetailResponse update(Long id, CreateExamRequest req) {
        Exam exam = findOrThrow(id);
        Subject subject = findSubjectOrThrow(req.subjectId());
        validate(req);
        // Xóa bài tập cũ rồi flush NGAY: tránh lỗi trùng unique uq_exam_exercise
        // (Hibernate vốn insert dòng mới trước khi xóa orphan trong cùng 1 flush)
        exam.getExamExercises().clear();
        examRepository.flush();
        buildExam(exam, req, subject);
        Exam saved = examRepository.save(exam);
        materializeExamStudents(saved, req);
        return ExamDetailResponse.from(saved);
    }

    @Transactional
    public ExamDetailResponse updateStatus(Long id, UpdateExamStatusRequest req) {
        Exam exam = findOrThrow(id);
        exam.setStatus(req.status());
        return ExamDetailResponse.from(examRepository.save(exam));
    }

    @Transactional
    public void delete(Long id) {
        examRepository.delete(findOrThrow(id));
    }

    /** Danh sach khoa hoc cua 1 de (popup cot "So khoa"), kem giao vien + si so. */
    public List<com.trungtam.exam.dto.response.ExamClassItem> listExamClasses(Long examId) {
        Exam exam = findOrThrow(examId);
        return exam.getClasses().stream()
                .sorted(Comparator.comparing(SchoolClass::getCode))
                .map(c -> new com.trungtam.exam.dto.response.ExamClassItem(
                        c.getId(),
                        c.getCode(),
                        c.getName(),
                        c.getSubject().getName(),
                        c.getSubject().getGradeLevel(),
                        c.getTeachers().stream().map(StudentOptionResponse::from).toList(),
                        classRepository.countStudents(c.getId())))
                .toList();
    }

    public List<StudentOptionResponse> listStudentOptions(List<Long> classIds) {
        if (CollectionUtils.isEmpty(classIds)) {
            return List.of();
        }
        return classRepository.findStudentsByClassIds(classIds).stream()
                .map(StudentOptionResponse::from)
                .toList();
    }

    // ---- De thi <-> Hoc vien ----

    /** Danh sach de thi cua 1 hoc vien trong 1 khoa (cho popup man Hoc vien). */
    public List<StudentExamItem> listStudentExamsInClass(Long userId, Long classId) {
        Set<Long> myClassIds = classRepository.findClassesByStudentId(userId).stream()
                .map(SchoolClass::getId)
                .collect(Collectors.toSet());
        Instant now = Instant.now();
        return examStudentRepository.findActiveForStudentInClass(userId, classId).stream()
                .map(es -> toStudentExamItem(es, myClassIds, now))
                .toList();
    }

    /** Admin doi trang thai de cho 1 hoc vien: chi cho DA_PHAT_HANH / CHUA_PHAT_HANH / DA_XOA. */
    @Transactional
    public void updateExamStudentStatus(Long examId, Long userId, UpdateExamStudentStatusRequest req) {
        ExamStudentStatus newStatus = req.status();
        if (newStatus != ExamStudentStatus.DA_PHAT_HANH
                && newStatus != ExamStudentStatus.CHUA_PHAT_HANH
                && newStatus != ExamStudentStatus.DA_XOA) {
            throw new AppException(ErrorCode.EXAM_STATUS_NOT_ALLOWED);
        }
        ExamStudent es = examStudentRepository.findByExamIdAndUserId(examId, userId)
                .orElseGet(() -> {
                    ExamStudent created = new ExamStudent();
                    created.setExam(findOrThrow(examId));
                    created.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
                    created.setSource(ExamStudentSource.CLASS);
                    return created;
                });
        // Roi trang thai DA_LAM -> ket qua cham tung cau khong con hieu luc, xoa di.
        if (es.getStatus() == ExamStudentStatus.DA_LAM && newStatus != ExamStudentStatus.DA_LAM
                && es.getId() != null) {
            examQuestionResultRepository.deleteByExamStudentId(es.getId());
        }
        es.setStatus(newStatus);
        examStudentRepository.save(es);
    }

    /** Tao truoc cac dong exam_student cho hoc vien dich (idempotent). */
    private void materializeExamStudents(Exam exam, CreateExamRequest req) {
        if (req.type() == ExamType.BY_CLASS) {
            if (CollectionUtils.isEmpty(req.classIds())) return;
            for (User u : classRepository.findStudentsByClassIds(req.classIds())) {
                upsertMissing(exam, u, ExamStudentSource.CLASS);
            }
        } else if (!CollectionUtils.isEmpty(req.studentIds())) {
            for (Long uid : req.studentIds()) {
                User u = userRepository.findById(uid)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                upsertMissing(exam, u, ExamStudentSource.SUPPLEMENTARY);
            }
        }
    }

    private void upsertMissing(Exam exam, User user, ExamStudentSource source) {
        if (examStudentRepository.existsByExamIdAndUserId(exam.getId(), user.getId())) {
            return;
        }
        ExamStudent es = new ExamStudent();
        es.setExam(exam);
        es.setUser(user);
        es.setSource(source);
        es.setStatus(ExamStudentStatus.CHUA_PHAT_HANH);
        examStudentRepository.save(es);
    }

    private StudentExamItem toStudentExamItem(ExamStudent es, Set<Long> myClassIds, Instant now) {
        Exam e = es.getExam();
        // overlay QUA_HAN: da phat hanh/dang lam ma qua endAt va chua nop
        boolean overdue = e.getEndAt() != null
                && now.isAfter(e.getEndAt())
                && es.getSubmittedAt() == null
                && (es.getStatus() == ExamStudentStatus.DA_PHAT_HANH
                    || es.getStatus() == ExamStudentStatus.DANG_KIEM_TRA);
        String status = overdue ? "QUA_HAN" : es.getStatus().name();

        boolean canView = "DA_PHAT_HANH".equals(status) || "DA_LAM".equals(status);
        boolean canTake = "DA_PHAT_HANH".equals(status) || "DANG_KIEM_TRA".equals(status);

        List<StudentExamItem.CourseRef> courses = e.getClasses().stream()
                .filter(c -> myClassIds.contains(c.getId()))
                .map(c -> new StudentExamItem.CourseRef(c.getId(), c.getName()))
                .toList();

        return new StudentExamItem(
                e.getId(), e.getCode(), e.getName(), courses,
                e.getPublishAt(), e.getEndAt(), e.getDurationMinutes(),
                status, canView, canTake);
    }

    // ---- helpers ----

    private void buildExam(Exam exam, CreateExamRequest req, Subject subject) {
        exam.setName(req.name());
        exam.setSubject(subject);
        exam.setTopic(resolveTopic(req.topicId(), subject));
        exam.setType(req.type());
        exam.setDurationMinutes(req.durationMinutes());
        exam.setPublishAt(req.publishAt());
        exam.setEndAt(req.endAt());
        exam.setStatus(req.status() != null ? req.status() : ExamStatus.ACTIVE);

        // rebuild exercises (orphanRemoval deletes old ones)
        exam.getExamExercises().clear();
        if (!CollectionUtils.isEmpty(req.exercises())) {
            List<Long> exerciseIds = req.exercises().stream()
                    .map(ExamExerciseRequest::exerciseId).toList();
            Map<Long, Exercise> exerciseMap = exerciseRepository.findAllById(exerciseIds).stream()
                    .collect(Collectors.toMap(Exercise::getId, Function.identity()));

            for (int i = 0; i < req.exercises().size(); i++) {
                ExamExerciseRequest er = req.exercises().get(i);
                Exercise exercise = exerciseMap.get(er.exerciseId());
                if (exercise == null) throw new AppException(ErrorCode.EXERCISE_NOT_FOUND);

                ExamExercise ee = new ExamExercise();
                ee.setExam(exam);
                ee.setExercise(exercise);
                ee.setSortOrder(er.sortOrder() != null ? er.sortOrder() : i);
                ee.setPoints(er.points());

                if (exercise.getType() == ExerciseType.TRUE_FALSE
                        && !CollectionUtils.isEmpty(er.itemScores())) {
                    Map<Long, TrueFalseItem> itemMap = exercise.getTrueFalseItems().stream()
                            .collect(Collectors.toMap(TrueFalseItem::getId, Function.identity()));
                    List<ExamTfItemScore> scores = new ArrayList<>();
                    for (var scoreReq : er.itemScores()) {
                        TrueFalseItem tfItem = itemMap.get(scoreReq.tfItemId());
                        if (tfItem == null) continue;
                        ExamTfItemScore score = new ExamTfItemScore();
                        score.setExamExercise(ee);
                        score.setTfItem(tfItem);
                        score.setPoints(scoreReq.points());
                        scores.add(score);
                    }
                    ee.setItemScores(scores);
                }
                exam.getExamExercises().add(ee);
            }
        }

        // rebuild classes
        Set<com.trungtam.schoolclass.entity.SchoolClass> classes = new HashSet<>();
        if (!CollectionUtils.isEmpty(req.classIds())) {
            for (Long classId : req.classIds()) {
                classes.add(classRepository.findById(classId)
                        .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND)));
            }
        }
        exam.setClasses(classes);

        // rebuild students (SUPPLEMENTARY only)
        Set<User> students = new HashSet<>();
        if (req.type() == ExamType.SUPPLEMENTARY && !CollectionUtils.isEmpty(req.studentIds())) {
            for (Long userId : req.studentIds()) {
                students.add(userRepository.findById(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
            }
        }
        exam.setStudents(students);
    }

    private void validate(CreateExamRequest req) {
        if (req.publishAt() != null && req.endAt() != null
                && !req.endAt().isAfter(req.publishAt())) {
            throw new AppException(ErrorCode.EXAM_INVALID_TIME_RANGE);
        }
        if (req.type() == ExamType.SUPPLEMENTARY
                && CollectionUtils.isEmpty(req.studentIds())) {
            throw new AppException(ErrorCode.EXAM_SUPPLEMENTARY_NO_STUDENT);
        }
    }

    private Exam findOrThrow(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    /** Lay chuyen de va dam bao thuoc dung mon hoc cua de thi. */
    private com.trungtam.topic.entity.Topic resolveTopic(Long topicId, Subject subject) {
        if (topicId == null) return null;
        com.trungtam.topic.entity.Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (!topic.getSubject().getId().equals(subject.getId())) {
            throw new AppException(ErrorCode.TOPIC_SUBJECT_MISMATCH);
        }
        return topic;
    }

    private Sort resolveSort(String field, String order) {
        String sortField = switch (StringUtils.hasText(field) ? field : "createdAt") {
            case "code" -> "code";
            case "name" -> "name";
            case "createdAt" -> "createdAt";
            default -> "createdAt";
        };
        Sort.Direction dir = "ascend".equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortField);
    }
}
