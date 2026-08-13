package com.trungtam.leave.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.leave.dto.request.AdminSetLeaveStatusRequest;
import com.trungtam.leave.dto.request.CreateLeaveRequest;
import com.trungtam.leave.dto.request.LeaveSearchParams;
import com.trungtam.leave.dto.response.LeaveItem;
import com.trungtam.leave.dto.response.LeavePageResponse;
import com.trungtam.leave.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @PreAuthorize("hasAuthority('LEAVE:READ')")
    public LeavePageResponse list(@ModelAttribute LeaveSearchParams params) {
        return leaveService.list(params);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LEAVE:WRITE')")
    public ApiResponse<LeaveItem> create(@Valid @RequestBody CreateLeaveRequest request) {
        return ApiResponse.ok(leaveService.create(request));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LEAVE:APPROVE')")
    public ApiResponse<LeaveItem> approve(@PathVariable Long id) {
        return ApiResponse.ok(leaveService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LEAVE:APPROVE')")
    public ApiResponse<LeaveItem> reject(@PathVariable Long id) {
        return ApiResponse.ok(leaveService.reject(id));
    }

    /** PHU HUYNH xac nhan don xin nghi cua con (yeu cau nguoi dung 13/08/2026). */
    @PatchMapping("/{id}/parent-confirm")
    @PreAuthorize("hasAuthority('LEAVE:CONFIRM')")
    public ApiResponse<LeaveItem> confirmByParent(@PathVariable Long id) {
        return ApiResponse.ok(leaveService.confirmByParent(id));
    }

    /** ADMIN dat truc tiep bat ky trang thai nao, bo qua dieu kien PH xac nhan. */
    @PatchMapping("/{id}/admin-status")
    @PreAuthorize("hasAuthority('LEAVE:APPROVE')")
    public ApiResponse<LeaveItem> adminSetStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminSetLeaveStatusRequest request) {
        return ApiResponse.ok(leaveService.adminSetStatus(id, request.status()));
    }
}
