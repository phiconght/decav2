package com.trungtam.subject.dto.response;

import com.trungtam.subject.entity.Subject;

public record SubjectDetailResponse(Long id, String code, String name, String gradeLevel) {

    public static SubjectDetailResponse from(Subject s) {
        return new SubjectDetailResponse(s.getId(), s.getCode(), s.getName(), s.getGradeLevel());
    }
}
