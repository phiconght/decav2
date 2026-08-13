package com.trungtam.schedule.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.video.entity.LectureVideo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gan 1 video bai giang (kho) vao 1 buoi hoc cu the — N:N co thu tu.
 * Xem SPEC_VideoBaiGiang_Zoom.md §2.
 */
@Entity
@Table(name = "session_videos", uniqueConstraints = {
        @UniqueConstraint(name = "uq_session_video", columnNames = {"session_id", "video_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class SessionVideo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private LectureVideo video;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
