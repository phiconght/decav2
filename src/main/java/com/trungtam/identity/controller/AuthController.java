package com.trungtam.identity.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.identity.dto.LoginRequest;
import com.trungtam.identity.dto.RefreshRequest;
import com.trungtam.identity.dto.TokenResponse;
import com.trungtam.identity.dto.UserResponse;
import com.trungtam.identity.service.AuthService;
import com.trungtam.identity.service.UserService;
import com.trungtam.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /** Dang nhap, tra ve cap access + refresh token. */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** Lam moi token (xoay refresh token). */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    /** Dang xuat: thu hoi refresh token duoc gui len. */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }

    /** Dang xuat tat ca thiet bi: thu hoi moi refresh token cua nguoi dung hien tai. */
    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll() {
        UserResponse me = userService.getByUsername(SecurityUtils.requireCurrentUsername());
        authService.logoutAll(me.id());
        return ApiResponse.ok();
    }

    /** Thong tin nguoi dung dang dang nhap (kem vai tro & quyen). */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(userService.getByUsername(SecurityUtils.requireCurrentUsername()));
    }
}
