package com.trungtam.topic.dto;

import com.trungtam.topic.entity.Topic;

public record TopicListItem(
        Long id,
        Long subjectId,
        String name,
        int sortOrder
) {
    public static TopicListItem from(Topic t) {
        return new TopicListItem(
                t.getId(),
                t.getSubject().getId(),
                t.getName(),
                t.getSortOrder());
    }
}
