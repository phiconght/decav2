package com.trungtam.exam.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamSearchParams {
    private String code;
    private String name;
    private Long subjectId;
    private String type;
    private String status;
    private Long classId;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
