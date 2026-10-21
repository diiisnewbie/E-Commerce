# Phase 1 — Authentication & Authorization

> **Trạng thái:** Đã duyệt 2026-10-20 — chủ dự án chốt toàn bộ 10.4 theo đề xuất mặc định; triển khai theo mục 11.
> **Phụ thuộc:** Phase 0 (đã Done — skeleton, Testcontainers, CI, CORS cho `localhost:5173`).
> **Tài liệu liên quan:** [`docs/project-overview.md`](./project-overview.md) (mục 5, 7, 9), [`docs/phase-0-foundation.md`](./phase-0-foundation.md), [`AGENTS.md`](../AGENTS.md)
> **Cập nhật:** 2026-10-20 — rà soát kỹ thuật (dependency JWT, filter, luồng refresh, mật khẩu) + phản hồi review chủ dự án (A1–A6, B1–B6). Các câu hỏi đã chốt nằm ở mục 10.3.

---

## 1. Mục tiêu & Phạm vi

### 1.1. Mục tiêu

Hoàn thiện xác thực (authentication) và phân quyền (authorization) cho toàn bộ API: đăng ký, đăng nhập, vòng đời access/refresh token, logout có hiệu lực ngay, RBAC theo vai trò và tài khoản admin seed sẵn. Sau Phase 1, mọi phase sau chỉ cần "dán" endpoint vào các quy tắc bảo mật đã có.

### 1.2. Trong phạm vi Phase 1

| Hạng mục | Chi tiết |
|---|---|
| Đăng ký | `POST /api/v1/auth/register` — công khai, luôn tạo `USER` |
| Đăng nhập | `POST /api/v1/auth/login` — trả access + refresh token |
| Refresh token | `POST /api/v1/auth/refresh` — rotation; dùng lại token cũ → thu hồi phiên |
| Logout | `POST /api/v1/auth/logout` — thu hồi phiên ngay lập tức (blacklist `sid`) |
| Thông tin hiện tại | `GET /api/v1/auth/me` — dùng khôi phục phiên trên frontend |
| Admin endpoint mẫu | `GET /api/v1/admin/stats` — chứng minh RBAC (`ADMIN` only) |
| Filter xác thực | `JwtAuthenticationFilter`: verify chữ ký, kiểm `exp` và blacklist `sid` trên mọi request có token; **không ghi response lỗi và không chặn endpoint công khai** (xem 6.2) |
| Xử lý lỗi chuẩn | 401/403 JSON theo `ApiResponse`; bổ sung mã lỗi vào `docs/error-codes.md` |
| Seed admin | Flyway migration chèn tài khoản `ADMIN` đầu tiên (hash đọc từ env) |
| Frontend | Trang đăng ký/đăng nhập (antd), `AuthContext`, Axios interceptor gắn token + tự refresh, route bảo vệ |

### 1.3. Ngoài phạm vi Phase 1

- Xác minh email, quên mật khẩu / đặt lại mật khẩu, đăng nhập mạng xã hội (overview mục 2.2).
- Đổi mật khẩu, sửa hồ sơ, vô hiệu hóa tài khoản (phase sau nếu có yêu cầu).
- Refresh token cho thiết bị cụ thể với UI quản lý phiên (chỉ hỗ trợ multi-device ở mức nhiều phiên song song).
- Rate limiting brute-force (ghi nhận ở mục 10.2 — rủi ro đã biết).
- OAuth2/OpenID Connect, MFA.

---

## 2. Quyết định Thiết kế

### 2.1. Quyết định đã chốt trong `project-overview.md` mục 7 (bắt buộc tuân thủ)

| Chủ đề | Quyết định (nguyên văn overview) |
|---|---|
| Vai trò | `ROLE_USER`, `ROLE_ADMIN`; đăng ký công khai chỉ tạo `ROLE_USER`; admin đầu tiên được seed qua migration/biến môi trường |
| Ký JWT | HS256, secret đọc từ biến môi trường |
| Thời hạn token | Access token 15 phút; refresh token 7 ngày |
| Access token | Chứa `jti` và `sid` (mã phiên đăng nhập). Refresh thành công cấp access token mới nhưng **không** blacklist access token cũ: cùng một phiên, tối đa 15 phút còn hiệu lực |
| Refresh token | Có rotation; lưu dạng hash trong Redis kèm TTL. Dùng lại refresh token cũ đã rotate thì thu hồi cả phiên (`sid`) |
| Logout và thu hồi phiên | Đưa `sid` vào blacklist Redis với TTL bằng thời hạn access token (15 phút) và xóa refresh token của phiên. Mỗi request kiểm tra `sid` có bị thu hồi không, nên mọi access token cùng phiên bị từ chối ngay |
| State frontend | React Context + một module Axios dùng chung; không dùng Redux |

### 2.2. Thư viện JWT — Dùng module JOSE chính thức của Spring Security (Nimbus)

**Quyết định (D-1-01):** Không thêm thư viện JWT bên ngoài (jjwt, java-jose). Dùng JOSE support chính thức của Spring Security, chứa Nimbus JOSE+JWT.

**Artifact thật (đã kiểm tra Maven Central và Spring Boot 4.0 Migration Guide, 2026-10-20):**

- `org.springframework.security:spring-security-oauth2-jose` — module JOSE của Spring Security (tồn tại trên Maven Central); chứa `NimbusJwtEncoder`/`NimbusJwtDecoder`, kéo `nimbus-jose-jwt` theo transitive; phiên bản do BOM của Spring Boot quản lý.
- Nếu chọn dùng `BearerTokenAuthenticationFilter`: `org.springframework.boot:spring-boot-starter-security-oauth2-resource-server` — starter OAuth2 chuẩn của Boot 4 (đổi tên từ `spring-boot-starter-oauth2-resource-server` đang deprecated; mọi starter OAuth2 của Boot 4 đều có tiền tố `security-`).
- **`spring-boot-starter-oauth2-jose` KHÔNG tồn tại** — trên Maven Central không có artifact này và Spring Boot 4.0 Migration Guide không liệt kê nó. Tài liệu cũ ghi tên starter này là sai.

**Lý do:** Không thêm dependency ngoài hệ sinh thái Spring (overview mục 3); `NimbusJwtEncoder`/`NimbusJwtDecoder` là API công khai, tương thích chắc chắn với Spring Security 7 / Boot 4; tạo/parse token với claim tùy biến (`sid`, `role`, `type`) đầy đủ.

**So sánh hai cách tích hợp (đã chốt — O-1-01 → phương án A, xem 10.3):**

| | Phương án A — `spring-security-oauth2-jose` + filter tự viết (**đề xuất mặc định**) | Phương án B — starter resource-server + `BearerTokenAuthenticationFilter` + `OAuth2TokenValidator<Jwt>` tùy biến |
|---|---|---|
| Dependency | 1 module; không auto-config ngầm liên quan OAuth2 | Starter + auto-config resource-server |
| Kiểm soát filter | Toàn quyền: filter không ghi response, chỉ lưu mã lỗi vào request attribute (6.2) | `BearerTokenAuthenticationFilter` mặc định **vẫn trả 401 ngay** khi Bearer hỏng ngay cả trên endpoint `permitAll` — phải can thiệp entry point/filter tùy biến để xử lý như A |
| Blacklist `sid` | Kiểm ngay trong filter tự viết | Đặt trong `OAuth2TokenValidator<Jwt>` tùy biến (extension point được thiết kế sẵn) |
| Test | Tự viết test cho filter | Ít code hơn nhưng hành vi ẩn nhiều hơn; khó giải thích cho fresher |

Cả hai phương án đều phải giải quyết bài toán "Bearer hết hạn gửi kèm endpoint công khai" (A2/6.2); phương án B không free lunch. Tài liệu này mô tả chi tiết theo **phương án A — đã chốt 2026-10-20 (O-1-01, xem 10.3)**; bảng trên giữ làm tham chiếu để giải thích vì sao chọn A.

**Phương án bị loại:** `io.jsonwebtoken:jjwt` 0.13.x — phổ biến trong tutorial nhưng thêm 3 artifact bên ngoài; không có lợi thế gì khi Spring Security đã kèm Nimbus.

### 2.3. Thiết kế token và Redis — chi tiết hóa overview

**Access token** (JWT, HS256, TTL 15 phút):

| Claim | Ý nghĩa |
|---|---|
| `sub` | `userId` (chuỗi số) |
| `jti` | UUID — định danh token |
| `sid` | UUID — định danh phiên đăng nhập (mỗi lần login 1 phiên mới) |
| `role` | `USER` hoặc `ADMIN` |
| `type` | `access` |
| `iat`, `exp` | Thời điểm phát hành và hết hạn (giây) |

