package com.trungtam.topic.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.topic.dto.TopicListItem;
import com.trungtam.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    /** Danh sach chuyen de theo mon (cho dropdown). */
    public List<TopicListItem> listBySubject(Long subjectId) {
        if (subjectId == null) {
            return List.of();
        }
        return topicRepository.findBySubjectIdOrderBySortOrderAscNameAsc(subjectId).stream()
                .map(TopicListItem::from)
                .toList();
    }

    public TopicListItem getById(Long id) {
        return topicRepository.findById(id)
                .map(TopicListItem::from)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }
}
