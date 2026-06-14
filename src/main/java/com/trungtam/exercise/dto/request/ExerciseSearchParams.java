package com.trungtam.exercise.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Query params cho GET /exercises — bind qua @ModelAttribute.
 * Tat ca cac truong deu optional; current/pageSize co gia tri mac dinh.
 */
@Getter
@Setter
public class ExerciseSearchParams {
    private String code;
    private Long subjectId;
    private String createdBy;
    private String createdFrom;
    private String createdTo;
    private String status;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
