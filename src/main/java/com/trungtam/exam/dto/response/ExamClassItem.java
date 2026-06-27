package com.trungtam.exam.dto.response;

import java.util.List;

/**
 * 1 dong khoa hoc trong popup "So khoa" cua de thi.
 * teachers: giao vien phu trach khoa (ten + ma=username); studentCount: si so.
 */
public record ExamClassItem(
        Long classId,
        String code,
        String name,
        String subjectName,
        String gradeLevel,
        List<StudentOptionResponse> teachers,
        long studentCount
) {}