**Refresh token** (JWT, cùng secret, TTL 7 ngày): claim `sub`, `jti`, `sid`, `type=refresh`. Server lưu **SHA-256(token string)** trong Redis — không lưu token thô.

**Cấu trúc Redis** (D-1-02):

| Key | Kiểu | TTL | Nội dung |
|---|---|---|---|
| `auth:refresh:{jti}` | String | 7 ngày | SHA-256 hex của refresh token ứng với `jti` đó |
| `auth:session:{sid}` | Set | 7 ngày | Các `jti` refresh còn hiệu lực của phiên |
| `auth:blacklist:sid:{sid}` | String | 15 phút | Đánh dấu phiên đã thu hồi (đúng bằng TTL access token) |

**Luồng refresh với rotation nguyên tử (D-1-03):**

1. Parse refresh JWT (chữ ký + hạn). Sai/hết hạn → 401 `TOKEN_INVALID` / `TOKEN_EXPIRED`.
2. **Kiểm tra claim `type`:** `type != refresh` (gồm cả trường hợp access token bị gửi nhầm vào `/refresh`) → trả 401 `TOKEN_INVALID` **ngay lập tức, không chạm Redis, không thu hồi phiên.** *Hậu quả nếu thiếu bước này:* `jti` của access token không tồn tại trong `auth:refresh:*` → `GETDEL` trả `null` → bị nhầm là "dùng lại token đã rotate" (bước 3) → thu hồi oan cả phiên khi user chỉ gửi nhầm endpoint.
3. `GETDEL auth:refresh:{jti}` (Redis `GETDEL` — atomic read-and-delete; Spring Data Redis `getAndDelete`). Trả về `null` nghĩa là `jti` đã bị dùng (hoặc TTL hết) → coi là **dùng lại token đã rotate** (xem D-1-04).
4. So sánh hash đã lưu với SHA-256(presented token). Khớp mới tiếp tục. **Không khớp** → trả 401 `TOKEN_INVALID`, **không** thu hồi phiên (khác nhánh dùng lại ở bước 3 — không để token sai giết phiên đang sống).
5. **Kiểm blacklist `sid`:** đọc `auth:blacklist:sid:{sid}`; tồn tại → trả 401 `TOKEN_REVOKED`, không cấp token mới. *Mục đích:* nếu logout chạy xen giữa `GETDEL` (bước 3) và việc ghi key mới (bước 7), logout đã xóa session set và đặt blacklist; nếu bỏ qua bước này, refresh sẽ ghi `auth:refresh:{jti mới}` + `SADD` vào session set đã bị xóa → phiên "sống lại" sau logout (access token mới bị chặn tối đa 15 phút bởi blacklist, nhưng refresh token mới sống trọn 7 ngày).
6. **Query user theo claim `sub`:** user không tồn tại hoặc `is_active = FALSE` → thu hồi phiên (xóa session set + mọi `auth:refresh:{jti}` của phiên, đặt blacklist `sid`) → trả 401 `TOKEN_REVOKED` (không để sliding session giữ phiên của user đã bị xóa/khóa). Bình thường → lấy `role` **từ DB** cho token mới (không chép `role` từ token cũ).
7. Xóa `jti` khỏi `auth:session:{sid}`. Tạo `jti`/token mới (cùng `sid`, `role` từ DB), ghi key mới + `SADD` vào session set, cả hai set **TTL 7 ngày (được đặt lại mỗi lần rotation)**.
8. Kiểm lại `auth:blacklist:sid:{sid}` sau khi đã ghi (thu hẹp race ở bước 5); nếu tồn tại → xóa key vừa ghi + trả 401 `TOKEN_REVOKED`.
9. Trả access token mới + refresh token mới.

**Dùng lại token cũ (D-1-04):** Bước 3 trả `null` → thu hồi **toàn phiên**: xóa mọi `auth:refresh:{jti}` trong `auth:session:{sid}`, xóa session set, đặt `auth:blacklist:sid:{sid}` TTL 15 phút → trả 401 `TOKEN_REVOKED`. Mọi access token của phiên (kể cả vừa cấp) bị từ chối ngay. *Lưu ý:* token sai `type` **không** rơi vào nhánh này — đã bị chặn ở bước 2 với `TOKEN_INVALID`, không revoke; hash không khớp (bước 4) cũng trả `TOKEN_INVALID` không revoke.

**Logout (theo overview):** Xóa toàn bộ refresh token của phiên (giống trên) + đặt blacklist `sid` TTL 15 phút.

**Lưu ý hành vi có chủ đích:**
- Access token cũ **vẫn hợp lệ** tối đa 15 phút sau refresh — theo đúng chốt ở mục 7 overview; đòn bẩy là TTL ngắn + blacklist `sid` khi cần thu hồi thật sự.
- **Sliding session:** mỗi lần rotation đặt lại TTL 7 ngày cho key và session set, nên user hoạt động đều đặn sẽ không bị đăng xuất theo tuổi phiên. **Đã chốt (O-1-04):** Phase 1 **không** đặt hạn tuyệt đối — xem 10.3.
- **Nhiều tab:** hai tab dùng chung `localStorage` cùng một refresh token thì request thứ hai bị coi là "dùng lại" và thu hồi cả phiên (false positive). **Đã chốt (O-1-02 → phương án (a)):** FE dùng `navigator.locks` single-flight và đọc lại `localStorage` sau khi giành khóa (xem 7.1); rủi ro còn lại ghi ở R-1-07.

### 2.4. Mật khẩu và tài khoản admin

- **BCrypt** strength **10** (`BCryptPasswordEncoder`) — chỉ lưu hash (D-1-07).
- **Chính sách mật khẩu (D-1-16):** 8–**72 byte UTF-8** (không phải 72 ký tự — một ký tự tiếng Việt có dấu chiếm 2–3 byte). Validate theo số byte trước khi gọi `BCryptPasswordEncoder.encode`, áp dụng cho **cả `RegisterRequest` và `LoginRequest`** (`matches()` chỉ so 72 byte đầu nên payload dài vẫn tốn xử lý — phải chặn ngay ở DTO). *Đã xác minh trên source Spring Security (nhánh main, 2026-10-20):* `BCrypt.hashpw` mã hóa input bằng `StandardCharsets.UTF_8`; đường `encode` **ném `IllegalArgumentException("password cannot be more than 72 bytes")`** khi input > 72 byte (nếu không validate trước, request đăng ký sẽ thành 500); đường `matches` không ném lỗi mà chỉ so sánh ý nghĩa với 72 byte đầu (mật khẩu dài hơn 72 byte có cùng 72 byte đầu vẫn khớp — lý do bắt buộc chặn từ đầu). Email chuẩn hóa bằng `toLowerCase(Locale.ROOT)` trước khi lưu và so sánh (khớp `lower()` của PostgreSQL — tránh lệch locale); ràng buộc `UNIQUE` + `CHECK (email = lower(email))` ở DB.
- **Chống dò tài khoản bằng thời gian (B11):** chuẩn bị một **dummy BCrypt hash** cố định (hash của một mật khẩu ngẫu nhiên, sinh một lần khi triển khai — không phải secret thật). Khi email không tồn tại **hoặc** `is_active = FALSE`, service vẫn gọi `passwordEncoder.matches(input, dummy_hash)` rồi trả `INVALID_CREDENTIALS` — thời gian xử lý gần bằng trường hợp sai mật khẩu, không lộ email có tồn tại hay không.
- **Seed admin (D-1-08):** Migration Flyway chèn 1 dòng `ADMIN` bằng placeholder `${admin_password_hash}`. **Base `application.yml`** khai báo `spring.flyway.placeholders.admin_password_hash: ${ADMIN_PASSWORD_HASH}` **không có default** → thiếu env (và không nạp profile `local`/`test`) là fail-fast. `application-local.yml` **và** `application-test.yml` khai default `${ADMIN_PASSWORD_HASH:<hash bcrypt của Admin@123>}` — hash sinh một lần khi triển khai và commit (giống pattern dev-default của D-0-10); **test profile bắt buộc có** (TC-AUTH-15 đăng nhập bằng `Admin@123`). Mật khẩu dev: `Admin@123`, email `admin@ecommerce.dev`. Deploy thật: set `ADMIN_PASSWORD_HASH` = hash của mật khẩu riêng. *Lưu ý:* seed chỉ có tác dụng ở **lần migrate đầu trên DB trống**; `ON CONFLICT (email) DO NOTHING` + Flyway checksum khiến migration không chạy lại — đổi mật khẩu admin sau này phải update trực tiếp DB hoặc tạo user admin mới.
- **JWT secret (D-1-09 — đã chốt O-1-03):** `application.yml` gốc khai báo `${JWT_SECRET}` **không có default** → thiếu env ở bất kỳ profile nào cũng fail-fast (placeholder không phân giải được). Default dev chỉ nằm ở profile `local` (`application-local.yml`) và default test ở `application-test.yml`. Deploy thật: bắt buộc override qua env. *Ghi ý: file `.gitignore` hiện tại (cả root và `Backend/`) **không** ignore `application-local.yml` — đã kiểm tra 2026-10-20 — nên default dev commit được bình thường, không cần sửa `.gitignore`.*

