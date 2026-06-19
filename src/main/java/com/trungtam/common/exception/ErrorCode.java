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

    // 400 — User guard
    CANNOT_MODIFY_SELF(HttpStatus.BAD_REQUEST, "Khong the tu thay doi trang thai hoac quyen cua chinh minh"),
    LAST_ADMIN(HttpStatus.BAD_REQUEST, "Khong the vo hieu hoa admin cuoi cung"),
    CODE_DUPLICATED(HttpStatus.CONFLICT, "Ma bai tap da ton tai"),

    // 400 — Exercise validation
    NO_CORRECT_OPTION(HttpStatus.BAD_REQUEST, "Trac nghiem phai co it nhat 1 dap an dung"),
    MULTIPLE_CORRECT_OPTION(HttpStatus.BAD_REQUEST, "Trac nghiem chi duoc co 1 dap an dung"),

    // 404 — Exercise
    EXERCISE_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay bai tap"),

    // 404 — Subject
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay mon hoc"),

    // 404 — Class
    CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay lop hoc"),

    // 400 — Class enrollment
    NOT_A_STUDENT(HttpStatus.BAD_REQUEST, "Nguoi dung khong phai hoc sinh"),

    // 404 — Exam
    EXAM_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay de thi"),

    // 400 — Exam validation
    EXAM_INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "Thoi diem ket thuc phai sau thoi diem phat de"),
    EXAM_SUPPLEMENTARY_NO_STUDENT(HttpStatus.BAD_REQUEST, "De bo sung phai chon it nhat 1 hoc sinh"),

    // 400 — File validation
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "File rong"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "Dinh dang file khong ho tro"),

    // 404 — File
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay file"),

    // 413 — File size
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File vuot qua dung luong cho phep"),

    // 500
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Loi luu tru file"),
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
