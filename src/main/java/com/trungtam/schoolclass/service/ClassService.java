package com.trungtam.schoolclass.service;

import com.trungtam.common.codegen.CodeGeneratorService;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.schoolclass.dto.request.AddStudentsRequest;
import com.trungtam.schoolclass.dto.request.ClassSearchParams;
import com.trungtam.schoolclass.dto.request.CreateClassRequest;
import com.trungtam.schoolclass.dto.request.UpdateClassStatusRequest;
import com.trungtam.schoolclass.dto.response.ClassDetailResponse;
import com.trungtam.schoolclass.dto.response.ClassListItem;
import com.trungtam.schoolclass.dto.response.ClassPageResponse;
import com.trungtam.schoolclass.dto.response.StudentOptionResponse;
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

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;

    public ClassPageResponse search(ClassSearchParams params) {
        Specification<SchoolClass> spec = ClassSpec.build(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ClassListItem> result = classRepository.findAll(spec, pageable)
                .map(c -> ClassListItem.from(
                        c,
                        classRepository.countStudents(c.getId()),
                        examRepository.countByClassId(c.getId())));
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

    // ---- Ghi danh ----

    public List<StudentOptionResponse> listStudents(Long classId) {
        findOrThrow(classId);
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        return schoolClass.getStudents().stream()
                .sorted((a, b) -> {
                    String nameA = a.getFullName() != null ? a.getFullName() : "";
                    String nameB = b.getFullName() != null ? b.getFullName() : "";
                    return nameA.compareTo(nameB);
                })
                .map(StudentOptionResponse::from)
                .toList();
    }

    public List<StudentOptionResponse> listEligibleStudents(Long classId, String keyword) {
        findOrThrow(classId);
        return userRepository.findByRoleAndKeyword(RoleName.STUDENT, keyword).stream()
                .map(StudentOptionResponse::from)
                .toList();
    }

    @Transactional
    public void addStudents(Long classId, AddStudentsRequest req) {
        SchoolClass schoolClass = findOrThrow(classId);
        for (Long userId : req.studentIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            boolean isStudent = user.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.STUDENT);
            if (!isStudent) {
                throw new AppException(ErrorCode.NOT_A_STUDENT);
            }
            schoolClass.getStudents().add(user);
        }
        classRepository.save(schoolClass);
    }

    @Transactional
    public void removeStudent(Long classId, Long userId) {
        SchoolClass schoolClass = findOrThrow(classId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        schoolClass.getStudents().remove(user);
        classRepository.save(schoolClass);
    }

    public List<StudentOptionResponse> listStudentsByClassIds(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        return classRepository.findStudentsByClassIds(classIds).stream()
                .map(StudentOptionResponse::from)
                .toList();
    }

    // ---- helpers ----

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
