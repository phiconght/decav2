package com.trungtam.exercise.dto.request;

import java.util.List;

/**
 * Cau truc file du lieu .json nhan tu buoc Word -> AI -> file du lieu (ngoai
 * he thong) — xem SPEC_NhapBaiTap_TuWord_QuaAI.md §5.1. subjectId/topicId
 * KHONG nam trong file (da chon o panel truoc khi upload).
 */
public record ImportBatchFileDto(
        String sourceFileName,
        String examName,
        List<ImportItemDto> items
) {}
