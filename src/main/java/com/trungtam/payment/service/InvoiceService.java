package com.trungtam.payment.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.service.NotificationService;
import com.trungtam.payment.dto.request.CreateInvoiceBatchRequest;
import com.trungtam.payment.dto.request.InvoiceSearchParams;
import com.trungtam.payment.dto.request.UpdateInvoiceRequest;
import com.trungtam.payment.dto.response.InvoiceItemLine;
import com.trungtam.payment.dto.response.InvoicePageResponse;
import com.trungtam.payment.dto.response.InvoicePreviewItem;
import com.trungtam.payment.dto.response.InvoiceQrResponse;
import com.trungtam.payment.dto.response.InvoiceResponse;
import com.trungtam.payment.dto.response.MyInvoiceItem;
import com.trungtam.payment.entity.InvoiceStatus;
import com.trungtam.payment.entity.PaymentSettings;
import com.trungtam.payment.entity.TuitionInvoice;
import com.trungtam.payment.entity.TuitionInvoiceItem;
import com.trungtam.payment.repository.PaymentSettingsRepository;
import com.trungtam.payment.repository.TuitionInvoiceItemRepository;
import com.trungtam.payment.repository.TuitionInvoiceRepository;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schoolclass.entity.PaymentType;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Nghiep vu dot thu hoc phi (SPEC_ThanhToan §2.4): preview / tao / xac nhan / thu / huy
 * + query mobile + QR. Phat thong bao FEE_CONFIRMED/FEE_PAID (§2.5).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvoiceService {

    /** Bang chu sinh payment_code, bo ky tu de nham (0,O,1,I). */
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LEN = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TuitionInvoiceRepository invoiceRepository;
    private final TuitionInvoiceItemRepository itemRepository;
    private final PaymentSettingsRepository settingsRepository;
    private final SchoolClassRepository classRepository;
    private final ClassSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final StudentParentRepository studentParentRepository;
    private final PricingService pricingService;
    private final VietQrService vietQrService;
    private final NotificationService notificationService;

    // ============================ PREVIEW ============================

    /** Preview dot thu ca lop trong ky — KHONG ghi DB (SPEC_ThanhToan §2.4). */
    public List<InvoicePreviewItem> preview(Long classId, LocalDate from, LocalDate to) {
        SchoolClass clazz = findClassOrThrow(classId);
        validatePaymentType(clazz);
        validatePeriod(from, to);
        List<User> roster = classRepository.findStudentsByClassIds(List.of(classId));
        List<InvoicePreviewItem> result = new ArrayList<>();
        for (User student : roster) {
            result.add(computePreview(clazz, student, from, to));
        }
        return result;
    }

    /** Tinh preview cho 1 HV: dem buoi tinh phi, gross, discount, amount. */
    private InvoicePreviewItem computePreview(SchoolClass clazz, User student, LocalDate from, LocalDate to) {
        List<ClassSession> sessions = sessionRepository.findFeeSessions(
                clazz.getId(), student.getId(), from, to);
        BigDecimal gross = BigDecimal.ZERO;
        for (ClassSession s : sessions) {
            gross = gross.add(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
        }
        BigDecimal discount = pricingService.discountPercentOf(clazz.getId(), student.getId());
        BigDecimal amount = applyDiscount(gross, discount);
        Long existing = invoiceRepository
                .findExistingInvoiceId(student.getId(), clazz.getId(), from, to)
                .orElse(null);
        return new InvoicePreviewItem(
                student.getId(), student.getFullName(), student.getUsername(),
                sessions.size(), gross, discount, amount, existing);
    }

    /** amount = floor(gross * (1 - discount/100) / 1000) * 1000 (lam tron xuong nghin — §9#2). */
    private BigDecimal applyDiscount(BigDecimal gross, BigDecimal discountPercent) {
        BigDecimal factor = BigDecimal.ONE.subtract(
                discountPercent.divide(HUNDRED, 6, RoundingMode.HALF_UP));
        BigDecimal raw = gross.multiply(factor);
        return raw.divide(THOUSAND, 0, RoundingMode.DOWN).multiply(THOUSAND);
    }

    // ============================ CREATE BATCH ============================

    @Transactional
    public List<InvoiceResponse> createBatch(CreateInvoiceBatchRequest req) {
        SchoolClass clazz = findClassOrThrow(req.classId());
        validatePaymentType(clazz);
        validatePeriod(req.from(), req.to());

        List<User> targets = resolveTargets(clazz, req.studentIds());
        List<InvoiceResponse> created = new ArrayList<>();
        for (User student : targets) {
            // HV da co dot thu trung ky -> bo qua (khong fail ca batch)
            if (invoiceRepository.findExistingInvoiceId(
                    student.getId(), clazz.getId(), req.from(), req.to()).isPresent()) {
                continue;
            }
            List<ClassSession> sessions = sessionRepository.findFeeSessions(
                    clazz.getId(), student.getId(), req.from(), req.to());
            if (sessions.isEmpty()) {
                continue; // khong co buoi tinh phi -> khong tao dot thu rong
            }
            BigDecimal gross = BigDecimal.ZERO;
            for (ClassSession s : sessions) {
                gross = gross.add(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
            }
            BigDecimal discount = pricingService.discountPercentOf(clazz.getId(), student.getId());
            BigDecimal amount = applyDiscount(gross, discount);

            TuitionInvoice inv = new TuitionInvoice();
            inv.setStudent(student);
            inv.setClazz(clazz);
            inv.setPeriodFrom(req.from());
            inv.setPeriodTo(req.to());
            inv.setSessionCount(sessions.size());
            inv.setGrossAmount(gross);
            inv.setDiscountPercent(discount);
            inv.setAmount(amount);
            inv.setPaymentCode(generatePaymentCode(student.getUsername()));
            inv.setStatus(InvoiceStatus.DRAFT);
            TuitionInvoice saved = invoiceRepository.save(inv);

            for (ClassSession s : sessions) {
                TuitionInvoiceItem item = new TuitionInvoiceItem();
                item.setInvoice(saved);
                item.setSession(s);
                item.setSessionDate(s.getSessionDate());
                item.setPrice(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
                itemRepository.save(item);
            }
            created.add(toResponse(saved));
        }
        return created;
    }

    /** studentIds null/rong -> ca lop; nguoc lai chi cac HV thuoc lop. */
    private List<User> resolveTargets(SchoolClass clazz, List<Long> studentIds) {
        List<User> roster = classRepository.findStudentsByClassIds(List.of(clazz.getId()));
        if (studentIds == null || studentIds.isEmpty()) {
            return roster;
        }
        return roster.stream().filter(u -> studentIds.contains(u.getId())).toList();
    }

    /** payment_code = username + "-" + 4 ky tu; retry neu dung UNIQUE (xac suat ~0). */
    private String generatePaymentCode(String username) {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(username).append('-');
            for (int i = 0; i < CODE_LEN; i++) {
                sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
            }
            String code = sb.toString();
            if (!invoiceRepository.existsByPaymentCode(code)) {
                return code;
            }
        }
        throw new AppException(ErrorCode.INTERNAL_ERROR, "Khong sinh duoc ma thanh toan");
    }

    // ============================ STATE MACHINE ============================

    @Transactional
    public InvoiceResponse confirm(Long invoiceId) {
        TuitionInvoice inv = findOrThrow(invoiceId);
        if (inv.getStatus() != InvoiceStatus.DRAFT) {
            throw new AppException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        inv.setStatus(InvoiceStatus.CONFIRMED);
        inv.setConfirmedAt(Instant.now());
        TuitionInvoice saved = invoiceRepository.save(inv);
        notifyFee(saved, NotificationType.FEE_CONFIRMED);
        return toResponse(saved);
    }

    @Transactional
    public List<InvoiceResponse> confirmBatch(List<Long> ids) {
        List<InvoiceResponse> result = new ArrayList<>();
        for (Long id : ids) {
            TuitionInvoice inv = invoiceRepository.findById(id).orElse(null);
            if (inv == null || inv.getStatus() != InvoiceStatus.DRAFT) {
                continue; // bo qua don khong hop le, khong fail ca lo
            }
            inv.setStatus(InvoiceStatus.CONFIRMED);
            inv.setConfirmedAt(Instant.now());
            TuitionInvoice saved = invoiceRepository.save(inv);
            notifyFee(saved, NotificationType.FEE_CONFIRMED);
            result.add(toResponse(saved));
        }
        return result;
    }

    @Transactional
    public InvoiceResponse markPaid(Long invoiceId, String note) {
        TuitionInvoice inv = findOrThrow(invoiceId);
        if (inv.getStatus() != InvoiceStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaidAt(Instant.now());
        if (StringUtils.hasText(note)) {
            inv.setNote(note);
        }
        TuitionInvoice saved = invoiceRepository.save(inv);
        notifyFee(saved, NotificationType.FEE_PAID);
        return toResponse(saved);
    }

    @Transactional
    public InvoiceResponse cancel(Long invoiceId) {
        TuitionInvoice inv = findOrThrow(invoiceId);
        if (inv.getStatus() == InvoiceStatus.PAID || inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        inv.setStatus(InvoiceStatus.CANCELLED);
        return toResponse(invoiceRepository.save(inv));
    }

    @Transactional
    public InvoiceResponse update(Long invoiceId, UpdateInvoiceRequest req) {
        TuitionInvoice inv = findOrThrow(invoiceId);
        if (inv.getStatus() != InvoiceStatus.DRAFT) {
            throw new AppException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        if (req.amount() != null) {
            inv.setAmount(req.amount().setScale(0, RoundingMode.DOWN));
        }
        if (req.note() != null) {
            inv.setNote(req.note());
        }
        return toResponse(invoiceRepository.save(inv));
    }

    /**
     * Cong/tru truc tiep so tien hoc phi cua 1 dot thu — dung doc lap voi
     * confirm() (khong gan voi buoc chuyen trang thai), cho phep Admin dieu
     * chinh bat cu luc nao (kể cả sau khi da CONFIRMED/PAID), tru dot thu
     * da CANCELLED. Cong don vao adjustmentAmount/adjustmentNote de giu
     * lai lich su dieu chinh qua nhieu lan.
     */
    @Transactional
    public InvoiceResponse adjust(Long invoiceId, BigDecimal adjustmentAmount, String adjustmentNote) {
        TuitionInvoice inv = findOrThrow(invoiceId);
        if (inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        if (adjustmentAmount == null || adjustmentAmount.signum() == 0) {
            throw new AppException(ErrorCode.INVOICE_ADJUSTMENT_INVALID);
        }
        BigDecimal finalAmount = inv.getAmount().add(adjustmentAmount);
        if (finalAmount.signum() < 0) {
            throw new AppException(ErrorCode.INVOICE_ADJUSTMENT_INVALID);
        }
        inv.setAmount(finalAmount);
        inv.setAdjustmentAmount(inv.getAdjustmentAmount().add(adjustmentAmount));
        String combinedNote = StringUtils.hasText(adjustmentNote) ? adjustmentNote : null;
        if (combinedNote != null) {
            inv.setAdjustmentNote(StringUtils.hasText(inv.getAdjustmentNote())
                    ? inv.getAdjustmentNote() + "; " + combinedNote
                    : combinedNote);
        }
        return toResponse(invoiceRepository.save(inv));
    }

    // ============================ THONG BAO (§2.5) ============================

    /**
     * Phat thong bao hoc phi cho HV + tat ca PH lien ket. dedupeKey theo spec.
     * notify chay REQUIRES_NEW nen trung dedupe khong keo rollback giao dich chinh.
     */
    private void notifyFee(TuitionInvoice inv, NotificationType type) {
        Long invoiceId = inv.getId();
        Long studentId = inv.getStudent().getId();
        Long classId = inv.getClazz().getId();
        String className = inv.getClazz().getName();
        String period = inv.getPeriodFrom().format(DATE_FMT) + "–" + inv.getPeriodTo().format(DATE_FMT);
        String amountStr = formatVnd(inv.getAmount());

        String shortTitle;
        String shortBody;
        String fullTitle;
        String fullContent;
        String prefix;
        if (type == NotificationType.FEE_CONFIRMED) {
            prefix = "FEE_CONFIRMED";
            shortTitle = "Thông báo học phí";
            shortBody = "Học phí lớp " + className + " kỳ " + period + ": " + amountStr
                    + " đ. Mở app để thanh toán.";
            fullTitle = "Thông báo học phí";
            fullContent = "Học viên: " + safeName(inv.getStudent())
                    + "\nLớp: " + className
                    + "\nKỳ: " + period
                    + "\nSố buổi: " + inv.getSessionCount()
                    + "\nSố tiền: " + amountStr + " đ"
                    + "\n\nBấm nút Thanh toán trong app để quét QR VietQR."
                    + "\nNội dung chuyển khoản: " + inv.getPaymentCode();
        } else { // FEE_PAID
            prefix = "FEE_PAID";
            shortTitle = "Đã nhận học phí";
            shortBody = "Trung tâm đã nhận học phí " + amountStr + " đ — lớp " + className
                    + " kỳ " + period + ". Cảm ơn quý phụ huynh.";
            fullTitle = "Đã nhận học phí";
            fullContent = "Trung tâm đã ghi nhận đã thu học phí."
                    + "\nHọc viên: " + safeName(inv.getStudent())
                    + "\nLớp: " + className
                    + "\nKỳ: " + period
                    + "\nSố tiền: " + amountStr + " đ"
                    + (inv.getPaidAt() != null ? "\nThời điểm ghi nhận: " + inv.getPaidAt() : "")
                    + (StringUtils.hasText(inv.getNote()) ? "\nGhi chú: " + inv.getNote() : "")
                    + "\n\nCảm ơn quý phụ huynh.";
        }
        String payload = "{\"invoiceId\":" + invoiceId + ",\"classId\":" + classId
                + ",\"studentId\":" + studentId + "}";

        // Hoc vien
        notificationService.notify(studentId, type, shortTitle, shortBody, fullTitle, fullContent,
                payload, prefix + ":" + invoiceId + ":" + studentId);
        // Tung phu huynh lien ket
        for (Long parentId : studentParentRepository.findParentIdsByStudentId(studentId)) {
            notificationService.notify(parentId, type, shortTitle, shortBody, fullTitle, fullContent,
                    payload, prefix + ":" + invoiceId + ":" + parentId);
        }
    }

    // ============================ QUERY (admin) ============================

    public InvoicePageResponse list(InvoiceSearchParams params) {
        Specification<TuitionInvoice> spec = buildSpec(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<InvoiceResponse> result = invoiceRepository.findAll(spec, pageable)
                .map(i -> InvoiceResponse.from(i, List.of()));
        return InvoicePageResponse.of(result);
    }

    public InvoiceResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ============================ QUERY (mobile) ============================

    /** SELF/con: STUDENT -> minh; PARENT -> con (check student_parents). Chi CONFIRMED/PAID. */
    public List<MyInvoiceItem> myInvoices(Long studentId) {
        Long target = resolveOwnedStudent(studentId);
        return invoiceRepository.findByStudentIdAndStatusInOrderByIdDesc(
                        target, List.of(InvoiceStatus.CONFIRMED, InvoiceStatus.PAID)).stream()
                .map(MyInvoiceItem::from)
                .toList();
    }

    /** QR cho invoice CONFIRMED (ownership check). */
    public InvoiceQrResponse qr(Long invoiceId) {
        TuitionInvoice inv = findOrThrow(invoiceId);
        // ownership: HV chinh minh hoac PH cua HV
        resolveOwnedStudent(inv.getStudent().getId());
        if (inv.getStatus() != InvoiceStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        PaymentSettings settings = settingsRepository.findById(PaymentSettings.SINGLETON_ID)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_SETTINGS_MISSING));
        String payload = vietQrService.build(
                settings.getBankBin(), settings.getAccountNumber(), inv.getAmount(), inv.getPaymentCode());
        return new InvoiceQrResponse(
                payload, settings.getBankName(), settings.getAccountNumber(),
                settings.getAccountName(), inv.getAmount(), inv.getPaymentCode());
    }

    /**
     * Kiem quyen so huu HV + tra ve studentId hop le.
     * ADMIN/EMPLOYEE: bat ky (can studentId). STUDENT: chinh minh. PARENT: con lien ket.
     */
    private Long resolveOwnedStudent(Long requestedStudentId) {
        User me = currentUser();
        boolean isStudent = hasRole(me, RoleName.STUDENT);
        boolean isParent = hasRole(me, RoleName.PARENT);
        boolean isStaff = hasRole(me, RoleName.ADMIN) || hasRole(me, RoleName.EMPLOYEE);

        if (isStudent && (requestedStudentId == null || requestedStudentId.equals(me.getId()))) {
            return me.getId();
        }
        Long target = requestedStudentId;
        if (target == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Thieu studentId");
        }
        if (isStaff) {
            return target;
        }
        if (isParent && studentParentRepository
                .findByStudentIdAndParentId(target, me.getId()).isPresent()) {
            return target;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    // ============================ HELPERS ============================

    private InvoiceResponse toResponse(TuitionInvoice inv) {
        List<InvoiceItemLine> items = itemRepository
                .findByInvoiceIdOrderBySessionDateAsc(inv.getId()).stream()
                .map(InvoiceItemLine::from)
                .toList();
        return InvoiceResponse.from(inv, items);
    }

    private Specification<TuitionInvoice> buildSpec(InvoiceSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (params.getClassId() != null) {
                predicates.add(cb.equal(root.get("clazz").get("id"), params.getClassId()));
            }
            if (params.getStudentId() != null) {
                predicates.add(cb.equal(root.get("student").get("id"), params.getStudentId()));
            }
            if (StringUtils.hasText(params.getStatus())) {
                predicates.add(cb.equal(root.get("status"),
                        InvoiceStatus.valueOf(params.getStatus().trim().toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort resolveSort(String field, String order) {
        String sortField = switch (StringUtils.hasText(field) ? field : "createdAt") {
            case "amount" -> "amount";
            case "status" -> "status";
            case "periodFrom" -> "periodFrom";
            default -> "createdAt";
        };
        Sort.Direction dir = "ascend".equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortField);
    }

    /** Chi lop POSTPAID_TRANSFER moi dung dot thu hoc phi VND (§SPEC_ThanhToan). */
    private void validatePaymentType(SchoolClass clazz) {
        if (clazz.getPaymentType() != PaymentType.POSTPAID_TRANSFER) {
            throw new AppException(ErrorCode.CLASS_PAYMENT_TYPE_INVALID);
        }
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Khoang ky khong hop le");
        }
    }

    private static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return String.format("%,d", amount.setScale(0, RoundingMode.DOWN).toBigInteger())
                .replace(',', '.');
    }

    private static String safeName(User u) {
        return u.getFullName() != null ? u.getFullName() : u.getUsername();
    }

    private TuitionInvoice findOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
    }

    private SchoolClass findClassOrThrow(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
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
