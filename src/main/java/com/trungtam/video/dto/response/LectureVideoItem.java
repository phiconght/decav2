package com.trungtam.video.dto.response;

import com.trungtam.video.entity.LectureVideo;

public record LectureVideoItem(
        Long id,
        String title,
        String youtubeUrl,
        String description,
        Long topicId,
        String topicName,
        String thumbnailUrl,
        Integer durationSeconds
) {
    public static LectureVideoItem from(LectureVideo v) {
        return new LectureVideoItem(
                v.getId(),
                v.getTitle(),
                v.getYoutubeUrl(),
                v.getDescription(),
                v.getTopic() != null ? v.getTopic().getId() : null,
                v.getTopic() != null ? v.getTopic().getName() : null,
                v.getThumbnailUrl(),
                v.getDurationSeconds());
    }
}
