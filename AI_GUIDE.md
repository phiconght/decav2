# AI_GUIDE — Backend (BE)

> Tài liệu ngữ cảnh cho AI/dev tiếp nhận. Đọc xong file này là đủ để code tiếp BE mà không cần dò lại.
> Cập nhật khi thay đổi kiến trúc lớn.

## 1. Đây là gì
API Server cho **hệ thống quản lý trung tâm đào tạo**. Hiện đã có **Auth (đăng nhập/làm mới/đăng xuất) + RBAC (user/role/permission)** và các module chung. Các module nghiệp vụ (lớp học, điểm danh, điểm, học phí, WebSocket realtime) **chưa làm** — sẽ thêm sau.

Liên quan: frontend web `ADMIN/` (Ant Design Pro) và app `MOBILE/` (Flutter) đều gọi vào BE này.

## 2. Công nghệ
| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ / Framework | Java 21, Spring Boot **3.4.3** |
| Bảo mật | Spring Security 6 + JWT (jjwt 0.12.6): access token (15') + refresh token (7 ngày) |
| Web | Spring Web MVC, springdoc-openapi (Swagger) 2.8.6 |
| Persistence | Spring Data JPA (Hibernate) |
| DB (prod) | PostgreSQL + Flyway migration |
| DB (dev) | **PostgreSQL** (Docker, profile `dev`) + Flyway migration |
| Tiện ích | Lombok **1.18.44** (pin để hợp JDK/IntelliJ) |
| Build | Maven (máy này: `C:\develop\apache-maven-3.9.9`) |

**Port: 9090.** Swagger: `http://localhost:9090/swagger-ui.html`.

## 3. Cách chạy
**Bước 1 — Cần một PostgreSQL ở `localhost:5432` với db/user/pass = `center`/`center`/`center`.** Chọn 1 trong 2:
- **PostgreSQL cài sẵn trên máy** (máy dev hiện tại: **PostgreSQL 17 native**, không dùng Docker). Tạo db + user một lần:
  ```sql
  CREATE USER center WITH PASSWORD 'center';
  CREATE DATABASE center OWNER center;
  ```
- **Hoặc dùng Docker** (nếu máy có Docker Desktop): `docker compose up -d` (tạo db `center` trong volume `center_pgdata`).

**Bước 2 — Chạy BE:**
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
& "C:\develop\apache-maven-3.9.9\bin\mvn.cmd" -f "pom.xml" spring-boot:run "-Dspring-boot.run.profiles=dev"
```
- Trong **IntelliJ**: Run Configuration → **Active profiles = `dev`**.
- **Tài khoản admin mặc định** (tự tạo lúc khởi động): `admin` / `Admin@123`.
- Dev dùng PostgreSQL + Flyway → **dữ liệu bền**, không mất khi tắt BE.
- Reset sạch DB (chạy lại toàn bộ migration từ đầu):
  - Native: `psql -U center -d center -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"`
  - Docker: `docker compose down -v && docker compose up -d`

## 4. Cấu trúc thư mục (`src/main/java/com/trungtam/`)
```
CenterApiApplication.java        # entry point, @EnableJpaAuditing
common/
  dto/         ApiResponse, ApiError, PageResponse   # bao response chuẩn
  exception/   ErrorCode (enum mã lỗi + HTTP status), AppException, GlobalExceptionHandler
  entity/      BaseEntity (audit: created/updated at/by)
  audit/       AuditorAwareConfig (điền người tạo/sửa)
security/
  JwtProperties, JwtService            # sinh/parse JWT (HMAC)
  JwtAuthenticationFilter              # đọc Bearer -> set SecurityContext
  CustomUserDetails(Service)           # nạp user cho Spring Security
  RestAuthEntryPoint                   # trả 401/403 dạng JSON
  SecurityConfig                       # stateless, CORS, @PreAuthorize, public paths
  SecurityService ("securityService")  # KHUNG ownership-check cho @PreAuthorize (mở rộng sau)
  SecurityUtils                        # lấy user hiện tại
identity/
  entity/      User, Role, Permission, RefreshToken, RoleName(enum), UserStatus(enum)
  repository/  *Repository (Spring Data JPA)
  dto/         LoginRequest, RefreshRequest, TokenResponse, UserResponse, CreateUserRequest, AssignRolesRequest
  service/     AuthService (login/refresh/logout), UserService (CRUD user + gán role)
  controller/  AuthController (/api/v1/auth/**), UserController (/api/v1/admin/users)
  bootstrap/   AdminInitializer (tạo admin), DevDataSeeder (seed role/permission cho profile dev)
resources/
  application.yml            # cấu hình mặc định = PROD (Postgres + Flyway)
  application-dev.yml        # override = DEV (Postgres Docker, Flyway bật, format_sql true)
  db/migration/V1__init_auth.sql   # schema + seed role/permission (chạy cả dev lẫn prod)
```

## 5. Phân quyền (RBAC)
- 6 vai trò: `ADMIN, EMPLOYEE, TEACHER, ASSISTANT, STUDENT, PARENT` (enum `RoleName`).
- 2 cấp kiểm soát:
  1. **Role/Permission** (coarse): `@PreAuthorize("hasRole('ADMIN')")` hoặc `hasAuthority('USER:WRITE')`.
  2. **Ownership/scope** (fine, theo dữ liệu): qua bean `@securityService` trong SpEL, ví dụ tương lai `@PreAuthorize("@securityService.canAccessClass(#classId)")`. Hiện `SecurityService` mới là khung — bổ sung khi có module academic.
- Authority trong JWT gồm `ROLE_<name>` + từng permission code (`RESOURCE:ACTION`, vd `USER:WRITE`). Filter đọc thẳng từ token, **không query DB mỗi request**.

## 6. Luồng code chính

### 6.1 Luồng một request đã xác thực
```
HTTP request (Authorization: Bearer <jwt>)
  -> JwtAuthenticationFilter: parse token, nạp authorities vào SecurityContext
  -> SecurityConfig: kiểm tra path (public? cần auth?)
  -> Controller (@PreAuthorize kiểm role/permission)
  -> Service (@Transactional, ném AppException nếu lỗi nghiệp vụ)
  -> Repository (JPA) -> DB
  -> trả về, bọc trong ApiResponse.ok(data)
Lỗi -> GlobalExceptionHandler -> ApiResponse.fail(ApiError)
```

### 6.2 Luồng đăng nhập (AuthService.login)
1. `AuthController.login` nhận `LoginRequest{username,password}`.
2. `AuthenticationManager.authenticate` → `CustomUserDetailsService` nạp user, so khớp BCrypt.
3. Sinh **access token (JWT)** + **refresh token (opaque, lưu SHA-256 hash ở bảng `refresh_tokens`)**.
4. Trả `TokenResponse{accessToken, refreshToken, tokenType, expiresInSeconds, user}` trong `ApiResponse`.
5. `/auth/refresh`: hash refresh gửi lên → tìm → kiểm còn hiệu lực → **xoay token** (thu hồi cũ, phát mới).
6. `/auth/logout`: thu hồi refresh token. `/auth/logout-all`: thu hồi mọi token của user.

### 6.3 Định dạng response (FE phụ thuộc vào cấu trúc này)
```json
// thành công
{ "success": true, "data": { ... }, "timestamp": "..." }
// lỗi
{ "success": false, "error": { "code": "INVALID_CREDENTIALS", "message": "..." }, "timestamp": "..." }
```

## 7. API hiện có
| Method | Path | Quyền |
|---|---|---|
| POST | `/api/v1/auth/login` | public |
| POST | `/api/v1/auth/refresh` | public |
| POST | `/api/v1/auth/logout` | đã đăng nhập |
| POST | `/api/v1/auth/logout-all` | đã đăng nhập |
| GET | `/api/v1/auth/me` | đã đăng nhập |
| GET/POST | `/api/v1/admin/users` | ADMIN |
| PUT | `/api/v1/admin/users/{id}/roles` | ADMIN |
| PUT | `/api/v1/admin/users/{id}/status` | ADMIN |

## 8. Cách THÊM một module nghiệp vụ mới (vd: "Lớp học")
Đi theo đúng lát cắt dọc, mỗi tầng một file:
1. **Entity** `academic/entity/ClassEntity.java` (extends `BaseEntity`).
2. **Repository** `academic/repository/ClassRepository.java` (extends `JpaRepository`).
3. **DTO** `academic/dto/*` (request/response dạng `record`).
4. **Service** `academic/service/ClassService.java` (`@Service`, `@Transactional`, ném `AppException(ErrorCode.X)`).
5. **Controller** `academic/controller/ClassController.java` (`@RestController`, `@PreAuthorize`, trả `ApiResponse`).
6. **Mã lỗi mới**: thêm vào enum `ErrorCode`.
7. **Migration**: thêm `db/migration/V{n}__*.sql` — Flyway chạy cả dev lẫn prod. Thêm permission mới cũng vào migration này.
8. **Ownership**: nếu cần giới hạn theo lớp/người → thêm method vào `SecurityService` và dùng trong `@PreAuthorize`.

## 9. Quy ước & lưu ý
- DTO ưu tiên Java `record`. Service nhận/đổ dữ liệu qua DTO, không lộ entity ra controller.
- Lỗi nghiệp vụ: luôn ném `AppException(ErrorCode.X)` — đừng trả lỗi thủ công.
- `application.yml` = PROD (Postgres + Flyway, `ddl-auto: validate`). KHÔNG đổi nó sang H2. Mọi tiện ích local nằm ở profile `dev`.
- CORS đang mở `allowedOriginPatterns("*")` để FE dev gọi được — **siết lại theo domain thật khi lên prod**.
- JWT secret/issuer ở `app.jwt.*` trong `application.yml`; đổi `APP_JWT_SECRET` ở prod.
- Git remote: `https://github.com/phiconght/decav2`.
