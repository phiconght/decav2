package com.trungtam.coin.repository;

import com.trungtam.coin.entity.CoinTopupRequest;
import com.trungtam.coin.entity.CoinTopupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CoinTopupRequestRepository
        extends JpaRepository<CoinTopupRequest, Long>, JpaSpecificationExecutor<CoinTopupRequest> {

    boolean existsByPaymentCode(String paymentCode);

    List<CoinTopupRequest> findByStudentIdOrderByIdDesc(Long studentId);

    List<CoinTopupRequest> findByStudentIdAndStatusInOrderByIdDesc(Long studentId,
                                                                    List<CoinTopupStatus> statuses);
}
