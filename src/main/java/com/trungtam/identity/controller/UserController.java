package com.trungtam.identity.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.identity.dto.UserResponse;
import com.trungtam.identity.dto.CreateUserRequest;
import com.trungtam.identity.dto.request.ResetPasswordRequest;
import com.trungtam.identity.dto.request.UpdateUserRequest;
import com.trungtam.identity.dto.request.UpdateUserStatusRequest;
import com.trungtam.identity.dto.request.UserSearchParams;
import com.trungtam.identity.dto.response.UserPageResponse;
import com.trungtam.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan tri nguoi dung. Phan quyen theo tung endpoint (USER:READ / WRITE / DELETE).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER:READ')")
    public UserPageResponse list(@ModelAttribute UserSearchParams params) {
        return userService.search(params);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:READ')")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ApiResponse<UserResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.ok(userService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:DELETE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.softDelete(id);
        return ApiResponse.ok();
    }
}
