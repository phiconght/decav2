package com.trungtam.leave.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Tham so loc + phan trang danh sach don nghi.
 */
@Getter
@Setter
public class LeaveSearchParams {

    private Long studentId;
    private String status;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
