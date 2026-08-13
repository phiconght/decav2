package com.trungtam.video.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.topic.entity.Topic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kho video bai giang (upload YouTube, dung lai nhieu buoi/nhieu lop).
 * Xem SPEC_VideoBaiGiang_Zoom.md §2.
 */
@Entity
@Table(name = "lecture_videos")
@Getter
@Setter
@NoArgsConstructor
public class LectureVideo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "youtube_url", nullable = false, unique = true, length = 500)
    private String youtubeUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Chuyen de de loc/tim khi gan vao buoi hoc, khong bat buoc. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
