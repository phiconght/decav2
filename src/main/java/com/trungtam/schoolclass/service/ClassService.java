package com.trungtam.schoolclass.service;

import com.trungtam.coin.dto.response.CoinBalanceResponse;
import com.trungtam.coin.service.CoinService;
import com.trungtam.common.codegen.CodeGeneratorService;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.entity.ExamStudentSource;
import com.trungtam.exam.entity.ExamStudentStatus;
import com.trungtam.exam.entity.ExamType;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
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
import com.trungtam.schoolclass.entity.PaymentType;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.ClassSpec;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamStudentRepository examStudentRepository;
    private final CoinService coinService;

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
        if (req.pricePerSession() != null) {
            schoolClass.setPricePerSession(req.pricePerSession());
        }
        schoolClass.setCoinPrice(req.coinPrice());
        if (req.paymentType() != null) {
            schoolClass.setPaymentType(req.paymentType());
        }
        if (req.deliveryMode() != null) {
            schoolClass.setDeliveryMode(req.deliveryMode());
        }
        schoolClass.getTeachers().addAll(resolveTeachers(req.teacherIds()));
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
        if (req.pricePerSession() != null) {
            schoolClass.setPricePerSession(req.pricePerSession());
        }
        schoolClass.setCoinPrice(req.coinPrice());
        if (req.paymentType() != null) {
            schoolClass.setPaymentType(req.paymentType());
        }
        if (req.deliveryMode() != null) {
            schoolClass.setDeliveryMode(req.deliveryMode());
        }
        // Ghi đè giáo viên: xóa + flush NGAY rồi thêm mới
        // (tránh đụng PK class_teachers khi Hibernate insert trước delete)
        schoolClass.getTeachers().clear();
        classRepository.flush();
        schoolClass.getTeachers().addAll(resolveTeachers(req.teacherIds()));
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

    /** Dropdown giao vien (gate CLASS:READ — phuc vu xep lich, khong can USER:READ). */
    public List<StudentOptionResponse> teacherOptions(String keyword) {
        return userRepository.findByRoleAndKeyword(RoleName.TEACHER, keyword).stream()
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
        // Ghi ngay ghi danh cho cac cap moi (class_students la @ManyToMany -> native update, phuong an A)
        classRepository.markEnrolledAt(classId, req.studentIds());
        // Hoc vien vao khoa sau khi de da phat hanh: tao truoc dong exam_student
        materializeNewMembersExams(classId, req.studentIds());
    }

    /** Tao truoc exam_student cho hoc vien moi vao khoa, voi cac de BY_CLASS cua khoa. */
    private void materializeNewMembersExams(Long classId, List<Long> userIds) {
        for (Exam exam : examRepository.findByClassId(classId)) {
            if (exam.getType() != ExamType.BY_CLASS) continue;
            for (Long userId : userIds) {
                if (examStudentRepository.existsByExamIdAndUserId(exam.getId(), userId)) {
                    continue;
                }
                ExamStudent es = new ExamStudent();
                es.setExam(exam);
                es.setUser(userRepository.getReferenceById(userId));
                es.setSource(ExamStudentSource.CLASS);
                es.setStatus(ExamStudentStatus.CHUA_PHAT_HANH);
                examStudentRepository.save(es);
            }
        }
    }

    @Transactional
    public void removeStudent(Long classId, Long userId) {
        SchoolClass schoolClass = findOrThrow(classId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        schoolClass.getStudents().remove(user);
        classRepository.save(schoolClass);
    }

    /** Danh sach lop ma 1 hoc vien (user) dang tham gia. */
    public List<ClassListItem> listClassesByStudent(Long userId) {
        return classRepository.findClassesByStudentId(userId).stream()
                .map(c -> ClassListItem.from(
                        c,
                        classRepository.countStudents(c.getId()),
                        examRepository.countByClassId(c.getId())))
                .toList();
    }

    /** Lay thong tin gon cua khoa theo list id (cho dropdown map nhan). */
    public List<com.trungtam.schoolclass.dto.response.ClassRefItem> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return classRepository.findAllById(ids).stream()
                .map(com.trungtam.schoolclass.dto.response.ClassRefItem::from)
                .toList();
    }

    /** Danh sach khoa hoc ma 1 giao vien phu trach. */
    public List<ClassListItem> listClassesByTeacher(Long userId) {
        return classRepository.findClassesByTeacherId(userId).stream()
                .map(c -> ClassListItem.from(
                        c,
                        classRepository.countStudents(c.getId()),
                        examRepository.countByClassId(c.getId())))
                .toList();
    }

    /** Nap danh sach giao vien tu danh sach id (bo qua neu null). */
    private Set<User> resolveTeachers(List<Long> teacherIds) {
        Set<User> result = new HashSet<>();
        if (teacherIds == null) return result;
        for (Long uid : teacherIds) {
            User user = userRepository.findById(uid)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            result.add(user);
        }
        return result;
    }

    /**
     * Danh muc TOAN HE THONG (moi lop, ke ca lop chua ghi danh) — nguon cho
     * man "Khám phá khóa học" (Mobile + Web) va khoi marketing Trang chu. Moi
     * vai tro da dang nhap deu xem duoc toan bo danh sach (khong gate
     * CLASS:READ — xem Controller), khong doi hanh vi cu. Khach CHUA dang
     * nhap (Web cong khai) chi thay lop dang ACTIVE — an lop nhap/tam dong
     * (KEHOACH_WEB_TrangChuCongKhai_HeroContent.md muc 4.7).
     */
    public List<com.trungtam.schoolclass.dto.response.ClassCatalogItem> listCatalog() {
        boolean anonymous = SecurityUtils.getCurrentUsername().isEmpty();
        Long selfStudentId = currentStudentIdOrNull();
        return classRepository.findAll().stream()
                .filter(c -> !anonymous || c.getStatus() == ClassStatus.ACTIVE)
                .sorted(java.util.Comparator
                        .comparing((SchoolClass c) -> c.getSubject().getGradeLevel())
                        .thenComparing(SchoolClass::getName))
                .map(c -> com.trungtam.schoolclass.dto.response.ClassCatalogItem.from(
                        c,
                        selfStudentId != null
                                && classRepository.existsByIdAndStudents_Id(c.getId(), selfStudentId)))
                .toList();
    }

    /**
     * HOC SINH TU dang ky tham gia 1 lop bang Xu (Mobile/Web) — tru Xu +
     * them vao lop NGAY, khong can duyet (yeu cau nguoi dung). Tru Xu qua
     * {@link CoinService#adjust} (khoa bi quan, tu nem COIN_BALANCE_INSUFFICIENT
     * neu khong du) TRUOC khi ghi danh — khong bao gio ghi danh ma khong tru
     * duoc tien, va khong bao gio tru tien ma khong ghi danh duoc (cung 1
     * transaction Spring, rollback ca 2 neu buoc sau loi).
     */
    @Transactional
    public com.trungtam.schoolclass.dto.response.EnrollResponse enrollSelf(Long classId) {
        User me = currentUser();
        if (!hasRole(me, RoleName.STUDENT)) {
            throw new AppException(ErrorCode.NOT_A_STUDENT);
        }
        SchoolClass schoolClass = findOrThrow(classId);
        Long coinPrice = schoolClass.getCoinPrice();
        if (schoolClass.getPaymentType() != PaymentType.PREPAID_COIN
                || coinPrice == null || coinPrice <= 0) {
            throw new AppException(ErrorCode.CLASS_NOT_PURCHASABLE);
        }
        if (schoolClass.getStatus() != ClassStatus.ACTIVE) {
            throw new AppException(ErrorCode.CLASS_NOT_ACTIVE);
        }
        if (schoolClass.getStudents().contains(me)) {
            throw new AppException(ErrorCode.ALREADY_ENROLLED);
        }

        CoinBalanceResponse balance = coinService.adjust(
                me.getId(), -coinPrice, "Đăng ký khóa \"" + schoolClass.getName() + "\"");

        schoolClass.getStudents().add(me);
        classRepository.save(schoolClass);
        classRepository.markEnrolledAt(classId, List.of(me.getId()));
        materializeNewMembersExams(classId, List.of(me.getId()));

        return new com.trungtam.schoolclass.dto.response.EnrollResponse(
                schoolClass.getId(), schoolClass.getName(), coinPrice, balance.balance());
    }

    private Long currentStudentIdOrNull() {
        User me;
        try {
            me = currentUser();
        } catch (AppException e) {
            return null;
        }
        return hasRole(me, RoleName.STUDENT) ? me.getId() : null;
    }

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean hasRole(User u, RoleName role) {
        return u.getRoles().stream().anyMatch(r -> r.getName() == role);
    }

    /** Danh sach lop ma hoc vien DANG DANG NHAP tham gia (self-scoped). */
    public List<ClassListItem> listMyClasses() {
        return listClassesByStudent(currentUser().getId());
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