*Phụ thuộc cấu hình (C7 — cập nhật theo chốt A2(a) ngày 2026-10-20):* Spring Boot **không** tự đọc file `.env` — `SPRING_PROFILES_ACTIVE=local` trong `.env` chỉ có tác dụng nếu được export vào env của process. Để default dev ở profile `local` (dòng trên) được nạp khi `./mvnw spring-boot:run`, profile `local` được cấu hình thẳng trong `spring-boot-maven-plugin` (parameter `profiles` của goal `run` — mẫu chính thức trên docs.spring.io, xác nhận trên bản 4.1.1): **chỉ `spring-boot:run` bật local**; jar chạy production không có profile mặc định → thiếu `JWT_SECRET`/`ADMIN_PASSWORD_HASH` là fail-fast thật. Chạy `java -jar` ở local cần thêm `-Dspring.profiles.active=local`; IDE chọn profile trong run configuration. (Cách trước đây là `spring.profiles.default: local` ở base — đã gỡ cùng ngày 2026-10-20 vì bật luôn default dev khi deploy quên `SPRING_PROFILES_ACTIVE`; xem `phase-0-foundation.md` 6.3, Q-0-11 → Q-0-12.)

### 2.5. Mô hình vai trò

`role` là cột `VARCHAR(20)` trên bảng `users` (`USER` / `ADMIN`), **không** dùng bảng `roles` + join (D-1-06).

**Lý do:** Dự án chỉ có 2 vai trò, mỗi user đúng 1 vai trò; overview mục 4.1: "Chỉ tạo abstraction khi có nhu cầu rõ ràng". Khi ánh xạ sang Security, prefix `ROLE_` được thêm trong code (`ROLE_USER`, `ROLE_ADMIN`) để dùng `hasRole` chuẩn.

### 2.6. Multi-device (D-1-11)

Mỗi lần login tạo `sid` mới → một user có nhiều phiên song song (nhiều thiết bị). Logout chỉ thu hồi phiên của access token đang dùng. Không có endpoint "đăng xuất mọi nơi" trong Phase 1.

### 2.7. Phân tách package (D-1-12)

- `com.app.backend.user`: `User` entity, `UserRepository`, `Role` enum — dữ liệu người dùng thuộc domain `user`.
- `com.app.backend.auth`: controller/service/repository-tokens/DTO — chỉ logic xác thực.
- `com.app.backend.admin`: `AdminController` — endpoint quản trị (Phase 1 chỉ có stats; phase sau mở rộng).

### 2.8. Lưu token phía frontend (D-1-10)

Access + refresh token lưu trong `localStorage`. **Đánh đổi đã chấp nhận:** localStorage dễ bị XSS đọc hơn httpOnly cookie. Bù lại: đơn giản, phù hợp hợp portfolio, và overview 4.7 đã định hướng Axios gắn `Authorization` header (mô hình Bearer thuần). Ghi nhận rủi ro ở mục 10.2.

---

## 3. Data Model & Migration

### 3.1. Bảng `users` — `V2__create_users.sql`

```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);
```

`ck_users_email_lowercase` là lớp phòng thủ thứ hai: service đã chuẩn hóa `toLowerCase(Locale.ROOT)` trước khi lưu, CHECK chặn mọi đường ghi bỏ sót (bug trong tương lai). **Không** tạo index cho `role` — cột chỉ có 2 giá trị, index không cải thiện truy vấn và ngược nguyên tắc tránh over-engineering (overview mục 4.1); `uk_users_email` đủ cho truy vấn chính.

### 3.2. Seed admin — `V3__seed_admin.sql`

```sql
INSERT INTO users (email, password_hash, full_name, role)
VALUES ('admin@ecommerce.dev', '${admin_password_hash}', 'System Admin', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
```

Giá trị placeholder chứa ký tự `$` (ví dụ `$2a$10$…`). Flyway thay placeholder một lần, không đệ quy; để chắc chắn hash không bị phá, TC-AUTH-15 assert trực tiếp định dạng hash trong DB và login thành công.

### 3.3. Quy tắc dữ liệu

| Trường | Quy tắc |
|---|---|
| `email` | Bắt buộc, định dạng hợp lệ, tối đa 255; chuẩn hóa lowercase trước khi lưu/so sánh; unique; CHECK `email = lower(email)` |
| `password` (input) | 8–72 **byte UTF-8** (validate theo byte — D-1-16); không lưu plaintext, chỉ hash BCrypt |
| `full_name` | 2–100 ký tự |
| `role` | Chỉ `USER` (mặc định) hoặc `ADMIN`; client không thể tự chọn `ADMIN` khi register (field `role` trong body bị bỏ qua) |
| `is_active` | Luôn `TRUE` ở Phase 1; login với `is_active = FALSE` trả `INVALID_CREDENTIALS` (chuẩn bị cho tính năng khóa tài khoản sau) |

---

## 4. API Contract

Tất cả response dùng `ApiResponse<T>`; timestamp UTC ISO 8601. Mọi mã lỗi mới phải được thêm vào `docs/error-codes.md` **trước hoặc cùng lúc** code merge.

### 4.1. Mã lỗi bổ sung cho `docs/error-codes.md`

| Mã lỗi | HTTP | Mô tả |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Request DTO không hợp lệ; `data` chứa chi tiết từng trường khi có |
| `INVALID_CREDENTIALS` | 401 | Email không tồn tại hoặc sai mật khẩu (cùng một mã, không phân biệt — chống dò tài khoản) |
| `TOKEN_EXPIRED` | 401 | Access token hết hạn; client nên gọi refresh |
| `TOKEN_INVALID` | 401 | Token thiếu, sai chữ ký, sai cấu trúc hoặc sai `type` |
| `TOKEN_REVOKED` | 401 | Phiên (`sid`) đã logout hoặc bị thu hồi do dùng lại refresh token |
| `ACCESS_DENIED` | 403 | Đã xác thực nhưng thiếu vai trò cần thiết |
| `EMAIL_ALREADY_EXISTS` | 409 | Email đã được đăng ký |
| `NOT_FOUND` | 404 | Route không tồn tại (`NoResourceFoundException`) |
| `METHOD_NOT_ALLOWED` | 405 | Method sai cho route (`HttpRequestMethodNotSupportedException`) |

### 4.2. `POST /api/v1/auth/register` — công khai

Request:

```json
{
  "email": "nguyenVanA@example.com",
  "password": "MatKhau123",
  "fullName": "Nguyen Van A"
}
```

Response **201**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "nguyenvana@example.com",
    "fullName": "Nguyen Van A",
    "role": "USER"
  },
  "message": "Registration successful",
  "errorCode": null,
  "timestamp": "2026-01-01T10:00:00Z"
}
```

Lỗi: 400 `VALIDATION_ERROR`, 409 `EMAIL_ALREADY_EXISTS` (kể cả khi hai request song song cùng email — xem 6.4).

### 4.3. `POST /api/v1/auth/login` — công khai

Request:

```json
{
  "email": "nguyenvana@example.com",
  "password": "MatKhau123"
}
```

Response **200** — `TokenResponse`:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresInSeconds": 900,
    "user": {
      "id": 1,
      "email": "nguyenvana@example.com",
      "fullName": "Nguyen Van A",
      "role": "USER"
    }
  },
  "message": "Login successful",
  "errorCode": null,
  "timestamp": "2026-01-01T10:00:00Z"
}
```

Lỗi: 400 `VALIDATION_ERROR`, 401 `INVALID_CREDENTIALS`.

### 4.4. `POST /api/v1/auth/refresh` — công khai

Request: `{ "refreshToken": "<jwt>" }` — Response **200**: cùng cấu trúc `TokenResponse` (access + refresh **mới**, `user` giữ nguyên). Refresh token cũ không còn hiệu lực ngay sau khi refresh thành công.

