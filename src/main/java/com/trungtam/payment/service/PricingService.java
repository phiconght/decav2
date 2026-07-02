package com.trungtam.payment.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.payment.dto.response.ClassPriceUpdateResponse;
import com.trungtam.payment.dto.response.SessionPriceItem;
import com.trungtam.payment.entity.ClassStudentPricing;
import com.trungtam.payment.repository.ClassStudentPricingRepository;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Dinh gia hoc phi: gia khoa/buoi + giam gia theo HV (SPEC_ThanhToan §2.3).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PricingService {

    private final SchoolClassRepository classRepository;
    private final ClassSessionRepository sessionRepository;
    private final ClassStudentPricingRepository pricingRepository;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    /**
     * Doi don gia/buoi cua khoa. Ap gia moi cho MOI buoi PLANNED chua bat dau va chua chinh tay.
     * Tra ve so buoi da cap nhat (FE hien "Da ap dung cho N buoi chua bat dau").
     */
    @Transactional
    public ClassPriceUpdateResponse updateClassPrice(Long classId, BigDecimal price) {
        SchoolClass clazz = findClassOrThrow(classId);
        clazz.setPricePerSession(price);
        classRepository.save(clazz);
        int updated = sessionRepository.bulkUpdateFuturePrice(classId, price, LocalDateTime.now(zone()));
        return new ClassPriceUpdateResponse(price, updated);
    }

    /**
     * Chinh gia le 1 buoi. Buoi phai PLANNED + chua bat dau, khong thi SESSION_ALREADY_STARTED.
     * Dat price + price_overridden = true.
     */
    @Transactional
    public SessionPriceItem updateSessionPrice(Long sessionId, BigDecimal price) {
        ClassSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (!isNotStarted(s)) {
            throw new AppException(ErrorCode.SESSION_ALREADY_STARTED);
        }
        s.setPrice(price);
        s.setPriceOverridden(true);
        sessionRepository.save(s);
        return SessionPriceItem.from(s);
    }

    /** Dat giam gia % rieng cho 1 HV trong lop (0..100). HV phai thuoc lop. */
    @Transactional
    public void updateStudentDiscount(Long classId, Long studentId, BigDecimal percent) {
        if (percent == null
                || percent.compareTo(BigDecimal.ZERO) < 0
                || percent.compareTo(new BigDecimal("100")) > 0) {
            throw new AppException(ErrorCode.INVALID_DISCOUNT);
        }
        findClassOrThrow(classId);
        ClassStudentPricing row = pricingRepository.findByClassAndStudent(classId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_IN_CLASS));
        row.setDiscountPercent(percent);
        pricingRepository.save(row);
    }

    /** Bang gia cac buoi cua lop trong khoang (SPEC_ThanhToan §2.9). */
    public List<SessionPriceItem> listSessionPrices(Long classId, LocalDate from, LocalDate to) {
        findClassOrThrow(classId);
        return sessionRepository.findByClazzIdAndSessionDateBetween(classId, from, to).stream()
                .sorted((a, b) -> {
                    int c = a.getSessionDate().compareTo(b.getSessionDate());
                    return c != 0 ? c : a.getStartTime().compareTo(b.getStartTime());
                })
                .map(SessionPriceItem::from)
                .toList();
    }

    /** Giam gia % cua 1 HV trong lop (mac dinh 0 neu chua dat). */
    public BigDecimal discountPercentOf(Long classId, Long studentId) {
        return pricingRepository.findByClassAndStudent(classId, studentId)
                .map(ClassStudentPricing::getDiscountPercent)
                .orElse(BigDecimal.ZERO);
    }

    // ---- helpers ----

    /** Buoi "chua bat dau" = PLANNED va session_date+start_time > now (theo timezone). */
    private boolean isNotStarted(ClassSession s) {
        if (s.getStatus() != SessionStatus.PLANNED) {
            return false;
        }
        LocalDateTime start = LocalDateTime.of(s.getSessionDate(), s.getStartTime());
        return start.isAfter(LocalDateTime.now(zone()));
    }

    private SchoolClass findClassOrThrow(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    private ZoneId zone() {
        return ZoneId.of(timezone);
    }
}
