package com.trungtam.exam.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Tham so loc + phan trang danh sach bai thi cho xac nhan. */
@Getter
@Setter
public class ExamConfirmationSearchParams {

    private Long examId;
    private Long classId;
    private int current = 1;
    private int pageSize = 10;
}
