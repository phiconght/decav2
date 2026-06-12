package com.trungtam.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Danh muc ma loi nghiep vu, gan san HTTP status. Dung chung toan he thong.
 */
public enum ErrorCode {

    // 400
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Du lieu khong hop le"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yeu cau khong hop le"),

    // 401
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Chua xac thuc"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Sai tai khoan hoac mat khau"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Token khong hop le hoac da het han"),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "Token da bi thu hoi"),

    // 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Khong co quyen truy cap"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Tai khoan da bi vo hieu hoa"),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay tai nguyen"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay nguoi dung"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay vai tro"),

    // 409
    USERNAME_EXISTS(HttpStatus.CONFLICT, "Ten dang nhap da ton tai"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "Email da ton tai"),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Loi he thong");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
