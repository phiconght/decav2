package com.trungtam.security;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Tien ich truy xuat thong tin nguoi dung dang dang nhap tu SecurityContext.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    public static String requireCurrentUsername() {
        return getCurrentUsername().orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }
}
