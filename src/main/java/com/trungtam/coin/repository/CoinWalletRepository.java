package com.trungtam.coin.repository;

import com.trungtam.coin.entity.CoinWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CoinWalletRepository extends JpaRepository<CoinWallet, Long> {

    /**
     * Khoa bi quan (SELECT ... FOR UPDATE) khi cong/tru Xu — chong race 2 admin thao tac cung luc.
     * Dung trong transaction cua CoinService.adjust.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM CoinWallet w WHERE w.userId = :userId")
    Optional<CoinWallet> findWithLockByUserId(@Param("userId") Long userId);
}
