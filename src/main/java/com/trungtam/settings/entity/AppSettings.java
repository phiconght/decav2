package com.trungtam.settings.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cau hinh dung chung toan he thong. Chi 1 dong duy nhat (id = 1) — cung ky
 * thuat singleton nhu {@code payment_settings} (SPEC_ThanhToan §1).
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
public class AppSettings extends BaseEntity {

    /** Luon = 1 (single-row). */
    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    /** So dien thoai tong dai ho tro — hien tren moi Card/trang chi tiet khoa hoc (Web/Mobile). */
    @Column(name = "support_hotline", nullable = false, length = 20)
    private String supportHotline;
}