Lỗi: 400 `VALIDATION_ERROR`, 401 `TOKEN_EXPIRED`, 401 `TOKEN_INVALID`, 401 `TOKEN_REVOKED` (phát hiện dùng lại, phiên đã logout, hoặc user đã bị xóa/khóa — bước 6 của luồng refresh, xem 2.3).

*Lưu ý:* access token bị gửi nhầm vào `/refresh` trả 401 `TOKEN_INVALID` và **không** thu hồi phiên (xem 2.3, bước 2).

### 4.5. `POST /api/v1/auth/logout` — yêu cầu access token hợp lệ

Response **200**: `data: null`, message `"Logout successful"`. Phiên bị thu hồi ngay; request tiếp theo với access token vừa dùng trả 401 `TOKEN_REVOKED`.

Lỗi: 401 `TOKEN_EXPIRED` / `TOKEN_INVALID` / `TOKEN_REVOKED`.

### 4.6. `GET /api/v1/auth/me` — yêu cầu access token hợp lệ

Response **200**: `data` = `UserResponse` (id, email, fullName, role) lấy từ DB theo `sub` của token.

Lỗi: 401 như trên; 401 `TOKEN_INVALID` nếu user đã bị xóa (token còn hạn nhưng `sub` không còn trong DB).

### 4.7. `GET /api/v1/admin/stats` — yêu cầu `ROLE_ADMIN`

Response **200**:

```json
{
  "success": true,
  "data": { "totalUsers": 3 },
  "message": "Admin stats retrieved successfully",
  "errorCode": null,
  "timestamp": "2026-01-01T10:00:00Z"
}
```

Lỗi: 401 (không có token / token sai), 403 `ACCESS_DENIED` (role `USER`).

---

## 5. Luồng Nghiệp vụ (Given/When/Then)

### 5.1. Đăng nhập thành công

- **Given** user đã đăng ký với email/password đúng; chưa có phiên nào.
- **When** `POST /api/v1/auth/login` với email/password đúng.
- **Then** 200; tạo `sid` mới; ghi `auth:refresh:{jti}` + `auth:session:{sid}`; trả access (15 phút) + refresh (7 ngày) + `user`.

### 5.2. Đăng nhập sai mật khẩu / email không tồn tại

- **Given** email không tồn tại, **hoặc** email đúng nhưng sai mật khẩu, **hoặc** email đúng nhưng `is_active = FALSE`.
- **When** `POST /api/v1/auth/login`.
- **Then** 401 `INVALID_CREDENTIALS` với message và thời gian xử lý như nhau (không lộ email có tồn tại hay không). Với email không tồn tại hoặc `is_active = FALSE`, service vẫn so BCrypt với dummy hash cố định trước khi trả lỗi (xem 2.4 — chống dò thời gian).

### 5.3. Gọi API được bảo vệ với access token hợp lệ

- **Given** user đã đăng nhập, phiên chưa bị thu hồi.
- **When** request tới endpoint cần đăng nhập kèm `Authorization: Bearer <access>`.
- **Then** filter verify chữ ký + tự kiểm `exp`; kiểm tra `auth:blacklist:sid:{sid}` không tồn tại; request đi tiếp với `Authentication` chứa **userId + role + sid** (`sid` dùng cho logout blacklist — xem 6.2).

### 5.4. Refresh token thành công (rotation)

- **Given** access token hết hạn (hoặc sắp hết), refresh token còn hạn (`type=refresh`), phiên chưa thu hồi.
- **When** `POST /api/v1/auth/refresh` với refresh token hiện tại.
- **Then** 200; xác nhận chữ ký + hạn + `type=refresh` trước khi chạm Redis; `GETDEL` consume `jti` cũ; kiểm blacklist `sid` không tồn tại; query user theo `sub` (bị xóa/`is_active = FALSE` → thu hồi phiên, 401 `TOKEN_REVOKED`); cấp access + refresh mới (cùng `sid`, `role` lấy từ DB, TTL 7 ngày được đặt lại); access token cũ (nếu vẫn trong 15 phút) vẫn hoạt động theo chính sách overview.

### 5.5. Dùng lại refresh token đã rotate

- **Given** client A đã refresh thành công (token T1 → T2). Kẻ tấn công (hoặc client mất trạng thái) gửi lại T1.
- **When** `POST /api/v1/auth/refresh` với T1.
- **Then** chữ ký + hạn + `type` hợp lệ; `GETDEL` trả `null` → thu hồi toàn phiên: xóa toàn bộ refresh token của `sid`, blacklist `sid` 15 phút → 401 `TOKEN_REVOKED`. Access token T2 cũng bị từ chối ngay từ request sau.

*Phân nhánh:* access token (sai `type`) gửi vào `/refresh` → 401 `TOKEN_INVALID` ngay, **không** thu hồi phiên (xem 2.3, bước 2).

### 5.6. Logout

- **Given** phiên đang hoạt động.
- **When** `POST /api/v1/auth/logout` với access token hợp lệ.
- **Then** 200; xóa refresh token + session set của `sid`; blacklist `sid` TTL 15 phút. Request sau với access token cũ → 401 `TOKEN_REVOKED`. Gọi `/refresh` sau logout → 401 (key `auth:refresh:{jti}` đã bị xóa → `GETDEL` trả `null` → nhánh dùng lại → `TOKEN_REVOKED`).

### 5.7. Access token hết hạn

- **Given** access token quá `exp`.
- **When** request tới endpoint bảo vệ.
- **Then** 401 `TOKEN_EXPIRED` (phân biệt rõ với `TOKEN_INVALID` để frontend biết nên refresh thay vì đăng nhập lại).

### 5.8. User role gọi admin endpoint

- **Given** user `USER` đã đăng nhập hợp lệ.
- **When** `GET /api/v1/admin/stats`.
- **Then** 403 `ACCESS_DENIED`. (Chưa đăng nhập → 401 `TOKEN_INVALID`.)

### 5.9. Bearer hết hạn/sai gửi kèm endpoint công khai

- **Given** client còn giữ access token cũ đã hết hạn trong `localStorage` (Axios gắn header cho mọi request).
- **When** request tới `/auth/login`, `/auth/register` hoặc `/auth/refresh` kèm `Authorization: Bearer <hết hạn>`.
- **Then** request **đi tới controller bình thường** — filter chỉ lưu mã lỗi (`TOKEN_EXPIRED`) vào request attribute, không ghi response; các endpoint này là `permitAll` nên `AuthenticationEntryPoint` không chạy. Đăng nhập/refresh thành công như bình thường (xem 6.2, 7.1).

---

## 6. Bảo mật & Phân quyền

### 6.1. `SecurityConfig` — thay đổi so với Phase 0

