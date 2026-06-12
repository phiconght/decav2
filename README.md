# Center API — Auth & RBAC (Bước 1)

API Server cho hệ thống quản lý trung tâm đào tạo. Bước này gồm **module đăng nhập/đăng xuất**, **phân quyền (RBAC)** và **các module chung**. Các module nghiệp vụ (lớp học, điểm danh, điểm, học phí, WebSocket…) sẽ bổ sung ở các bước sau.

## Công nghệ
- Java 21, Spring Boot 3.3
- Spring Security 6 + JWT (access + refresh token)
- PostgreSQL 16 + Flyway
- Maven, Lombok, springdoc OpenAPI

## Phân quyền
6 vai trò: `ADMIN`, `EMPLOYEE`, `TEACHER`, `ASSISTANT`, `STUDENT`, `PARENT`.
- **Role-based**: chặn ở endpoint qua `@PreAuthorize("hasRole('ADMIN')")`.
- **Permission-based**: quyền chi tiết dạng `RESOURCE:ACTION` (vd `USER:WRITE`), được nhúng trong JWT.
- **Ownership/scope** (theo dữ liệu): `SecurityService` — khung sẵn, sẽ hoàn thiện khi có module academic.

## Chạy thử

### 1. Khởi động PostgreSQL
```bash
docker compose up -d
```

### 2. Chạy ứng dụng
```bash
# Cần Maven (chưa cài: https://maven.apache.org/install.html)
mvn spring-boot:run
```
Lần đầu chạy, Flyway tạo schema + seed role/permission; `AdminInitializer` tạo tài khoản admin mặc định:
- username: `admin`
- password: `Admin@123`  ← **đổi ngay ở production** (biến `APP_ADMIN_PASSWORD`)

- Swagger UI: http://localhost:8080/swagger-ui.html

## API

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/v1/auth/login` | public | Đăng nhập → access + refresh token |
| POST | `/api/v1/auth/refresh` | public | Làm mới token (xoay refresh) |
| POST | `/api/v1/auth/logout` | đã đăng nhập | Thu hồi refresh token gửi lên |
| POST | `/api/v1/auth/logout-all` | đã đăng nhập | Thu hồi mọi refresh token của mình |
| GET | `/api/v1/auth/me` | đã đăng nhập | Thông tin + vai trò + quyền |
| GET | `/api/v1/admin/users` | ADMIN | Danh sách người dùng (phân trang) |
| POST | `/api/v1/admin/users` | ADMIN | Tạo người dùng |
| PUT | `/api/v1/admin/users/{id}/roles` | ADMIN | Gán vai trò |
| PUT | `/api/v1/admin/users/{id}/status` | ADMIN | Đổi trạng thái (ACTIVE/DISABLED/LOCKED) |

### Ví dụ đăng nhập
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```
Dùng `accessToken` cho các request sau: header `Authorization: Bearer <token>`.

## Cấu trúc
```
common/      # ApiResponse, exception, BaseEntity, audit
security/    # JWT, filter, SecurityConfig, SecurityService (ownership)
identity/    # user/role/permission: entity, repo, dto, service, controller
```

## Định dạng response
```json
{ "success": true, "data": { ... }, "timestamp": "..." }
{ "success": false, "error": { "code": "INVALID_CREDENTIALS", "message": "..." }, "timestamp": "..." }
```
