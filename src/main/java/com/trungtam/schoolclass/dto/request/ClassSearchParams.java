package com.trungtam.schoolclass.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassSearchParams {

    private String code;
    private String name;
    private Long subjectId;
    private String status;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
