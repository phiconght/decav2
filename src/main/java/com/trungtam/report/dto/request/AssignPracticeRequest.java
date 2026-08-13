package com.trungtam.report.dto.request;

/**
 * examId = bai thi PH dang xem (truy vet source) — KHONG BAT BUOC: null khi
 * giao bai truc tiep tu cap khoa/chuong/buoi (khong xuat phat tu 1 bai thi cu
 * the). topicId = chuong dang chon; null -> BE tu suy: tu bai thi (neu co) ->
 * chuong yeu nhat trong khoa (§10.3 Buoc 0, mo rong cho truong hop examId null).
 */
public record AssignPracticeRequest(
        Long examId,
        Long topicId
) {
}
