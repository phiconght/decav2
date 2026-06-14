package com.trungtam.schoolclass.service;

import com.trungtam.common.codegen.CodeGeneratorService;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.schoolclass.dto.request.ClassSearchParams;
import com.trungtam.schoolclass.dto.request.CreateClassRequest;
import com.trungtam.schoolclass.dto.request.UpdateClassStatusRequest;
import com.trungtam.schoolclass.dto.response.ClassDetailResponse;
import com.trungtam.schoolclass.dto.response.ClassListItem;
import com.trungtam.schoolclass.dto.response.ClassPageResponse;
import com.trungtam.schoolclass.entity.ClassStatus;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.ClassSpec;
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
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final CodeGeneratorService codeGeneratorService;

    public ClassPageResponse search(ClassSearchParams params) {
        Specification<SchoolClass> spec = ClassSpec.build(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ClassListItem> result = classRepository.findAll(spec, pageable)
                .map(ClassListItem::from);
        return ClassPageResponse.of(result);
    }

    public ClassDetailResponse getById(Long id) {
        return ClassDetailResponse.from(findOrThrow(id));
    }

    @Transactional
    public ClassDetailResponse create(CreateClassRequest req) {
        Subject subject = findSubjectOrThrow(req.subjectId());
        String code = codeGeneratorService.generateClassCode(subject.getName(), subject.getGradeLevel());
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setCode(code);
        schoolClass.setName(req.name());
        schoolClass.setSubject(subject);
        schoolClass.setStartDate(req.startDate());
        schoolClass.setEndDate(req.endDate());
        schoolClass.setStatus(req.status() != null ? req.status() : ClassStatus.ACTIVE);
        return ClassDetailResponse.from(classRepository.save(schoolClass));
    }

    @Transactional
    public ClassDetailResponse update(Long id, CreateClassRequest req) {
        SchoolClass schoolClass = findOrThrow(id);
        Subject subject = findSubjectOrThrow(req.subjectId());
        schoolClass.setName(req.name());
        schoolClass.setSubject(subject);
        schoolClass.setStartDate(req.startDate());
        schoolClass.setEndDate(req.endDate());
        if (req.status() != null) {
            schoolClass.setStatus(req.status());
        }
        return ClassDetailResponse.from(classRepository.save(schoolClass));
    }

    @Transactional
    public void updateStatus(Long id, UpdateClassStatusRequest req) {
        SchoolClass schoolClass = findOrThrow(id);
        schoolClass.setStatus(req.status());
        classRepository.save(schoolClass);
    }

    @Transactional
    public void delete(Long id) {
        classRepository.delete(findOrThrow(id));
    }

    private SchoolClass findOrThrow(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
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
