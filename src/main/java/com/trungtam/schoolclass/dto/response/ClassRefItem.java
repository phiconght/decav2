package com.trungtam.schoolclass.dto.response;

import com.trungtam.schoolclass.entity.SchoolClass;

/**
 * Thông tin gọn của 1 khóa để map vào dropdown (lấy theo list id).
 */
public record ClassRefItem(
        Long id,
        String code,
        String name,
        Long subjectId,
        String subjectName,
        String gradeLevel
) {
    public static ClassRefItem from(SchoolClass c) {
        return new ClassRefItem(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getSubject().getId(),
                c.getSubject().getName(),
                c.getSubject().getGradeLevel());
    }
}
