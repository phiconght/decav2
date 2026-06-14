package com.trungtam.subject.dto.response;

import com.trungtam.subject.entity.Subject;

public record SubjectListItem(Long id, String code, String name, String gradeLevel) {

    public static SubjectListItem from(Subject s) {
        return new SubjectListItem(s.getId(), s.getCode(), s.getName(), s.getGradeLevel());
    }
}
