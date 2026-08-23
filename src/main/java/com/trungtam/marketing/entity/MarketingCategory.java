package com.trungtam.marketing.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1 nhom danh muc noi bat tren Trang chu mobile (vd "Luyện thi vào 10").
 * Xem docs/SPEC_Home_BaiViet_ThongBaoAdmin.md phan mo rong marketing (neu co)
 * hoac ThietKe/Mobile/files/m-home.html cho thiet ke goc.
 */
@Entity
@Table(name = "marketing_categories")
@Getter
@Setter
@NoArgsConstructor
public class MarketingCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "emoji", length = 8)
    private String emoji;

    @Column(name = "banner_tag", nullable = false, length = 30)
    private String bannerTag;

    @Column(name = "banner_headline", nullable = false, length = 150)
    private String bannerHeadline;

    @Column(name = "gradient_start", nullable = false, length = 9)
    private String gradientStart;

    @Column(name = "gradient_end", nullable = false, length = 9)
    private String gradientEnd;

    @Column(name = "accent_color", nullable = false, length = 9)
    private String accentColor;

    /**
     * Danh sach so khoi lop (phan cach dau phay, vd "6,7,8,9") dung de LOC
     * lop hoc THAT (bang classes/subjects.grade_level) vao danh muc nay.
     * Null/rong = khong loc (hien tat ca).
     */
    @Column(name = "grade_filter", length = 50)
    private String gradeFilter;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
