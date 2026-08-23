package com.trungtam.exercise.dto.request;

/** 1 phuong an trac nghiem trong file du lieu — label chi de doc (debug), khong dung de map. */
public record ImportOptionDto(
        String label,
        String text,
        Boolean correct
) {}
