package com.trungtam.topic.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.topic.dto.TopicListItem;
import com.trungtam.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    /** Danh sach chuyen de theo mon (?subjectId=). */
    @GetMapping
    @PreAuthorize("hasAuthority('TOPIC:READ')")
    public ApiResponse<List<TopicListItem>> list(
            @RequestParam(required = false) Long subjectId) {
        return ApiResponse.ok(topicService.listBySubject(subjectId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TOPIC:READ')")
    public ApiResponse<TopicListItem> getById(@PathVariable Long id) {
        return ApiResponse.ok(topicService.getById(id));
    }
}
