package com.trungtam.exam.service;

import com.trungtam.common.codegen.CodeGeneratorService;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.dto.request.CreateExamRequest;
import com.trungtam.exam.dto.request.ExamExerciseRequest;
import com.trungtam.exam.dto.request.ExamSearchParams;
import com.trungtam.exam.dto.request.UpdateExamStatusRequest;
import com.trungtam.exam.dto.response.ExamDetailResponse;
import com.trungtam.exam.dto.response.ExamListItem;
import com.trungtam.exam.dto.response.ExamPageResponse;
import com.trungtam.exam.dto.response.StudentOptionResponse;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamExercise;
import com.trungtam.exam.entity.ExamStatus;
import com.trungtam.exam.entity.ExamTfItemScore;
import com.trungtam.exam.entity.ExamType;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamSpec;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseType;
import com.trungtam.exercise.entity.TrueFalseItem;
import com.trungtam.exercise.repository.ExerciseRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.subject.entity.Subject;
import com.trungtam.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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
    private final CodeGeneratorService codeGeneratorService;

    public ExamPageResponse search(ExamSearchParams params) {
        Specification<Exam> spec = ExamSpec.build(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExamListItem> result = examRepository.findAll(spec, pageable).map(ExamListItem::from);
        return ExamPageResponse.of(result);
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
        return ExamDetailResponse.from(examRepository.save(exam));
    }

    @Transactional
    public ExamDetailResponse update(Long id, CreateExamRequest req) {
        Exam exam = findOrThrow(id);
        Subject subject = findSubjectOrThrow(req.subjectId());
        buildExam(exam, req, subject);
        validate(req);
        return ExamDetailResponse.from(examRepository.save(exam));
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

    public List<StudentOptionResponse> listStudentOptions(List<Long> classIds) {
        if (CollectionUtils.isEmpty(classIds)) {
            return List.of();
        }
        return classRepository.findStudentsByClassIds(classIds).stream()
                .map(StudentOptionResponse::from)
                .toList();
    }

    // ---- helpers ----

    private void buildExam(Exam exam, CreateExamRequest req, Subject subject) {
        exam.setName(req.name());
        exam.setSubject(subject);
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
