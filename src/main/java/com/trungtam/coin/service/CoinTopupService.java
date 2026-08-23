package com.trungtam.coin.service;

import com.trungtam.coin.dto.request.CoinTopupSearchParams;
import com.trungtam.coin.dto.request.CreateCoinTopupRequest;
import com.trungtam.coin.dto.response.CoinTopupPageResponse;
import com.trungtam.coin.dto.response.CoinTopupResponse;
import com.trungtam.coin.entity.CoinTopupRequest;
import com.trungtam.coin.entity.CoinTopupStatus;
import com.trungtam.coin.repository.CoinTopupRequestRepository;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.payment.entity.PaymentSettings;
import com.trungtam.payment.repository.PaymentSettingsRepository;
import com.trungtam.payment.service.VietQrService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Nap Xu bang chuyen khoan (VietQR) — ty le co dinh 1.000 VND = 1 Xu (yeu cau
 * nguoi dung). HOC SINH tu tao yeu cau (PENDING, chua cong Xu) -> ADMIN doi
 * soat ngan hang thu cong roi confirm() de cong Xu qua {@link CoinService#adjust}.
 * Mirror cau truc InvoiceService nhung don gian hon (1 HV/1 lan, khong theo ky).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CoinTopupService {

    /** 1.000 VND = 1 Xu (co dinh — yeu cau nguoi dung). */
    private static final BigDecimal VND_PER_COIN = BigDecimal.valueOf(1000);
    private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(10_000);

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LEN = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CoinTopupRequestRepository topupRepository;
    private final PaymentSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final StudentParentRepository studentParentRepository;
    private final CoinService coinService;
    private final VietQrService vietQrService;

    // ============================ HOC SINH TAO YEU CAU ============================

    @Transactional
    public CoinTopupResponse create(CreateCoinTopupRequest req) {
        User me = currentUser();
        if (!hasRole(me, RoleName.STUDENT)) {
            throw new AppException(ErrorCode.NOT_A_STUDENT);
        }
        if (req.amountVnd() == null || req.amountVnd().compareTo(MIN_AMOUNT) < 0) {
            throw new AppException(ErrorCode.COIN_TOPUP_AMOUNT_INVALID);
        }

        BigDecimal amount = req.amountVnd().setScale(0, RoundingMode.DOWN);
        long coinAmount = amount.divideToIntegralValue(VND_PER_COIN).longValueExact();

        CoinTopupRequest r = new CoinTopupRequest();
        r.setStudent(me);
        r.setAmountVnd(amount);
        r.setCoinAmount(coinAmount);
        r.setPaymentCode(generatePaymentCode(me.getUsername()));
        r.setStatus(CoinTopupStatus.PENDING);
        return toResponse(topupRepository.save(r));
    }

    // ============================ STATE MACHINE (Admin) ============================

    @Transactional
    public CoinTopupResponse confirm(Long id) {
        CoinTopupRequest r = findOrThrow(id);
        if (r.getStatus() != CoinTopupStatus.PENDING) {
            throw new AppException(ErrorCode.COIN_TOPUP_STATUS_INVALID);
        }
        coinService.adjust(r.getStudent().getId(), r.getCoinAmount(),
                "Nạp Xu qua chuyển khoản (" + r.getPaymentCode() + ")");
        r.setStatus(CoinTopupStatus.CONFIRMED);
        r.setConfirmedAt(Instant.now());
        return toResponse(topupRepository.save(r));
    }

    /**
     * Cong/tru truc tiep so Xu cua 1 yeu cau nap — doc lap voi confirm() (khong
     * gan voi buoc chuyen trang thai). Dung duoc ca khi con PENDING (chi sua
     * lai con so truoc khi confirm that) lan khi da CONFIRMED (day them/bot
     * chenh lech thang vao vi that qua {@link CoinService#adjust}), tru yeu
     * cau da CANCELLED.
     */
    @Transactional
    public CoinTopupResponse adjust(Long id, Long adjustmentCoinAmount, String adjustmentNote) {
        CoinTopupRequest r = findOrThrow(id);
        if (r.getStatus() == CoinTopupStatus.CANCELLED) {
            throw new AppException(ErrorCode.COIN_TOPUP_STATUS_INVALID);
        }
        if (adjustmentCoinAmount == null || adjustmentCoinAmount == 0L) {
            throw new AppException(ErrorCode.COIN_TOPUP_ADJUSTMENT_INVALID);
        }
        long finalCoinAmount = r.getCoinAmount() + adjustmentCoinAmount;
        if (finalCoinAmount < 0) {
            throw new AppException(ErrorCode.COIN_TOPUP_ADJUSTMENT_INVALID);
        }
        r.setCoinAmount(finalCoinAmount);
        r.setAmountVnd(BigDecimal.valueOf(finalCoinAmount).multiply(VND_PER_COIN));
        r.setAdjustmentCoinAmount(r.getAdjustmentCoinAmount() + adjustmentCoinAmount);
        if (StringUtils.hasText(adjustmentNote)) {
            r.setAdjustmentNote(StringUtils.hasText(r.getAdjustmentNote())
                    ? r.getAdjustmentNote() + "; " + adjustmentNote
                    : adjustmentNote);
        }
        if (r.getStatus() == CoinTopupStatus.CONFIRMED) {
            // Da cong Xu that vao vi luc confirm — chi day phan CHENH LECH vao vi.
            coinService.adjust(r.getStudent().getId(), adjustmentCoinAmount,
                    "Điều chỉnh nạp Xu (" + r.getPaymentCode() + ")"
                            + (StringUtils.hasText(adjustmentNote) ? ": " + adjustmentNote : ""));
        }
        return toResponse(topupRepository.save(r));
    }

    @Transactional
    public CoinTopupResponse cancel(Long id) {
        CoinTopupRequest r = findOrThrow(id);
        if (r.getStatus() != CoinTopupStatus.PENDING) {
            throw new AppException(ErrorCode.COIN_TOPUP_STATUS_INVALID);
        }
        r.setStatus(CoinTopupStatus.CANCELLED);
        return toResponse(topupRepository.save(r));
    }

    // ============================ QUERY (Admin) ============================

    public CoinTopupPageResponse list(CoinTopupSearchParams params) {
        Specification<CoinTopupRequest> spec = buildSpec(params);
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CoinTopupResponse> result = topupRepository.findAll(spec, pageable).map(this::toResponse);
        return CoinTopupPageResponse.of(result);
    }

    // ============================ QUERY (mobile/web self) ============================

    public List<CoinTopupResponse> myTopups(Long studentId) {
        Long target = resolveOwnedStudent(studentId);
        return topupRepository.findByStudentIdOrderByIdDesc(target).stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================ HELPERS ============================

    private CoinTopupResponse toResponse(CoinTopupRequest r) {
        PaymentSettings settings = settingsRepository.findById(PaymentSettings.SINGLETON_ID).orElse(null);
        if (settings == null) {
            return CoinTopupResponse.from(r, null, null, null, null);
        }
        String payload = vietQrService.build(
                settings.getBankBin(), settings.getAccountNumber(), r.getAmountVnd(), r.getPaymentCode());
        return CoinTopupResponse.from(r, payload,
                settings.getBankName(), settings.getAccountNumber(), settings.getAccountName());
    }

    private Specification<CoinTopupRequest> buildSpec(CoinTopupSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (params.getStudentId() != null) {
                predicates.add(cb.equal(root.get("student").get("id"), params.getStudentId()));
            }
            if (StringUtils.hasText(params.getStatus())) {
                predicates.add(cb.equal(root.get("status"),
                        CoinTopupStatus.valueOf(params.getStatus().trim().toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** payment_code = username + "-XU-" + 4 ky tu; retry neu dung UNIQUE (xac suat ~0). */
    private String generatePaymentCode(String username) {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(username).append("-XU-");
            for (int i = 0; i < CODE_LEN; i++) {
                sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
            }
            String code = sb.toString();
            if (!topupRepository.existsByPaymentCode(code)) {
                return code;
            }
        }
        throw new AppException(ErrorCode.INTERNAL_ERROR, "Khong sinh duoc ma nap Xu");
    }

    /**
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

    private CoinTopupRequest findOrThrow(Long id) {
        return topupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COIN_TOPUP_NOT_FOUND));
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
