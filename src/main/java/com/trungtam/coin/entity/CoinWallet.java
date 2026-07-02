package com.trungtam.coin.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vi Xu cua hoc vien: 1 dong/HV, luu so du hien tai. Khoa chinh = user_id
 * (KHONG @GeneratedValue — id gan bang chinh id cua user, lazy-create o giao dich dau tien).
 */
@Entity
@Table(name = "coin_wallets")
@Getter
@Setter
@NoArgsConstructor
public class CoinWallet extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    public CoinWallet(Long userId) {
        this.userId = userId;
        this.balance = 0L;
    }
}
