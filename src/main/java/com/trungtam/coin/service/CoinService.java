package com.trungtam.coin.service;

import com.trungtam.coin.dto.response.CoinBalanceResponse;
import com.trungtam.coin.dto.response.CoinTransactionItem;
import com.trungtam.coin.dto.response.CoinTransactionPageResponse;
import com.trungtam.coin.entity.CoinTransaction;
import com.trungtam.coin.entity.CoinWallet;
import com.trungtam.coin.repository.CoinTransactionRepository;
import com.trungtam.coin.repository.CoinWalletRepository;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiep vu Xu hoc vien (tien ao): admin cong/tru; HV/PH xem so du + lich su.
 * Mo hinh ledger + so du — doc so du O(1), moi lan cong/tru ghi 1 dong coin_transactions
 * kem balance_after de doi soat. Cong/tru chay duoi khoa bi quan chong race.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CoinService {

    private final CoinWalletRepository coinWalletRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;
    private final StudentParentRepository studentParentRepository;

    // ------------------------------------------------------------------
    // ADMIN / EMPLOYEE (COIN:READ / COIN:WRITE)
    // ------------------------------------------------------------------

    /**
     * Cong/tru Xu cho hoc vien (COIN:WRITE — chi ADMIN qua controller).
     * amount != 0; user phai la STUDENT; so du sau khong duoc am. Khoa bi quan chong race.
     */
    @Transactional
    public CoinBalanceResponse adjust(Long studentId, Long amount, String reason) {
        if (amount == null || amount == 0L) {
            throw new AppException(ErrorCode.COIN_AMOUNT_INVALID);
        }
        User student = findStudentOrThrow(studentId);

        // Khoa vi (SELECT ... FOR UPDATE); chua co -> lazy-create balance 0.
        CoinWallet wallet = coinWalletRepository.findWithLockByUserId(studentId)
                .orElseGet(() -> coinWalletRepository.save(new CoinWallet(studentId)));

        long newBalance = wallet.getBalance() + amount;
        if (newBalance < 0L) {
            throw new AppException(ErrorCode.COIN_BALANCE_INSUFFICIENT);
        }
        wallet.setBalance(newBalance);
        coinWalletRepository.save(wallet);

        CoinTransaction tx = new CoinTransaction();
        tx.setUser(student);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReason(reason);
        coinTransactionRepository.save(tx);

        return toBalance(student, newBalance);
    }

    /** So du hien tai cua HV (COIN:READ). Chua co vi -> tra balance 0 (khong loi). */
    public CoinBalanceResponse balance(Long studentId) {
        User student = findStudentOrThrow(studentId);
        long balance = coinWalletRepository.findById(studentId)
                .map(CoinWallet::getBalance)
                .orElse(0L);
        return toBalance(student, balance);
    }

    /** Lich su Xu cua HV (COIN:READ), phan trang phang. */
    public CoinTransactionPageResponse history(Long studentId, int current, int pageSize) {
        return toHistoryPage(studentId, current, pageSize);
    }

    // ------------------------------------------------------------------
    // MOBILE (isAuthenticated + ownership self/con — nhu /invoices/my)
    // ------------------------------------------------------------------

    /** So du Xu cua chinh minh (STUDENT) hoac cua con (PARENT truyen studentId). */
    public CoinBalanceResponse myBalance(Long studentId) {
        Long targetId = resolveOwnTarget(studentId);
        return balance(targetId);
    }

    /** Lich su Xu cua chinh minh (STUDENT) hoac cua con (PARENT truyen studentId). */
    public CoinTransactionPageResponse myHistory(Long studentId, int current, int pageSize) {
        Long targetId = resolveOwnTarget(studentId);
        return toHistoryPage(targetId, current, pageSize);
    }

    // ------------------------------------------------------------------

    private CoinTransactionPageResponse toHistoryPage(Long studentId, int current, int pageSize) {
        int page = Math.max(0, current - 1);
        int size = pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<CoinTransactionItem> result = coinTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(studentId, pageable)
                .map(CoinTransactionItem::from);
        return CoinTransactionPageResponse.of(result);
    }

    private CoinBalanceResponse toBalance(User student, long balance) {
        return new CoinBalanceResponse(
                student.getId(), student.getFullName(), student.getUsername(), balance);
    }

    /**
     * Ownership cho endpoint /coins/my (tuong tu /invoices/my):
     * - STUDENT: bo qua studentId, luon la chinh minh.
     * - PARENT: bat buoc truyen studentId la con (co lien ket student_parents), sai -> 403.
     */
    private Long resolveOwnTarget(Long studentId) {
        User me = currentUser();
        if (hasRole(me, RoleName.STUDENT)) {
            return me.getId();
        }
        if (hasRole(me, RoleName.PARENT)) {
            if (studentId == null
                    || studentParentRepository.findByStudentIdAndParentId(studentId, me.getId()).isEmpty()) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            return studentId;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private User findStudentOrThrow(Long studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!hasRole(user, RoleName.STUDENT)) {
            throw new AppException(ErrorCode.COIN_USER_NOT_STUDENT);
        }
        return user;
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
