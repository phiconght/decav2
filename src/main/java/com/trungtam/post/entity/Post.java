package com.trungtam.post.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Bai viet content hien tren Home mobile. Noi dung dang MARKDOWN.
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "content_md", nullable = false, columnDefinition = "text")
    private String contentMd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "published_at")
    private Instant publishedAt;
}
