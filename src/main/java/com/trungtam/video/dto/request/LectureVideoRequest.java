package com.trungtam.video.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LectureVideoRequest(
        @NotBlank String title,
        @NotBlank String youtubeUrl,
        String description,
        Long topicId
) {}
