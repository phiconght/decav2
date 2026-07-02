package com.trungtam.announcement.dto.response;

import com.trungtam.announcement.entity.Announcement;
import com.trungtam.announcement.entity.AudienceType;

import java.time.Instant;

/**
 * Dong lich su thong bao da gui.
 */
public record AnnouncementItem(
        Long id,
        String title,
        AudienceType audience,
        String audienceRef,
        int sentCount,
        Long postId,
        Instant createdAt,
        String author
) {
    public static AnnouncementItem from(Announcement a) {
        return new AnnouncementItem(
                a.getId(),
                a.getTitle(),
                a.getAudience(),
                a.getAudienceRef(),
                a.getSentCount(),
                a.getPostId(),
                a.getCreatedAt(),
                a.getCreatedBy());
    }
}
