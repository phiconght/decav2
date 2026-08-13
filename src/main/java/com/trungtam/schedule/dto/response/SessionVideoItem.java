package com.trungtam.schedule.dto.response;

import com.trungtam.schedule.entity.SessionVideo;

public record SessionVideoItem(
        Long videoId,
        String title,
        String youtubeUrl,
        String thumbnailUrl,
        Integer durationSeconds,
        int sortOrder
) {
    public static SessionVideoItem from(SessionVideo sv) {
        return new SessionVideoItem(
                sv.getVideo().getId(),
                sv.getVideo().getTitle(),
                sv.getVideo().getYoutubeUrl(),
                sv.getVideo().getThumbnailUrl(),
                sv.getVideo().getDurationSeconds(),
                sv.getSortOrder());
    }
}
