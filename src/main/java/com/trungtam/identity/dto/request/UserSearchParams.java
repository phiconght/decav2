package com.trungtam.identity.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Query params cho GET /admin/users — bind qua @ModelAttribute.
 * Tat ca cac truong deu optional; current/pageSize co gia tri mac dinh.
 */
@Getter
@Setter
public class UserSearchParams {
    private String username;
    private String fullName;
    private String phone;
    private String role;
    private String status;
    private Long classId;
    private Long teachingClassId;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
