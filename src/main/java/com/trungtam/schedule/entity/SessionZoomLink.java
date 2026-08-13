package com.trungtam.schedule.entity;

import com.trungtam.common.entity.BaseEntity;
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
 * Link Zoom cua 1 buoi hoc — 1:N (buoi co the co nhieu link: chinh, du phong, tach nhom).
 * Xem SPEC_VideoBaiGiang_Zoom.md §2.
 */
@Entity
@Table(name = "session_zoom_links")
@Getter
@Setter
@NoArgsConstructor
public class SessionZoomLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @Column(name = "label", nullable = false, length = 100)
    private String label = "Link chinh";

    @Column(name = "zoom_url", nullable = false, length = 500)
    private String zoomUrl;

    @Column(name = "meeting_id", length = 50)
    private String meetingId;

    @Column(name = "passcode", length = 50)
    private String passcode;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
