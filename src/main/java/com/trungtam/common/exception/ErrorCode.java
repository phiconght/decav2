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

    // 404 — Topic
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay chuyen de"),
    TOPIC_SUBJECT_MISMATCH(HttpStatus.BAD_REQUEST, "Chuyen de khong thuoc mon hoc da chon"),

    // 404 — Class
    CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay khoa hoc"),

    // 400 — Class enrollment
    NOT_A_STUDENT(HttpStatus.BAD_REQUEST, "Nguoi dung khong phai hoc sinh"),

    // 404 — Exam
    EXAM_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay de thi"),

    // 400 — Exam validation
    EXAM_INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "Thoi diem ket thuc phai sau thoi diem phat de"),
    EXAM_SUPPLEMENTARY_NO_STUDENT(HttpStatus.BAD_REQUEST, "De bo sung phai chon it nhat 1 hoc sinh"),
    EXAM_STATUS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Trang thai khong duoc phep dat thu cong"),

    // 403/400 — Lam bai de thi (hoc vien)
    EXAM_NOT_AVAILABLE(HttpStatus.FORBIDDEN, "De thi chua duoc phat hanh cho ban"),
    EXAM_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "Bai thi da duoc nop"),
    EXAM_TIME_OVER(HttpStatus.BAD_REQUEST, "Da het thoi gian lam bai"),

    // 404/409 — Room (co so / phong / ngay nghi)
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay co so"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay phong hoc"),
    HOLIDAY_DUPLICATED(HttpStatus.CONFLICT, "Ngay nghi da ton tai cho co so nay"),

    // 400 — Guardian / Teacher
    NOT_A_PARENT(HttpStatus.BAD_REQUEST, "Nguoi dung khong phai phu huynh"),
    NOT_A_TEACHER(HttpStatus.BAD_REQUEST, "Nguoi dung khong phai giao vien"),

    // 404/400/409 — Schedule (lich hoc / buoi hoc / diem danh)
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay quy tac lich"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay buoi hoc"),
    WEEKLY_REQUIRES_DAY(HttpStatus.BAD_REQUEST, "Lich tuan phai chon thu trong tuan"),
    SESSION_TIME_INVALID(HttpStatus.BAD_REQUEST, "Gio bat dau cong thoi luong phai trong ngay (<= 24:00)"),
    ROOM_TIME_CONFLICT(HttpStatus.CONFLICT, "Phong da co buoi hoc trung gio"),
    TEACHER_TIME_CONFLICT(HttpStatus.CONFLICT, "Giao vien da co buoi hoc trung gio"),
    QR_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Ma QR khong hop le hoac da het han"),
    NOT_IN_ROSTER(HttpStatus.FORBIDDEN, "Hoc vien khong thuoc buoi hoc"),

    // 400/403 — Cham cong giao vien
    NOT_SESSION_TEACHER(HttpStatus.FORBIDDEN, "Ban khong phai giao vien cua buoi hoc nay"),
    ROOM_QR_INVALID(HttpStatus.BAD_REQUEST, "Ma QR phong khong dung (sai phong hoac ma da doi)"),
    TEACHER_CHECKIN_TIME_INVALID(HttpStatus.BAD_REQUEST, "Ngoai khung gio cham cong cua buoi"),
    TEACHER_ALREADY_CHECKED_IN(HttpStatus.BAD_REQUEST, "Buoi nay da cham cong vao"),
    TEACHER_NOT_CHECKED_IN(HttpStatus.BAD_REQUEST, "Chua cham cong vao, khong the cham cong ra"),

    // 404/400 — Post / Announcement (content)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay bai viet"),
    ANNOUNCEMENT_INVALID_AUDIENCE(HttpStatus.BAD_REQUEST, "Doi tuong nhan thong bao khong hop le"),

    // 404/400 — Leave (nghi phep)
    LEAVE_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay don nghi phep"),
    LEAVE_ALREADY_REVIEWED(HttpStatus.BAD_REQUEST, "Don nghi phep da duoc xu ly"),
    LEAVE_INVALID_RANGE(HttpStatus.BAD_REQUEST, "Pham vi xin nghi khong hop le"),

    // 404/403/400 — Bao cao (nhan xet)
    REPORT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay nhan xet"),
    REPORT_COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "Khong co quyen sua/xoa nhan xet nay"),
    STUDENT_NOT_IN_CLASS(HttpStatus.BAD_REQUEST, "Hoc vien khong thuoc khoa hoc"),
    EXAM_NOT_IN_CLASS(HttpStatus.BAD_REQUEST, "De thi khong thuoc khoa hoc"),

    // 400 — Giao bai luyen tap (practice — SPEC_BaoCao §10)
    PRACTICE_BANK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "Kho bai tap khong du de sinh de luyen tap"),
    PRACTICE_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Da dat gioi han giao bai (con de chua lam hoac da giao hom nay)"),

    // 404/409/400 — Hoc phi (payment — SPEC_ThanhToan)
    PAYMENT_SETTINGS_MISSING(HttpStatus.CONFLICT, "Chua cau hinh tai khoan nhan tien"),
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Khong tim thay dot thu hoc phi"),
    INVOICE_STATUS_INVALID(HttpStatus.BAD_REQUEST, "Trang thai dot thu khong hop le cho thao tac nay"),
    INVOICE_DUPLICATE_PERIOD(HttpStatus.CONFLICT, "Hoc vien da co dot thu trong ky nay"),
    SESSION_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "Buoi hoc da bat dau/ket thuc, khong sua gia duoc"),
    INVALID_DISCOUNT(HttpStatus.BAD_REQUEST, "Muc giam gia phai tu 0 den 100"),

    // 400 — Xu hoc vien (coin — SPEC_ThanhToan §10)
    COIN_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "So Xu dieu chinh phai khac 0"),
    COIN_BALANCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, "So du Xu khong du de tru"),
    COIN_USER_NOT_STUDENT(HttpStatus.BAD_REQUEST, "Chi hoc vien moi co vi Xu"),

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
