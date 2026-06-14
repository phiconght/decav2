package com.trungtam.subject.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectSearchParams {

    private String code;
    private String gradeLevel;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
