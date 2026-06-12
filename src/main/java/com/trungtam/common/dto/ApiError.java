package com.trungtam.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Chi tiet loi tra ve client. `details` dung cho loi validation tung truong.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        List<FieldError> details
) {
    public ApiError(String code, String message) {
        this(code, message, null);
    }

    public record FieldError(String field, String message) {
    }
}
