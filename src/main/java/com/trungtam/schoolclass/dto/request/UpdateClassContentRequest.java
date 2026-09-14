package com.trungtam.schoolclass.dto.request;

public record UpdateClassContentRequest(
        String title,
        String coverImageUrl,
        String contentMd
) {}
