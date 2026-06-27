package com.trungtam.exercise.service;

import com.trungtam.common.codegen.CodeGeneratorService;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exercise.dto.request.ChoiceOptionRequest;
import com.trungtam.exercise.dto.request.CreateExerciseRequest;
import com.trungtam.exercise.dto.request.ExerciseSearchParams;
import com.trungtam.exercise.dto.request.UpdateStatusRequest;
import com.trungtam.exercise.dto.response.ExerciseDetailResponse;
import com.trungtam.exercise.dto.response.ExerciseListItem;
import com.trungtam.exercise.dto.response.ExercisePageResponse;
import com.trungtam.exercise.entity.ChoiceOption;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseStatus;
import com.trungtam.exercise.entity.ExerciseType;
import com.trungtam.exercise.entity.TrueFalseItem;
import com.trungtam.exercise.repository.ExerciseRepository;
import com.trungtam.exercise.repository.ExerciseSpec;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final SubjectRepository subjectRepository;
    private final com.trungtam.topic.repository.TopicRepository topicRepository;
    private final CodeGeneratorService codeGeneratorService;

    public ExercisePageResponse search(ExerciseSearchParams params) {
        Specification<Exercise> spec = ExerciseSpec.build(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1); // FE gui 1-based
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExerciseListItem> result = exerciseRepository.findAll(spec, pageable)
                .map(ExerciseListItem::from);
        return ExercisePageResponse.of(result);
    }

    public ExerciseDetailResponse getById(Long id) {
        return ExerciseDetailResponse.from(findOrThrow(id));
    }

    @Transactional
    public ExerciseDetailResponse create(CreateExerciseRequest req) {
        Subject subject = findSubjectOrThrow(req.subjectId());
        char typeChar = switch (req.type()) {
            case MULTIPLE_CHOICE -> 'N';
            case ESSAY           -> 'L';
            case TRUE_FALSE      -> 'D';
        };
        String code = codeGeneratorService.generateExerciseCode(subject.getName(), subject.getGradeLevel(), typeChar);
        Exercise exercise = buildExercise(new Exercise(), req, subject, code);
        validateMultipleChoice(req);
        return ExerciseDetailResponse.from(exerciseRepository.save(exercise));
    }

    @Transactional
    public ExerciseDetailResponse update(Long id, CreateExerciseRequest req) {
        Exercise exercise = findOrThrow(id);
        Subject subject = findSubjectOrThrow(req.subjectId());
        // Xoa dap an cu truoc khi ghi moi (orphanRemoval xu ly DELETE)
        exercise.getOptions().clear();
        exercise.getTrueFalseItems().clear();
        // Giu nguyen code da sinh tu luc tao, chi cap nhat noi dung
        buildExercise(exercise, req, subject, exercise.getCode());
        validateMultipleChoice(req);
        return ExerciseDetailResponse.from(exerciseRepository.save(exercise));
    }

    @Transactional
    public void updateStatus(Long id, UpdateStatusRequest req) {
        Exercise exercise = findOrThrow(id);
        exercise.setStatus(req.status());
        exerciseRepository.save(exercise);
    }

    @Transactional
    public void delete(Long id) {
        exerciseRepository.delete(findOrThrow(id));
    }

    // ------------------------------------------------------------------

    private Exercise findOrThrow(Long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EXERCISE_NOT_FOUND));
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    /** Lay chuyen de va dam bao thuoc dung mon hoc cua bai tap. */
    private com.trungtam.topic.entity.Topic resolveTopic(Long topicId, Subject subject) {
        if (topicId == null) return null;
        com.trungtam.topic.entity.Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (!topic.getSubject().getId().equals(subject.getId())) {
            throw new AppException(ErrorCode.TOPIC_SUBJECT_MISMATCH);
        }
        return topic;
    }

    private Exercise buildExercise(Exercise exercise, CreateExerciseRequest req, Subject subject, String code) {
        exercise.setCode(code);
        exercise.setTitle(req.title());
        exercise.setSubjectEntity(subject);
        exercise.setTopic(resolveTopic(req.topicId(), subject));
        exercise.setType(req.type());
        exercise.setDifficulty(req.difficulty());
        exercise.setStatus(req.status() != null ? req.status() : ExerciseStatus.ACTIVE);
        exercise.setQuestionText(req.questionText());
        exercise.setQuestionImage(req.questionImage());

        // Xoa cac truong khong thuoc loai hien tai
        exercise.setEssayAnswer(null);
        exercise.setEssayAnswerImage(null);

        if (req.type() == ExerciseType.ESSAY) {
            exercise.setEssayAnswer(req.essayAnswer());
            exercise.setEssayAnswerImage(req.essayAnswerImage());
        } else if (req.type() == ExerciseType.MULTIPLE_CHOICE && req.options() != null) {
            exercise.getOptions().addAll(buildOptions(req.options(), exercise));
        } else if (req.type() == ExerciseType.TRUE_FALSE && req.trueFalseItems() != null) {
            exercise.getTrueFalseItems().addAll(buildTrueFalseItems(req.trueFalseItems(), exercise));
        }

        return exercise;
    }

    private List<ChoiceOption> buildOptions(List<ChoiceOptionRequest> requests, Exercise exercise) {
        List<ChoiceOption> result = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            ChoiceOptionRequest r = requests.get(i);
            ChoiceOption opt = new ChoiceOption();
            opt.setExercise(exercise);
            opt.setSortOrder(i);
            opt.setText(r.text());
            opt.setImage(r.image());
            opt.setCorrect(r.isCorrect());
            result.add(opt);
        }
        return result;
    }

    private List<TrueFalseItem> buildTrueFalseItems(
            List<com.trungtam.exercise.dto.request.TrueFalseItemRequest> requests,
            Exercise exercise) {
        List<TrueFalseItem> result = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            var r = requests.get(i);
            TrueFalseItem item = new TrueFalseItem();
            item.setExercise(exercise);
            item.setSortOrder(i);
            item.setText(r.text());
            item.setImage(r.image());
            item.setAnswer(r.answer());
            result.add(item);
        }
        return result;
    }

    private void validateMultipleChoice(CreateExerciseRequest req) {
        if (req.type() != ExerciseType.MULTIPLE_CHOICE || req.options() == null) return;
        long correctCount = req.options().stream().filter(ChoiceOptionRequest::isCorrect).count();
        if (correctCount == 0) throw new AppException(ErrorCode.NO_CORRECT_OPTION);
        if (correctCount > 1) throw new AppException(ErrorCode.MULTIPLE_CORRECT_OPTION);
    }

    private Sort resolveSort(String field, String order) {
        String sortField = switch (StringUtils.hasText(field) ? field : "createdAt") {
            case "code" -> "code";
            case "title" -> "title";
            case "createdAt" -> "createdAt";
            default -> "createdAt";
        };
        Sort.Direction dir = "ascend".equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortField);
    }
}
