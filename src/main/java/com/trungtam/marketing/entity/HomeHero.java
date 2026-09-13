package com.trungtam.marketing.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Noi dung khoi Hero cua Trang chu cong khai Web (khach chua dang nhap) —
 * chi 1 dong duy nhat (id = 1), sua qua man quan tri "Noi Dung" cua ADMIN.
 * Xem migration V49 va KEHOACH_WEB_TrangChuCongKhai_HeroContent.md.
 */
@Entity
@Table(name = "home_hero")
@Getter
@Setter
@NoArgsConstructor
public class HomeHero extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "badge_text", length = 60)
    private String badgeText;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "subtitle", length = 400)
    private String subtitle;

    @Column(name = "primary_cta_label", length = 60)
    private String primaryCtaLabel;

    @Column(name = "primary_cta_href", length = 255)
    private String primaryCtaHref;

    @Column(name = "secondary_cta_label", length = 60)
    private String secondaryCtaLabel;

    @Column(name = "secondary_cta_href", length = 255)
    private String secondaryCtaHref;

    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    @Column(name = "visible", nullable = false)
    private boolean visible = true;
}