```java
// Pseudocode — triển khai chi tiết khi code
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/health", "/actuator/health",
                         "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll()
        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .cors(Customizer.withDefaults())
    .exceptionHandling(ex -> ex
        .authenticationEntryPoint(authenticationEntryPoint)   // 401 JSON
        .accessDeniedHandler(accessDeniedHandler))            // 403 ACCESS_DENIED
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

Nguyên tắc: **mặc định từ chối** — mọi endpoint không nằm trong `permitAll` đều cần đăng nhập; endpoint admin cần `ROLE_ADMIN`. Phase 2+ chỉ cần thêm matcher quyền cho endpoint mới.

**Đăng ký filter đúng một lần:** `JwtAuthenticationFilter` chỉ được chạy đúng một lần trong chuỗi. Nếu khai báo là Spring bean (`@Bean`/`@Component`), phải vô hiệu hóa đăng ký servlet-container bằng `FilterRegistrationBean<JwtAuthenticationFilter>` với `setEnabled(false)` (chỉ giữ đăng ký trong `SecurityFilterChain` qua `addFilterBefore`); hoặc không đánh dấu bean và chỉ `new` trong `SecurityConfig`. Đăng ký đôi gây chạy filter hai lần (side effect gấp đôi, khó debug).

### 6.2. `JwtAuthenticationFilter` — không chặn endpoint công khai, không ghi response

**Nguyên tắc (D-1-14):** Axios phía client gắn `Authorization` vào mọi request, nên Bearer hết hạn luôn có thể gửi kèm `/auth/login`, `/auth/register`, `/auth/refresh`. Nếu filter tự ghi 401, các request này bị chặn trước khi tới controller → client không bao giờ refresh được → vòng lặp. Vì vậy:

- Filter **không bao giờ ghi response** và không throw ra chuỗi filter. Token hỏng → filter **không authenticate** và lưu mã lỗi (`TOKEN_EXPIRED` / `TOKEN_INVALID` / `TOKEN_REVOKED`) vào **request attribute** (ví dụ `auth.token.error`).
- `AuthenticationEntryPoint` chỉ chạy khi `authorizeHttpRequests` yêu cầu đăng nhập (endpoint `permitAll` không chạm tới nó) —EntryPoint đọc attribute để chọn mã lỗi trả về (6.4).

**Các bước filter:**

1. Đọc header `Authorization`. Không có header, hoặc không bắt đầu bằng `Bearer ` → cho qua (anonymous; `EntryPoint` xử lý nếu endpoint bắt buộc đăng nhập).
2. Parse access JWT bằng `NimbusJwtDecoder` (HS256). Sai chữ ký / sai cấu trúc / alg không hỗ trợ → attribute `TOKEN_INVALID`, cho qua.
3. Claim `type != access` → attribute `TOKEN_INVALID`, cho qua.
4. **Tự kiểm `exp`:** `jwt.getExpiresAt() <= now` → attribute `TOKEN_EXPIRED`, cho qua.
5. Đọc `sid` → tra `auth:blacklist:sid:{sid}`; tồn tại → attribute `TOKEN_REVOKED`, cho qua.
6. Hợp lệ → nạp `UserDetails` tối giản từ claim (`sub`, `role`, `sid`) — **không** query DB mỗi request; set `Authentication` vào context; cho qua.

**Phân biệt `TOKEN_EXPIRED` với `TOKEN_INVALID` (D-1-15 — đã xác minh bằng source Spring Security, 2026-10-20):**

- `NimbusJwtDecoder.decode()` chạy verify chữ ký trước, sau đó chạy `OAuth2TokenValidator`. Sai chữ ký → `BadJwtException` ("Signed JWT rejected: Invalid signature"). Token `alg=none` → `BadJwtException` ("Unsupported algorithm of …").
- Token hết hạn bị `JwtTimestampValidator` bắt và decode ném `JwtValidationException` với mô tả `"… Jwt expired at <instant>"`. **Không** phân biệt EXPIRED/INVALID bằng cách soi chuỗi mô tả lỗi này — dễ vỡ khi nâng cấp Spring Security (chuỗi do framework sinh, không phải API cam kết). Ngoài ra validator mặc định còn cho phép **clock skew 60 giây** — token "hết hạn" vẫn được decode thành công thêm 1 phút, trái với hành vi 5.7.
- **Cách làm được chọn:** cấu hình decoder với validator **tự viết, không kiểm timestamp** (ví dụ `decoder.setJwtValidator(...)` với validator rỗng/không kiểm `exp` — `setJwtValidator` thay thế bộ validator mặc định; verify chữ ký vẫn chạy trước trong processor). Sau khi `decode()` trả về thành công, filter **tự kiểm `jwt.getExpiresAt()`** theo đồng hồ hệ thống (không skew) rồi gán `TOKEN_EXPIRED`. Cách này tách bạch chữ ký (INVALID) và hạn (EXPIRED) bằng type exception + check claim, không phụ thuộc chuỗi mô tả lỗi.

`/me` là ngoại lệ: cần dữ liệu mới nhất từ DB nên service tự query theo `sub`.

### 6.3. Quản lý secret

| Biến | Local (default ở profile `local` — đã chốt O-1-03) | Test | Production |
|---|---|---|---|
| `JWT_SECRET` | Dev default ≥ 32 byte trong `application-local.yml` (ghi rõ "dev-only") | Secret riêng trong `application-test.yml` | **Bắt buộc** set env; thiếu → fail-fast ở mọi profile (base `application.yml` không có default) |
| `ADMIN_PASSWORD_HASH` | Hash bcrypt của `Admin@123` trong `application-local.yml` (default) | **Bắt buộc** — cùng hash `Admin@123` trong `application-test.yml` (TC-AUTH-15 đăng nhập `Admin@123`) | Base khai `${ADMIN_PASSWORD_HASH}` không default → thiếu env là fail-fast; đặt hash mật khẩu admin thật |

Cả hai biến đều được khai báo **không default** ở base `application.yml` (`${JWT_SECRET}`, `spring.flyway.placeholders.admin_password_hash: ${ADMIN_PASSWORD_HASH}`); default dev/test nằm trọn trong profile `local`/`test`. Với jar production không còn profile mặc định (đã chốt A2(a) — xem 2.4/C7), thiếu env là fail-fast thật.

Không ghi token, mật khẩu hay secret ra log (overview mục 4.5). Log đăng nhập thất bại chỉ ghi email, không ghi mật khẩu.

**Ký tự `$` trong hash BCrypt ở `.env` (B8):** hash bcrypt có dạng `$2a$10$…` chứa `$`. Khi đưa vào `.env` cho Docker Compose đọc: bọc giá trị trong **single quote** hoặc escape thành `$$` (Compose coi `$VAR` là interpolate biến). Khi `source .env` trong shell: cũng phải bọc single quote, nếu không shell sẽ mở rộng `$2a` thành chuỗi rỗng/lỗi. `.env.example` phải ghi chú rõ hai điều này cạnh giá trị mẫu. Hành vi placeholder Flyway với giá trị chứa `$` được xác minh bằng TC-AUTH-15 (assert hash trong DB + login thành công).

### 6.4. Các lớp lỗi

- `AuthenticationEntryPoint` (401): đọc request attribute do filter để lại — `TOKEN_EXPIRED` / `TOKEN_REVOKED` / `TOKEN_INVALID`; không có attribute (không kèm token) → mặc định `TOKEN_INVALID`.
- `AccessDeniedHandler` (403): `ACCESS_DENIED`.
- `GlobalExceptionHandler` mở rộng — **handler tường minh, không** kế thừa `ResponseEntityExceptionHandler` (lớp đó mặc định trả `ProblemDetail`, lệch định dạng `ApiResponse`):
  - `MethodArgumentNotValidException` → 400 `VALIDATION_ERROR` (kèm chi tiết trường);
  - `HttpMessageNotReadableException` → 400 `VALIDATION_ERROR` (body JSON hỏng hoặc thiếu body — nếu để catch-all `Exception.class` của Phase 0 bắt thì thành 500);
  - `HttpRequestMethodNotSupportedException` → 405 `METHOD_NOT_ALLOWED`; `NoResourceFoundException` → 404 `NOT_FOUND` (sai method / route không tồn tại không còn thành 500);
  - business exception riêng cho auth (ví dụ email trùng) → mã tương ứng;
  - `DataIntegrityViolationException` do vi phạm `uk_users_email` (hai register song song cùng email — một bên qua check `existsByEmail`, một bên không) → 409 `EMAIL_ALREADY_EXISTS`, không để thành 500. Service gọi **`saveAndFlush`** để exception ném ngay tại service thay vì lúc commit. Vi phạm constraint khác → 500 như bình thường.
- Spring Security 7 không ném exception ra `@RestControllerAdvice` cho 401/403 — bắt ở `EntryPoint`/`Handler` và self-inject `ObjectMapper` để viết JSON (dùng `ObjectMapper` Jackson 3 — package `tools.jackson`, xem `AGENTS.md` mục 5).

---

## 7. Frontend

### 7.1. Phạm vi

| Hạng mục | Chi tiết |
|---|---|
| Dependency mới | `react-router-dom` (overview mục 3 đã chốt React Router) |
| Trang | `/login`, `/register` (antd Form + validation client cơ bản) |
| `AuthContext` | State: `user`, `accessToken`, `refreshToken`; actions: `login`, `register`, `logout`; persist xuống `localStorage` (D-1-10); khôi phục phiên khi reload bằng `GET /auth/me` |
| Axios dùng chung | Mở rộng module Axios dùng chung hiện có là `src/api.js` (chứa instance Axios + `getHealth()` — cây thư mục Phase 0 đã được cập nhật 2026-10-20 cho khớp repo thật). **Request interceptor:** gắn `Authorization` cho mọi request **trừ** `/auth/login`, `/auth/register`, `/auth/refresh` (gửi kèm Bearer cũ gây nhiễu và là nguyên nhân vòng lặp refresh). **Response interceptor:** 401 `TOKEN_EXPIRED` → gọi refresh **một lần** (single-flight) → retry request gốc; **không retry chính request `/auth/refresh`** (401 từ refresh là kết thúc — clear storage, redirect `/login`); 401 khác (`TOKEN_REVOKED`/`TOKEN_INVALID`) → clear storage, redirect `/login`; **ngoại lệ:** 401 `INVALID_CREDENTIALS` từ chính request `/auth/login`/`/auth/register` là lỗi form — hiện message trên trang, **không** clear storage, **không** redirect (chặn vòng lặp đăng nhập) |
| Single-flight đa tab | Hai tab refresh cùng lúc sẽ false positive "dùng lại token" (2.3, R-1-07) — **đã chốt O-1-02 → phương án (a)**: mọi tab phải qua `navigator.locks.request('auth-refresh', ...)`; **sau khi giành khóa phải đọc lại `localStorage`** — nếu refresh token đã đổi nghĩa là tab khác refresh xong, bỏ qua và retry bằng token mới; đồng thời lắng nghe event `storage` để `AuthContext` đồng bộ user/token giữa các tab. Phương án (b) grace window server-side bị loại — không nới lỏng cơ chế revoke |
| Route bảo vệ | `RequireAuth` (chưa đăng nhập → `/login`), `RequireAdmin` (role ≠ `ADMIN` → `/` hoặc trang 403) |
| Điều hướng | Header hiển thị đăng nhập/đăng xuất đơn giản |

Mẫu single-flight với `navigator.locks` (chốt A4 — khóa chỉ có tác dụng khi đọc lại `localStorage` sau khi giành được):

```js
await navigator.locks.request('auth-refresh', async () => {
  if (localStorage.getItem('refreshToken') !== tokenUsedByFailedRequest) {
    return; // tab khác đã refresh xong, chỉ cần retry bằng token mới
  }
  // gọi /auth/refresh
});
```

`AuthContext` lắng nghe event `storage` để đồng bộ phiên khi tab khác đăng nhập/đăng xuất/refresh.

### 7.2. Frontend test (Vitest)

| ID | Nội dung |
|---|---|
| FE-AUTH-01 | `LoginPage` render, submit gọi `login` (mock API thành công), hiển thị user sau login |
| FE-AUTH-02 | `RequireAuth` redirect `/login` khi chưa có phiên; vào được route khi mock đã đăng nhập |

---

## 8. Kế hoạch Kiểm thử

### 8.1. Nguyên tắc

- Tiếp tục BaseIntegrationTest (Testcontainers PostgreSQL + Redis thật) — Redis cần thiết cho refresh/blacklist.
- Unit test cho `JwtService` (tạo/parse, claim, hạn) và logic rotation qua interface `RefreshTokenStore` (**đã chốt O-1-05** — tách interface, test với fake implementation, không cần Redis).
- **Bắt buộc:** mỗi endpoint được bảo vệ có ít nhất 1 test được phép và 1 test bị từ chối (overview mục 8).

### 8.2. Integration test cases

| ID | Case | Kỳ vọng |
|---|---|---|
| TC-AUTH-01 | Register hợp lệ | 201; email lowercase trong DB; `password_hash` ≠ plaintext; role `USER` |
| TC-AUTH-02 | Register email trùng (khác kiểu chữ hoa/thường) | 409 `EMAIL_ALREADY_EXISTS` |
| TC-AUTH-03 | Register input không hợp lệ (email sai, password < 8 byte, password > 72 byte UTF-8 — kể cả chỉ 30 ký tự tiếng Việt có dấu nhưng > 72 byte) | 400 `VALIDATION_ERROR` |
| TC-AUTH-04 | Login hợp lệ | 200; có access, refresh, `expiresInSeconds` = 900; refresh JWT có `type=refresh`, TTL ~7 ngày |
| TC-AUTH-05 | Login sai mật khẩu | 401 `INVALID_CREDENTIALS` |
| TC-AUTH-06 | Login email không tồn tại | 401 `INVALID_CREDENTIALS` (cùng message với 05) |
| TC-AUTH-07 | Gọi `/me` với access token hợp lệ | 200, đúng user |
| TC-AUTH-08 | Gọi `/me` không kèm token | 401 |
| TC-AUTH-09 | Access token hết hạn **10 giây trước** (tạo bằng JwtService với exp quá khứ 10s — nằm trong clock skew 60s của `JwtTimestampValidator` mặc định, nên chỉ trả về đúng khi đã bỏ timestamp validator — chứng minh D-1-15) | 401 `TOKEN_EXPIRED` |
| TC-AUTH-10 | Token bị sửa chữ ký / thiếu prefix Bearer | 401 `TOKEN_INVALID` |
| TC-AUTH-11 | Refresh thành công | 200; access + refresh mới; token cũ dùng để refresh lại → 401 |
| TC-AUTH-12 | Dùng lại refresh token đã rotate | 401 `TOKEN_REVOKED`; access token vừa cấp trước đó cũng bị từ chối |
| TC-AUTH-13 | Logout rồi gọi `/me` bằng access token cũ | 401 `TOKEN_REVOKED` |
| TC-AUTH-14 | `/admin/stats` với token role `USER` | 403 `ACCESS_DENIED` |
| TC-AUTH-15 | `/admin/stats` với tài khoản admin seed (login bằng dev admin); assert `password_hash` trong DB khớp regex bcrypt (`^\$2[a-z]\$\d\d\$`) | 200, có `totalUsers`; hash còn nguyên ký tự `$` (xác nhận Flyway placeholder không phá hash) |
| TC-AUTH-16 | Access token chứa `sid` đã bị blacklist (mô phỏng logout phiên khác cùng user) | 401 `TOKEN_REVOKED` |
| TC-AUTH-17 | **Access token gửi vào `/refresh`** (body chứa access token) | 401 `TOKEN_INVALID`; **phiên không bị thu hồi** — access token vẫn gọi `/me` thành công sau đó |
| TC-AUTH-18 | **Refresh token dùng làm `Authorization: Bearer` gọi API bảo vệ** (`/me`) | 401 `TOKEN_INVALID`; phiên không bị thu hồi — refresh vẫn dùng để refresh thành công sau đó |
| TC-AUTH-19 | **Bearer hết hạn gửi kèm `/auth/refresh` và `/auth/login`** | Request đi tới controller: refresh/login xử lý bình thường (200), không nhận 401 từ filter |
| TC-AUTH-20 | **Hai request refresh song song với cùng một refresh token** | **Tối đa một 200** — kết quả hợp lệ gồm (200, 401) lẫn (401, 401) tùy thứ tự request thắng/thua đến server, **không** được assert "đúng một 200"; **sau khi cả hai request kết thúc, phiên bị thu hồi**: gọi `/me` hoặc `/refresh` bằng token vừa nhận đều 401 `TOKEN_REVOKED`. Chứng minh `GETDEL` nguyên tử + nhánh dùng lại (D-1-04); đây là root cause của R-1-07 (FE single-flight — O-1-02(a) — để trường hợp này chỉ phát sinh khi hành vi bất thường) |
| TC-AUTH-21 | **Logout rồi gọi `/refresh`** bằng refresh token cũ | 401 (`TOKEN_REVOKED` — key đã bị xóa nên `GETDEL` trả `null`) |
| TC-AUTH-22 | **Register với `"role": "ADMIN"` trong body** | 201; role trong DB và response vẫn là `USER` |
| TC-AUTH-23 | **Login với user `is_active = FALSE`** | 401 `INVALID_CREDENTIALS` |
| TC-AUTH-24 | **Token `alg=none` và token ký bằng HS512 (cùng secret)** | Cả hai 401 `TOKEN_INVALID` (decoder chỉ chấp nhận HS256) |
| TC-AUTH-25 | **Response của register/login/me không chứa `passwordHash`** | Assert body không có trường `passwordHash`/`password` |
| TC-AUTH-26 | **Giới hạn mật khẩu theo byte:** mật khẩu 8+ ký tự tiếng Việt có dấu nhưng ≤ 72 byte UTF-8 → hợp lệ; mật khẩu ASCII dài 73 byte → 400; login với mật khẩu ASCII 73 byte → 400 `VALIDATION_ERROR` (không 500); không có case nào trả 500 | Đăng ký và login lại thành công với mật khẩu tiếng Việt ở ranh giới byte; login 73 byte bị từ chối 400 |
| TC-AUTH-27 | **Hai register song song cùng email** (2 thread, barrier) | Một bên 201, một bên 409 `EMAIL_ALREADY_EXISTS`; không bên nào nhận 500 |
| TC-AUTH-28 | **Body JSON hỏng** (ví dụ `{"email": "a@b.c", "pass` cắt ngang) gửi tới `POST /auth/login` | 400 `VALIDATION_ERROR` — `HttpMessageNotReadableException` không được thành 500 |

### 8.3. Unit test cases

| ID | Case | Kỳ vọng |
|---|---|---|
| TC-AUTH-UT-01 | `JwtService.generateAccessToken` | Claim đủ `sub`/`jti`/`sid`/`role`/`type`; `exp` ~ 15 phút |
| TC-AUTH-UT-02 | `JwtService` parse: (a) sai secret → `TOKEN_INVALID`; (b) token hợp lệ chữ ký nhưng `exp` quá khứ **10 giây** (trong window clock skew 60s của validator mặc định) → decoder với validator không timestamp vẫn decode được, filter/service tự kiểm `exp` và ánh xạ `TOKEN_EXPIRED`; (c) `type=refresh` nhưng gọi hàm parse access → `TOKEN_INVALID` | Ánh xạ đúng 3 nhánh (D-1-15) |
| TC-AUTH-UT-03 | Secret < 32 byte khi khởi động | ApplicationContext fail-fast |
| TC-AUTH-UT-04 | `AuthService.refresh` rotation với fake `RefreshTokenStore` (in-memory, không Redis — đúng mục đích O-1-05 đã chốt): (a) rotate thành công → refresh mới, jti cũ bị xóa/jti mới ghi, session đúng 1 token; (b) dùng lại → `TOKEN_REVOKED` + sid bị blacklist + session xóa | Rotation và nhánh thu hồi đúng (2.3); Mockito mock `UserRepository` |

### 8.4. Phân loại

- Unit: nhanh, chạy trong mọi CI job.
- Integration: full context + Testcontainers; tăng thời gian CI (~1–2 phút) nhưng bắt được lỗi cấu hình Redis/Security thật.

---

## 9. Definition of Done — Phase 1

> **Tick ngày 2026-10-20 theo chỉ thị của chủ dự án** — bằng chứng: `docs/progress.md` (backend `./mvnw clean verify` 39/39 PASS, frontend 6/6 PASS, E2E UI do chủ dự án tự test: đăng ký/đăng nhập/refresh…, CI hai job xanh sau push `b84f036` do chủ dự án xác nhận trên GitHub Actions).

### 9.1. Chức năng backend

- [x] Register/login/refresh/logout/`me` hoạt động đúng contract ở mục 4.
- [x] Rotation refresh token nguyên tử (`GETDEL`); dùng lại token cũ thu hồi toàn phiên; token sai `type` bị từ chối không revoke.
- [x] Logout có hiệu lực ngay: access token cũ bị từ chối sau logout; `/refresh` sau logout trả 401.
- [x] Bearer hết hạn gửi kèm `/auth/login`, `/auth/register`, `/auth/refresh` không bị filter chặn.
- [x] Email trùng (kể cả hai register song song) trả 409, không 500.
- [x] `GET /admin/stats` chỉ `ADMIN` truy cập được.
- [x] Admin seed đăng nhập được bằng credentials dev ghi trong README.
- [x] Refresh với user đã bị xóa hoặc `is_active = FALSE` → thu hồi phiên, 401 `TOKEN_REVOKED` (không để sliding session giữ phiên chết).
- [x] Body JSON hỏng, sai method, route không tồn tại trả 400/405/404 đúng mã lỗi — không lọt thành 500.

### 9.2. Bảo mật

- [x] Mật khẩu chỉ lưu BCrypt hash; không log mật khẩu/token/secret.
- [x] Access token sai, hết hạn, bị thu hồi trả đúng mã lỗi (`TOKEN_INVALID` / `TOKEN_EXPIRED` / `TOKEN_REVOKED`).
- [x] Đăng nhập sai mật khẩu, email không tồn tại và `is_active = FALSE` trả cùng `INVALID_CREDENTIALS`.
- [x] Mật khẩu > 72 byte UTF-8 bị từ chối 400 (không để `IllegalArgumentException` của BCrypt thành 500).
- [x] Không endpoint nào lọt khỏi rule bảo vệ ngoài danh sách `permitAll` (kiểm tra bằng test 401 cho các endpoint mới).
- [x] `JWT_SECRET` ngắn (< 32 byte) → ứng dụng không khởi động được (UT-03); thiếu `JWT_SECRET`/`ADMIN_PASSWORD_HASH` ở production (không nạp profile `local`/`test`) cũng fail-fast — jar không còn profile mặc định nhờ cấu hình `spring-boot:run` (đã chốt O-1-03 + A2(a)).

### 9.3. Kiểm thử

- [x] `(cd Backend && ./mvnw clean verify)` PASS toàn bộ (unit + integration).
- [x] Đủ 28 TC-AUTH integration + 4 UT ở mục 8 (hoặc ghi rõ case nào cut + lý do — không cắt im lặng).
- [x] Mỗi endpoint bảo vệ có cặp test được phép/bị từ chối.
- [x] CI hai job xanh sau push. *(Push `b84f036` 2026-10-20 — chủ dự án xác nhận hai job xanh trên GitHub Actions.)*

### 9.4. Frontend

- [x] Đăng ký/đăng nhập từ UI hoạt động với backend thật.
- [x] Tự refresh khi access token hết hạn (người dùng không bị đá ra chỉ vì hết hạn 15 phút); request `/auth/*` không bị gắn Bearer cũ.
- [x] Route cần đăng nhập và route admin bị chặn đúng ở client (ghi chú: backend vẫn là nơi quyết định thật).
- [x] `npm --prefix frontend test` xanh (FE-AUTH-01, FE-AUTH-02).

### 9.5. Tài liệu & cấu hình

- [x] `docs/error-codes.md` đủ 9 mã mới ở mục 4.1.
- [x] `.env.example` có `JWT_SECRET`, `ADMIN_PASSWORD_HASH`; giá trị mẫu chứa `$` được bọc single quote và có ghi chú về Docker Compose (`$$`) và `source .env` (xem 6.3).
- [x] README cập nhật: biến môi trường mới, tài khoản admin dev, các lệnh chạy.
- [x] Swagger hiển thị scheme Bearer và thử được các endpoint auth.
- [x] Không commit secret thật; mọi "secret" trong repo đều là dev default được đánh dấu.

---

## 10. Quyết định, Rủi ro & Câu hỏi

### 10.1. Quyết định đã chốt (tóm tắt)

| ID | Quyết định | Lý do chính |
|---|---|---|
| D-1-01 | Dùng JOSE của Spring Security (Nimbus) — module `org.springframework.security:spring-security-oauth2-jose`, filter tự viết; không thêm jjwt. **Đã chốt phương án A (O-1-01)** | Không dependency ngoài Spring; `spring-boot-starter-oauth2-jose` không tồn tại (đã kiểm tra 2026-10-20) |
| D-1-02 | Refresh token = JWT + hash SHA-256 trong Redis (3 key như mục 2.3) | Theo overview "lưu dạng hash trong Redis"; JWT cho phép verify hạn trước khi tra Redis |
| D-1-03 | Rotation nguyên tử bằng Redis `GETDEL`; kiểm `type=refresh` trước khi chạm Redis; kiểm blacklist `sid` trước và sau khi ghi token mới; query user theo `sub` trước khi cấp token (xóa/khóa → thu hồi phiên) (xem 2.3) | Tránh hai request refresh song song cùng consume một token; chặn access token nhầm vào `/refresh` gây revoke oan; chặn logout race làm "sống lại" phiên; chặn phiên chết của user bị xóa/khóa |
| D-1-04 | Phát hiện dùng lại → thu hồi toàn phiên (`sid`) | Overview bắt buộc; bảo vệ khi token bị đánh cắp |
| D-1-05 | Logout blacklist `sid` TTL 15 phút | Overview: hiệu lực ngay trong thời hạn access token |
| D-1-06 | `role` cột VARCHAR trên `users`, không bảng roles | 2 vai trò, 1-1; tránh over-engineering |
| D-1-07 | BCrypt strength 10 | Chuẩn Spring Security, đủ cho portfolio |
| D-1-08 | Seed admin qua Flyway placeholder `${admin_password_hash}` — base khai không default, `local`/`test` khai default hash dev; chỉ có tác dụng lần migrate đầu trên DB trống (**đã chốt O-1-03**) | Overview: "seed qua migration/biến môi trường"; tách secret khỏi SQL |
| D-1-09 | JWT secret: base `application.yml` không có default (thiếu env → fail-fast); default dev chỉ ở profile `local`/`test`; jar production không có profile mặc định (đã chốt O-1-03 + A2(a)) | Dev chạy ngay không cần export biến, nhưng deploy quên set env không làm lộ secret/admin public |
| D-1-10 | Token trong `localStorage` | Đơn giản cho portfolio; Axios header model; ghi nhận rủi ro XSS |
| D-1-11 | Multi-session (multi-device) cho phép | Mỗi login 1 `sid`; không cần logic "1 phiên/device" |
| D-1-12 | `User` entity ở package `user`, logic xác thực ở `auth` | Theo cấu trúc overview 4.1; tách data domain khỏi flow auth |
| D-1-13 | Admin endpoint mẫu `GET /api/v1/admin/stats` | DoD yêu cầu chứng minh RBAC; giữ tối giản |
| D-1-14 | `JwtAuthenticationFilter` không ghi response; token hỏng → không authenticate + lưu mã lỗi vào request attribute; `AuthenticationEntryPoint` đọc attribute | Bearer hết hạn gửi kèm endpoint công khai không được bị chặn (vòng lặp refresh) |
| D-1-15 | Phân biệt `TOKEN_EXPIRED`/`TOKEN_INVALID`: decoder với validator không kiểm timestamp, filter tự kiểm `exp` sau khi verify chữ ký; không soi chuỗi mô tả lỗi exception | Chuỗi mô tả lỗi của Nimbus không phải API cam kết (dễ vỡ); validator mặc định có clock skew 60 giây |
| D-1-16 | Validate mật khẩu theo byte UTF-8 (8–72 byte) trước khi BCrypt | BCrypt giới hạn theo byte; `encode` > 72 byte ném `IllegalArgumentException` (đã xác minh source Security 7) — phải chặn bằng 400, không để 500 |

### 10.2. Rủi ro đã xác định

| ID | Rủi ro | Mức | Biện pháp giảm thiểu |
|---|---|---|---|
| R-1-01 | API Spring Security 7 khác Security 6 (AI hay sinh sai) | Trung bình | Cấu hình đúng pattern Boot 4; build + 28 integration test bắt lỗi |
| R-1-02 | Redis down → mọi request authenticated fail (fail-closed) | Thấp (dev) | Chấp nhận; Redis là dependency có chủ đích (overview) |
| R-1-03 | XSS đọc token từ `localStorage` | Trung bình (nếu deploy public) | Ghi nhận; không render HTML thô; deployment thật cân nhắc chuyển httpOnly cookie (phase 6) |
| R-1-04 | Brute-force login không rate-limit | Trung bình | Ghi nhận ngoài phạm vi; portfolio chạy local |
| R-1-05 | Admin dev password public trong repo | Thấp | Default chỉ ở profile `local` (đã chốt O-1-03); README cảnh báo đổi khi deploy; Phase 6 đặt cứng `SPRING_PROFILES_ACTIVE` khi deploy |
| R-1-06 | Clock skew khiến `exp` lệch giữa các instance | Thấp | Chỉ 1 instance; kiểm `exp` thủ công không dùng skew (chấp nhận lệch tối đa vài giây giữa client/server) |
| R-1-07 | Nhiều tab cùng refresh một token → request thứ hai bị coi là "dùng lại" → thu hồi cả phiên (false positive) | Trung bình (UX) | Giảm thiểu phía FE bằng single-flight liên tab (`navigator.locks` + đọc lại `localStorage` sau khi giành khóa — **đã chốt O-1-02(a)**, xem 7.1); **không** chọn phương án nới lỏng server (grace window không revoke) vì làm yếu cơ chế phát hiện đánh cắp token |

### 10.3. Quyết định đã xác nhận (chốt ngày 2026-10-20)

| ID | Nội dung | Phương án đã chốt |
|---|---|---|
| O-1-01 | Dependency JWT | **A** — `org.springframework.security:spring-security-oauth2-jose` + filter tự viết (mục 6.2) |
| O-1-02 | Chống refresh đa tab | **(a)** — `navigator.locks` + đọc lại `localStorage` sau khi giành khóa; grace window server-side bị loại (mục 7.1) |
| O-1-03 | Default dev secret/hash | **Có** — chỉ ở profile `local`/`test`; base thiếu env → fail-fast |
| O-1-04 | Hạn tuyệt đối cho session | **Không** — giữ sliding session thuần ở Phase 1 |
| O-1-05 | Tách interface `RefreshTokenStore` | **Có** — interface nhỏ trong package `auth`, implementation Redis; unit test rotation với fake |
| Q-1-01 | A2 — xử lý mâu thuẫn D-1-09 với `spring.profiles.default: local` | **(a)** — gỡ `spring.profiles.default` khỏi `application.yml`, cấu hình `<profiles><profile>local</profile></profiles>` trong `spring-boot-maven-plugin` (chỉ `spring-boot:run` bật local); jar production không có profile mặc định → fail-fast thật |
| Q-1-02 | A5 — refresh khi user đã bị xóa / `is_active = FALSE` | Thu hồi phiên + 401 `TOKEN_REVOKED`; `role` cho token mới lấy từ DB (2.3 bước 6) |
| Q-1-03 | A5 — hash refresh token không khớp (2.3 bước 4) | 401 `TOKEN_INVALID`, **không** thu hồi phiên |
| Q-1-04 | A6 — mã lỗi cho 404/405 | Thêm `NOT_FOUND` (404) + `METHOD_NOT_ALLOWED` (405) vào `error-codes.md` (7 → 9 mã mới) |

### 10.4. Câu hỏi còn mở

Không còn câu hỏi nào mở — toàn bộ O-1-01 → O-1-05 đã được chủ dự án chốt theo đề xuất mặc định ngày 2026-10-20 và chuyển vào 10.3; các điểm phát sinh từ review (A1–A6, B1–B6) chốt cùng ngày (Q-1-01 → Q-1-04).

---

## 11. Thứ tự Triển khai Đề xuất

1. **Dependency:** thêm `org.springframework.security:spring-security-oauth2-jose` vào `pom.xml` (đã chốt O-1-01 → phương án A).
2. **Cấu hình:** `application.yml` (không default: `${JWT_SECRET}` + `spring.flyway.placeholders.admin_password_hash` ở base), `application-local.yml` (default dev secret + placeholder admin hash), `application-test.yml` (**bắt buộc** placeholder admin hash — cùng hash `Admin@123`), `.env.example` (ghi chú `$`/single quote/`$$`).
3. **Migration:** `V2__create_users.sql` (kèm CHECK lowercase, **không** tạo index `role`), `V3__seed_admin.sql`; sinh hash bcrypt của `Admin@123` và điền vào `application-local.yml` + `application-test.yml`.
4. **Security core:** `Role` enum, `JwtService` (decoder với validator không timestamp — D-1-15), `JwtAuthenticationFilter` (không ghi response, attribute — D-1-14; đăng ký đúng một lần), `AuthenticationEntryPoint` (đọc attribute), `AccessDeniedHandler`, cập nhật `SecurityConfig`, mở rộng `GlobalExceptionHandler` (`VALIDATION_ERROR`, `EMAIL_ALREADY_EXISTS` qua `DataIntegrityViolationException`).
5. **Domain user:** `User` entity, `UserRepository`.
6. **Auth module:** DTO (validate mật khẩu theo byte — D-1-16, cả login), `RefreshTokenStore` (interface đã chốt O-1-05), `AuthService` (register/login/refresh/logout/me — đầy đủ bước trong 2.3, dummy hash chống dò thời gian), `AuthController`.
7. **Admin module:** `AdminController` + stats.
8. **Error codes:** cập nhật `docs/error-codes.md` (9 mã).
9. **Backend tests:** unit (4 UT) + 28 integration test; `./mvnw clean verify` xanh.
10. **Frontend:** `react-router-dom`, `AuthContext`, interceptor Axios (không gắn Bearer cho `/auth/*`, không retry `/auth/refresh`, không coi `INVALID_CREDENTIALS` của login là hết phiên; single-flight `navigator.locks` đã chốt O-1-02), trang login/register, route bảo vệ; FE-AUTH-01/02.
11. **Tài liệu:** README (env mới, admin dev, ghi chú `$` trong `.env`), Swagger security scheme.
12. **Commit + push** (hỏi chủ dự án trước) → CI xanh → cập nhật `docs/progress.md` + báo cáo AGENTS mục 3.

---

*Tài liệu soạn theo Phase 0; cập nhật rà soát kỹ thuật ngày 2026-10-20 (sửa dependency JWT sai, thiết kế lại filter/refresh, mật khẩu theo byte, bổ sung test) và phản hồi review chủ dự án cùng ngày (A1 placeholder admin ở cả test, A2(a) gỡ `spring.profiles.default`, A3 assertion TC-AUTH-20, A4 `navigator.locks` đọc lại localStorage, A5 refresh nạp user + principal có `sid`, A6 handler JSON hỏng/404/405, B1–B6). Toàn bộ mục 10.4 được chốt theo đề xuất mặc định và chuyển vào 10.3 (kèm Q-1-01 → Q-1-04). Trạng thái: **Đã duyệt 2026-10-20** — sẵn sàng triển khai theo mục 11.*
