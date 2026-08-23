package com.trungtam.exercise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.dto.request.CreateExamRequest;
import com.trungtam.exam.dto.request.ExamExerciseRequest;
import com.trungtam.exam.entity.ExamType;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.service.ExamService;
import com.trungtam.exercise.dto.request.ChoiceOptionRequest;
import com.trungtam.exercise.dto.request.CreateExerciseRequest;
import com.trungtam.exercise.dto.request.ImportBatchFileDto;
import com.trungtam.exercise.dto.request.ImportItemDto;
import com.trungtam.exercise.dto.request.ImportOptionDto;
import com.trungtam.exercise.dto.request.ImportTrueFalseItemDto;
import com.trungtam.exercise.dto.request.TrueFalseItemRequest;
import com.trungtam.exercise.dto.response.ExerciseDetailResponse;
import com.trungtam.exercise.dto.response.ImportBatchDetailResponse;
import com.trungtam.exercise.dto.response.ImportBatchListItem;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseDifficulty;
import com.trungtam.exercise.entity.ExerciseStatus;
import com.trungtam.exercise.entity.ExerciseType;
import com.trungtam.exercise.entity.ImportBatch;
import com.trungtam.exercise.entity.ImportBatchStatus;
import com.trungtam.exercise.repository.ExerciseRepository;
import com.trungtam.exercise.repository.ImportBatchRepository;
import com.trungtam.security.SecurityUtils;
import com.trungtam.subject.entity.Subject;
import com.trungtam.subject.repository.SubjectRepository;
import com.trungtam.topic.entity.Topic;
import com.trungtam.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Nhap bai tap/de thi tu file du lieu .json (Word -> AI -> file du lieu, ngoai
 * he thong) — xem SPEC_NhapBaiTap_TuWord_QuaAI.md §6. Mo hinh STATEFUL: tao
 * ban ghi Exercise/Exam that ngay o trang thai PENDING (khac kenh Excel du
 * kien o SPEC_CongThucToan_NhapHangLoat.md, van STATELESS/dry-run — 2 kenh
 * doc lap, xem §0.2).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseImportBatchService {

    /** Dong bo gioi han da chot cho kenh Excel (SPEC_CongThucToan_NhapHangLoat.md §10.10-1). */
    private static final int MAX_ITEMS = 500;
    private static final int MAX_TRUE_FALSE_ITEMS = 6;
    private static final int MULTIPLE_CHOICE_OPTION_COUNT = 4;
    /** Khop dung rule client "Tên tối đa 30 ký tự" cua CreateExerciseForm.tsx —
     *  neu cat dai hon, mo "Sua" 1 bai vua nhap roi luu se bao loi validate
     *  ngay ca khi khong dung vao tieu de. */
    private static final int TITLE_MAX_LENGTH = 30;

    private final ImportBatchRepository importBatchRepository;
    private final ExerciseRepository exerciseRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ExerciseService exerciseService;
    private final ExamService examService;
    private final ExamRepository examRepository;
    private final ObjectMapper objectMapper;

    // ============================ TAO LO (nhap) ============================

    @Transactional
    public ImportBatchDetailResponse importBatch(MultipartFile data, Long subjectId, Long topicId,
                                                  String examName) {
        ImportBatchFileDto file = parseFile(data);
        validateItems(file.items());

        Subject subject = findSubjectOrThrow(subjectId);
        Topic topic = resolveTopic(topicId, subject);

        ImportBatch batch = new ImportBatch();
        batch.setSourceFileName(StringUtils.hasText(file.sourceFileName())
                ? file.sourceFileName() : data.getOriginalFilename());
        batch.setSubject(subject);
        batch.setTopic(topic);
        batch.setExamName(examName);
        batch.setTotalCount(file.items().size());
        batch.setStatus(ImportBatchStatus.IN_PROGRESS);
        batch = importBatchRepository.save(batch);

        List<Exercise> created = new ArrayList<>();
        for (int i = 0; i < file.items().size(); i++) {
            CreateExerciseRequest req = toCreateExerciseRequest(file.items().get(i), subjectId, topicId);
            ExerciseDetailResponse resp = exerciseService.create(req);
            Exercise exercise = exerciseRepository.getReferenceById(resp.id());
            exercise.setImportBatch(batch);
            exercise.setOrderIndex(i);
            created.add(exerciseRepository.save(exercise));
        }

        if (StringUtils.hasText(examName)) {
            createLinkedExam(batch, subject, topic, examName, created);
        }

        return toDetail(batch, created);
    }

    /** Tao 1 de thi PENDING, noi toan bo bai vua tao theo dung thu tu (§6.3). */
    private void createLinkedExam(ImportBatch batch, Subject subject, Topic topic,
                                  String examName, List<Exercise> exercises) {
        BigDecimal pointsEach = BigDecimal.TEN
                .divide(BigDecimal.valueOf(exercises.size()), 2, RoundingMode.HALF_UP);
        List<ExamExerciseRequest> examExercises = new ArrayList<>();
        for (int i = 0; i < exercises.size(); i++) {
            examExercises.add(new ExamExerciseRequest(exercises.get(i).getId(), i, pointsEach, null));
        }
        CreateExamRequest examReq = new CreateExamRequest(
                examName, subject.getId(), topic != null ? topic.getId() : null,
                null, ExamType.BY_CLASS, null, null, null,
                com.trungtam.exam.entity.ExamStatus.PENDING,
                examExercises, null, null);
        var examResp = examService.create(examReq);
        batch.setExam(examRepository.getReferenceById(examResp.id()));
        importBatchRepository.save(batch);
    }

    // ============================ QUERY ============================

    public List<ImportBatchListItem> myBatches() {
        String me = SecurityUtils.requireCurrentUsername();
        return importBatchRepository.findByCreatedByOrderByIdDesc(me).stream()
                .map(ImportBatchListItem::from)
                .toList();
    }

    public long myInProgressCount() {
        String me = SecurityUtils.requireCurrentUsername();
        return importBatchRepository.countByCreatedByAndStatus(me, ImportBatchStatus.IN_PROGRESS);
    }

    public ImportBatchDetailResponse getById(Long id) {
        ImportBatch batch = findBatchOrThrow(id);
        List<Exercise> exercises = exerciseRepository.findByImportBatchIdOrderByOrderIndexAsc(id);
        return toDetail(batch, exercises);
    }

    // ============================ HELPERS ============================

    private ImportBatchDetailResponse toDetail(ImportBatch batch, List<Exercise> exercises) {
        int confirmed = (int) exercises.stream()
                .filter(e -> e.getStatus() == ExerciseStatus.ACTIVE).count();
        List<ExerciseDetailResponse> items = exercises.stream()
                .map(ExerciseDetailResponse::from)
                .toList();
        return ImportBatchDetailResponse.from(batch, confirmed, items);
    }

    private ImportBatchFileDto parseFile(MultipartFile data) {
        if (data == null || data.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_DATA_FILE_INVALID, "Chua chon file du lieu");
        }
        try {
            ImportBatchFileDto file = objectMapper.readValue(data.getInputStream(), ImportBatchFileDto.class);
            if (file.items() == null || file.items().isEmpty()) {
                throw new AppException(ErrorCode.IMPORT_DATA_FILE_INVALID, "File khong co cau hoi nao");
            }
            if (file.items().size() > MAX_ITEMS) {
                throw new AppException(ErrorCode.IMPORT_DATA_FILE_INVALID,
                        "Vuot qua " + MAX_ITEMS + " cau moi lan nhap");
            }
            return file;
        } catch (AppException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.IMPORT_DATA_FILE_INVALID,
                    "File du lieu khong dung dinh dang JSON: " + e.getOriginalMessage());
        } catch (Exception e) {
            throw new AppException(ErrorCode.IMPORT_DATA_FILE_INVALID, "Khong doc duoc file du lieu");
        }
    }

    /** Validate toan bo file truoc khi tao gi — 1 cau loi thi chan ca file (§5.3). */
    private void validateItems(List<ImportItemDto> items) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            int rowNumber = i + 1;
            ImportItemDto item = items.get(i);
            if (item.type() == null) {
                errors.add("Câu " + rowNumber + ": thiếu loại câu hỏi");
                continue;
            }
            if (!StringUtils.hasText(item.questionText())) {
                errors.add("Câu " + rowNumber + ": thiếu đề bài");
            }
            checkLatexBalanced(item.questionText(), rowNumber, "đề bài", errors);
            if (item.type() == ExerciseType.ESSAY) {
                checkLatexBalanced(item.essayAnswer(), rowNumber, "đáp án", errors);
            }
            switch (item.type()) {
                case MULTIPLE_CHOICE -> validateMultipleChoice(item, rowNumber, errors);
                case TRUE_FALSE -> validateTrueFalse(item, rowNumber, errors);
                case ESSAY -> { /* essayAnswer khong bat buoc, da kiem LaTeX o tren */ }
            }
        }
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_ROW_INVALID, String.join("; ", errors));
        }
    }

    private void validateMultipleChoice(ImportItemDto item, int rowNumber, List<String> errors) {
        if (item.options() == null || item.options().size() != MULTIPLE_CHOICE_OPTION_COUNT) {
            errors.add("Câu " + rowNumber + ": trắc nghiệm cần đúng 4 phương án");
            return;
        }
        long correctCount = item.options().stream()
                .filter(o -> Boolean.TRUE.equals(o.correct())).count();
        if (correctCount != 1) {
            errors.add("Câu " + rowNumber + ": cần đúng 1 đáp án đúng, hiện có " + correctCount);
        }
        for (int i = 0; i < item.options().size(); i++) {
            ImportOptionDto o = item.options().get(i);
            if (!StringUtils.hasText(o.text())) {
                errors.add("Câu " + rowNumber + ": có phương án thiếu nội dung");
                break;
            }
            checkLatexBalanced(o.text(), rowNumber, "phương án " + (char) ('A' + i), errors);
        }
    }

    private void validateTrueFalse(ImportItemDto item, int rowNumber, List<String> errors) {
        if (item.trueFalseItems() == null || item.trueFalseItems().isEmpty()
                || item.trueFalseItems().size() > MAX_TRUE_FALSE_ITEMS) {
            errors.add("Câu " + rowNumber + ": đúng/sai cần 1 đến " + MAX_TRUE_FALSE_ITEMS + " ý");
            return;
        }
        for (int i = 0; i < item.trueFalseItems().size(); i++) {
            ImportTrueFalseItemDto t = item.trueFalseItems().get(i);
            if (!StringUtils.hasText(t.text()) || t.answer() == null) {
                errors.add("Câu " + rowNumber + ": có ý thiếu nội dung hoặc thiếu Đúng/Sai");
                break;
            }
            checkLatexBalanced(t.text(), rowNumber, "ý " + (char) ('a' + i), errors);
        }
    }

    /**
     * Kiem tra so dau {@code $} khong escape (bo qua {@code \$}) la SO CHAN —
     * le nghia la co it nhat 1 khoi {@code $...$} chua dong, se render RA
     * TEXT THO/mat chu tren Admin/Mobile/PDF thay vi cong thuc (da gap thuc
     * te: AI boc tach tu anh/Word quen 1 dau $ dong cuoi bieu thuc). Chan tu
     * luc nhap thay vi de loi am tham lot qua roi phai sua tay tung ban ghi.
     */
    private void checkLatexBalanced(String text, int rowNumber, String fieldLabel, List<String> errors) {
        if (!StringUtils.hasText(text)) return;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '$' && (i == 0 || text.charAt(i - 1) != '\\')) {
                count++;
            }
        }
        if (count % 2 != 0) {
            errors.add("Câu " + rowNumber + ": " + fieldLabel
                    + " thiếu 1 dấu $ đóng công thức LaTeX (số dấu $ không phải số chẵn)");
        }
    }

    private CreateExerciseRequest toCreateExerciseRequest(ImportItemDto item, Long subjectId, Long topicId) {
        List<ChoiceOptionRequest> options = item.options() == null ? null : item.options().stream()
                .map(o -> new ChoiceOptionRequest(o.text(), null, Boolean.TRUE.equals(o.correct())))
                .toList();
        List<TrueFalseItemRequest> trueFalseItems = item.trueFalseItems() == null ? null
                : item.trueFalseItems().stream()
                        .map(t -> new TrueFalseItemRequest(t.text(), null, Boolean.TRUE.equals(t.answer())))
                        .toList();
        return new CreateExerciseRequest(
                resolveTitle(item),
                subjectId,
                topicId,
                item.type(),
                item.difficulty() != null ? item.difficulty() : ExerciseDifficulty.MEDIUM,
                ExerciseStatus.PENDING,
                item.questionText(),
                null,
                item.essayAnswer(),
                null,
                options,
                trueFalseItems
        );
    }

    /** title trong file du lieu de trong -> cat {@value #TITLE_MAX_LENGTH} ky tu dau questionText. */
    private String resolveTitle(ImportItemDto item) {
        if (StringUtils.hasText(item.title())) {
            return item.title();
        }
        String q = item.questionText();
        if (!StringUtils.hasText(q)) {
            return null;
        }
        return q.length() > TITLE_MAX_LENGTH ? q.substring(0, TITLE_MAX_LENGTH) : q;
    }

    private ImportBatch findBatchOrThrow(Long id) {
        return importBatchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.IMPORT_BATCH_NOT_FOUND));
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private Topic resolveTopic(Long topicId, Subject subject) {
        if (topicId == null) return null;
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (!topic.getSubject().getId().equals(subject.getId())) {
            throw new AppException(ErrorCode.TOPIC_SUBJECT_MISMATCH);
        }
        return topic;
    }
}
