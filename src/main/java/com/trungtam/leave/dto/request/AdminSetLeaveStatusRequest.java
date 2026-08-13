package com.trungtam.leave.dto.request;

import com.trungtam.leave.entity.LeaveStatus;
import jakarta.validation.constraints.NotNull;

/** ADMIN dat truc tiep trang thai don nghi, bo qua dieu kien PH xac nhan. */
public record AdminSetLeaveStatusRequest(
        @NotNull LeaveStatus status
) {
}
