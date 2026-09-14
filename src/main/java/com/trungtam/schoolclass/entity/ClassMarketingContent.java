package com.trungtam.schoolclass.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Noi dung hien thi (marketing) cua 1 khoa hoc — 1-1 voi {@link SchoolClass}
 * qua classId (vua la PK vua la FK). Rieng biet du lieu gia/lich hoc: bang
 * nay CHI chua thu de hien thi Card + trang chi tiet khoa hoc (Web/Mobile).
 */
@Entity
@Table(name = "class_marketing_content")
@Getter
@Setter
@NoArgsConstructor
public class ClassMarketingContent extends BaseEntity {

    @Id
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "content_md", columnDefinition = "text")
    private String contentMd;
}
