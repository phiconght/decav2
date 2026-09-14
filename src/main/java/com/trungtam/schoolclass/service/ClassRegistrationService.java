package com.trungtam.schoolclass.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.payment.entity.PaymentSettings;
import com.trungtam.payment.repository.PaymentSettingsRepository;
import com.trungtam.payment.service.VietQrService;
import com.trungtam.schoolclass.dto.request.AddStudentsRequest;
import com.trungtam.schoolclass.dto.response.EnrollmentRequestItem;
import com.trungtam.schoolclass.dto.response.RegistrationResponse;
import com.trungtam.schoolclass.entity.ClassEnrollmentRequest;
import com.trungtam.schoolclass.entity.ClassStatus;
import com.trungtam.schoolclass.entity.EnrollmentRequestStatus;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.ClassEnrollmentRequestRepository;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * Dang ky khoa hoc bang chuyen khoan thu cong (khac han luong Xu tu dong
 * cua {@link ClassService#enrollSelf}) — hoc vien bam Dang ky -> tao 1 yeu
 * cau cho xu ly + nhan QR; Admin doi chieu sao ke roi bam Xac nhan &amp; Ghi
 * danh. Xem docs/DecaMath/.scratch/course-page-design-plan.html Phan 3.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClassRegistrationService {

    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // bo 0/O/1/I de tranh nham
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassEnrollmentRequestRepository requestRepository;
    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final PaymentSettingsRepository paymentSettingsRepository;
    private final VietQrService vietQrService;
    private final ClassService classService;

    /**
     * Hoc vien bam "Dang ky" — tao yeu cau moi, hoac tra lai yeu cau PENDING
     * da co (bam lai khong tao trung, dung UNIQUE INDEX lam luoi an toan cuoi).
     */
    @Transactional
    public RegistrationResponse register(Long classId) {
        User me = currentUser();
        if (!hasRole(me, RoleName.STUDENT)) {
            throw new AppException(ErrorCode.NOT_A_STUDENT);
        }
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        if (schoolClass.getStatus() != ClassStatus.ACTIVE) {
            throw new AppException(ErrorCode.CLASS_NOT_ACTIVE);
        }
        if (schoolClass.getFullPrice() == null || schoolClass.getFullPrice().signum() <= 0) {
            throw new AppException(ErrorCode.CLASS_NOT_REGISTRABLE);
        }
        if (schoolClass.getStudents().contains(me)) {
            throw new AppException(ErrorCode.ALREADY_ENROLLED);
        }

        ClassEnrollmentRequest request = requestRepository
                .findBySchoolClass_IdAndStudent_IdAndStatus(classId, me.getId(), EnrollmentRequestStatus.PENDING)
                .orElseGet(() -> createRequest(schoolClass, me));

        return buildResponse(request);
    }

    /** HV quay lai trang chi tiet sau khi da bam Dang ky — hien lai dung QR/ma cu. */
    public Optional<RegistrationResponse> myPendingRegistration(Long classId) {
        User me = currentUser();
        return requestRepository
                .findBySchoolClass_IdAndStudent_IdAndStatus(classId, me.getId(), EnrollmentRequestStatus.PENDING)
                .map(this::buildResponse);
    }

    /** Man Admin "Yêu cầu đăng ký khóa học" — loc theo trang thai (null = tat ca). */
    public List<EnrollmentRequestItem> adminList(EnrollmentRequestStatus status) {
        List<ClassEnrollmentRequest> requests = status != null
                ? requestRepository.findByStatusOrderByCreatedAtDesc(status)
                : requestRepository.findAllByOrderByCreatedAtDesc();
        return requests.stream().map(EnrollmentRequestItem::from).toList();
    }

    /**
     * Admin da doi chieu sao ke thay chuyen khoan dung ma -> xac nhan VA ghi
     * danh hoc vien trong CUNG 1 giao dich (tranh xac nhan roi quen ghi danh).
     * Tai dung nguyen {@link ClassService#addStudents} dang dung cho man
     * "Thêm học viên" — khong viet lai luong ghi danh.
     */
    @Transactional
    public void confirm(Long requestId) {
        ClassEnrollmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND));
        if (request.getStatus() != EnrollmentRequestStatus.PENDING) {
            throw new AppException(ErrorCode.ENROLLMENT_REQUEST_ALREADY_PROCESSED);
        }
        classService.addStudents(
                request.getSchoolClass().getId(),
                new AddStudentsRequest(List.of(request.getStudent().getId())));
        request.setStatus(EnrollmentRequestStatus.CONFIRMED);
        request.setConfirmedAt(java.time.Instant.now());
        requestRepository.save(request);
    }

    /** Admin khong thay khoan chuyen khoan tuong ung -> dong yeu cau, khong ghi danh. */
    @Transactional
    public void cancel(Long requestId) {
        ClassEnrollmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND));
        if (request.getStatus() != EnrollmentRequestStatus.PENDING) {
            throw new AppException(ErrorCode.ENROLLMENT_REQUEST_ALREADY_PROCESSED);
        }
        request.setStatus(EnrollmentRequestStatus.CANCELLED);
        requestRepository.save(request);
    }

    // ---- helpers ----

    private ClassEnrollmentRequest createRequest(SchoolClass schoolClass, User student) {
        ClassEnrollmentRequest request = new ClassEnrollmentRequest();
        request.setSchoolClass(schoolClass);
        request.setStudent(student);
        request.setAmount(schoolClass.getFullPrice());
        request.setRegistrationCode(generateUniqueCode(schoolClass.getCode()));
        request.setStatus(EnrollmentRequestStatus.PENDING);
        return requestRepository.save(request);
    }

    private RegistrationResponse buildResponse(ClassEnrollmentRequest request) {
        PaymentSettings settings = paymentSettingsRepository.findById(PaymentSettings.SINGLETON_ID)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_SETTINGS_MISSING));
        String qrPayload = vietQrService.build(
                settings.getBankBin(), settings.getAccountNumber(), request.getAmount(), request.getRegistrationCode());
        return RegistrationResponse.from(
                request, qrPayload, settings.getBankName(), settings.getAccountNumber(), settings.getAccountName());
    }

    private String generateUniqueCode(String classCode) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = classCode + "-" + randomSuffix(4);
            if (!requestRepository.existsByRegistrationCode(candidate)) {
                return candidate;
            }
        }
        throw new AppException(ErrorCode.INTERNAL_ERROR, "Khong sinh duoc ma dang ky duy nhat");
    }

    private String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean hasRole(User u, RoleName role) {
        return u.getRoles().stream().anyMatch(r -> r.getName() == role);
    }
}
