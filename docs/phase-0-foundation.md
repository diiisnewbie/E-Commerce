# Phase 0 — Khởi tạo (Foundation)

> **Trạng thái:** Done — chủ dự án xác nhận 2026-10-19 (smoke test + CI xanh; xem `docs/progress.md`).  
> **Cập nhật:** 2026-10-20 — rà soát kỹ thuật sau Done (A1–A4, B5–B6, C7–C9), đối chiếu với code đã commit `17f8fc4`.  
> **Phụ thuộc:** Không có phase tiên quyết.  
> **Tài liệu liên quan:** [`docs/project-overview.md`](./project-overview.md), [`AGENTS.md`](../AGENTS.md)

---

## 1. Mục tiêu & Phạm vi

### 1.1. Mục tiêu

Thiết lập nền tảng kỹ thuật cho toàn bộ dự án: cấu trúc thư mục, cấu hình môi trường, pipeline CI và health endpoint. Sau khi Phase 0 hoàn thành, mọi phase tiếp theo có thể bắt đầu ngay mà không cần thay đổi hạ tầng.

### 1.2. Trong phạm vi Phase 0

| Hạng mục | Chi tiết |
|---|---|
| Cấu trúc repository | Chuẩn hóa thư mục `Backend/`, `frontend/`, `docs/`, root files (giữ tên `Backend/` theo D-0-07) |
| Backend skeleton | Spring Boot application có thể khởi động, kết nối PostgreSQL, chạy Flyway |
| Docker Compose | Khởi động PostgreSQL 16 và Redis 7 cho môi trường phát triển |
| Flyway baseline | Migration V1 chạy thành công; Flyway ghi kết quả vào bảng `flyway_schema_history`; không tạo bảng nghiệp vụ |
| Health endpoint | `GET /api/v1/health` trả đúng response contract |
| Testcontainers | Cấu hình integration test với PostgreSQL và Redis thật (không dùng H2) |
| CI GitHub Actions | Hai job riêng: backend (build + test), frontend (build + Vitest) |
| Frontend skeleton | React + Vite khởi động, gọi được health endpoint (xác nhận CORS), có Vitest smoke test |
| Tài liệu | README ban đầu, `.env.example`, `docs/error-codes.md` (skeleton) |

### 1.3. Ngoài phạm vi Phase 0

- Bất kỳ logic nghiệp vụ nào (auth, product, order, v.v.).
- Bảo mật Spring Security đầy đủ (chỉ cấu hình permit-all tạm thời cho endpoint public của Phase 0: health, actuator health, swagger — xem D-0-06).
- Schema bảng nghiệp vụ (users, products, v.v.) — thuộc Phase 1+.
- Tối ưu hiệu năng, responsive, animation frontend.

---

## 2. Quyết định Tech Stack

### 2.1. Phiên bản được chốt

| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| Java | **21** (LTS) | Bắt buộc; dùng Java record, sealed class và virtual thread nếu cần |
| Spring Boot | **4.1.x** (đã cài **4.1.1**) | 4.1.0 GA **10/06/2026**, hỗ trợ OSS đến **31/07/2027**; 4.0 hết OSS 31/12/2026 — xem mục 2.2 và `project-overview.md` mục 3.1 |
| Maven Wrapper | `mvnw` kèm theo Spring Initializr | Không yêu cầu cài Maven toàn cục |
| PostgreSQL | **16** (image `postgres:16-alpine`) | `alpine` nhẹ hơn, đủ cho dev |
| Redis | **7** (image `redis:7-alpine`) | `alpine` nhẹ hơn, đủ cho dev |
| Flyway | Theo Spring Boot BOM | Boot 4 bắt buộc `spring-boot-starter-flyway` + `flyway-database-postgresql`; **không** dùng `flyway-core` trần (xem mục 2.4) |
| Testcontainers | Theo BOM của Spring Boot 4.1 (đã cài artifact **2.x**: `testcontainers-postgresql`, `testcontainers-junit-jupiter`) | Testcontainers 2.x đổi tên artifact (bản 1.x đóng băng ở 1.21.x); không pin version — BOM của Boot quản lý (xem mục 2.4) |
| Node.js | **22 LTS** | Node 20 EOL 30/04/2026; overview quy định "20+" nên 22 LTS vẫn hợp lệ (D-0-08) |
| React | **19.x** (đã cài 19.3.1) | Sinh bởi `npm create vite` bản mới nhất; overview quy định "18+" nên 19 vẫn hợp lệ (D-0-08) |
| Vite | **bản mới nhất** khi scaffold (đã cài 8.3.4) | Không hạ về 5.x; giữ nguyên bản do create-vite sinh để tương thích Node 22 (D-0-08) |
| Vitest | Bản tương thích Vite đã chọn (đã cài 5.0.5) | Test runner frontend, thay cho việc để lệnh `npm --prefix frontend test` của AGENTS.md chết (D-0-09) |
| Ant Design | **6.x** (đã cài 6.6.6; `@ant-design/icons` 6.3.4) | UI library chốt ở mục 2.3; antd 6 là nhánh dành cho React 19 (D-0-04) |
| Lombok, devtools | Theo Spring Boot BOM | Có sẵn từ skeleton, giữ nguyên (D-0-11) |
| springdoc-openapi | **3.x** (đã cài 3.1.1) | Đã xác nhận tương thích Boot 4 — Swagger UI load OK 2026-10-19 (Q-0-09); giữ nhánh dự phòng R-0-07 nếu cần |

### 2.2. Quyết định Spring Boot 4.1.x — Đã chốt

**Quyết định:** Dùng **Spring Boot 4.1.x** (nhánh 4.1, bản mới nhất khi khởi tạo project trên start.spring.io).

**Lý do** (chi tiết đầy đủ tại `project-overview.md` mục 3.1):
- Spring Boot 3.x (kể cả 3.5) hết hỗ trợ mã nguồn mở từ **30/06/2026** — không còn bản vá bảo mật miễn phí.
- Dự án khởi tạo tháng 10/2026 → nhánh 4.1.x là lựa chọn duy nhất còn được hỗ trợ OSS. **4.1.0 GA 10/06/2026, hạn OSS 31/07/2027** (nguồn: spring.io blog 10/06/2026 và endoflife.date; lưu ý 31/12/2026 là hạn OSS của **4.0**, không phải 4.1 — đợt release dự kiến tháng 5/2026 bị lùi sang tháng 6 do CVE). Đủ để hoàn thành dự án portfolio.
- Spring Boot 4 chạy trên Spring Framework 7 và Spring Security 7, yêu cầu Java 17+ (dự án dùng Java 21).

**Lưu ý quan trọng khi code với Spring Boot 4:** Xem `AGENTS.md` mục 5 để tránh các lỗi thường gặp khi AI sinh code theo kiểu Spring Boot 3 (tên starter thay đổi, package Jackson 3, Security 7, v.v.).

### 2.3. UI Library Frontend — Ant Design 6.x

**Lý do chọn Ant Design:**
- Bộ component đầy đủ (Table, Form, Modal, Layout) đáp ứng toàn bộ nhu cầu frontend của dự án.
- Thiết kế nhất quán, không cần viết CSS tùy biến.
- Tài liệu tiếng Anh/Trung phong phú, cộng đồng lớn.
- Phù hợp với mục 4.7 (dùng UI library, hạn chế CSS tay).

**Trade-off được chấp nhận:** Bundle size lớn hơn Material UI (~2MB gzip). Chấp nhận vì đây là portfolio, không phải sản phẩm production tối ưu.

**Cấu hình:** Dùng tree-shaking mặc định của Vite + `@ant-design/icons` riêng (chỉ import icon cần dùng).

**Tương thích React 19:** Dùng **antd 6.x** — nhánh hiện hành sinh cho React 19 (`package.json` đã cài antd 6.6.6 + `@ant-design/icons` 6.3.4 với React 19.3.1, 2026-10-19). antd 5.x là nhánh thế hệ trước, không chọn cho project mới. Ghi rõ phiên bản thực tế vào mục 2.1 (O-0-03).

### 2.4. Dependency cho `pom.xml` (đã đối chiếu Spring Boot 4.0 Migration Guide)

**Giữ nguyên 3 test starter đang có trong `pom.xml` — chúng hoàn toàn tồn tại và là đúng chuẩn Boot 4:**

```xml
<!-- ĐÃ CÓ trong pom.xml skeleton — GIỮ, KHÔNG XÓA -->
<artifactId>spring-boot-starter-webmvc-test</artifactId>       <!-- @AutoConfigureMockMvc, @WebMvcTest -->
<artifactId>spring-boot-starter-security-test</artifactId>     <!-- spring-security-test + auto-config -->
<artifactId>spring-boot-starter-validation-test</artifactId>
```

> **Sửa giả định sai của bản doc trước:** tài liệu từng ghi 3 artifact trên "không tồn tại" và yêu cầu xóa — **sai**. Cả 3 đều là starter test chuẩn của Spring Boot 4 (xem `AGENTS.md` mục 5 và Spring Boot 4.0 Migration Guide — mục Test Code). Xóa chúng sẽ mất MockMvc/security test support.

**Cần BỔ SUNG vào `pom.xml`:**

```xml
<!-- Persistence & Cache -->
spring-boot-starter-data-jpa
spring-boot-starter-data-redis

<!-- DB Migration — Boot 4 KHÔNG auto-config qua flyway-core trần -->
spring-boot-starter-flyway
org.flywaydb:flyway-database-postgresql      <!-- module dialect, version theo BOM -->

<!-- Monitoring (quyết định Q-0-02: bật Actuator, chỉ expose health) -->
spring-boot-starter-actuator

<!-- API Docs — đã xác nhận tương thích Boot 4 (Q-0-09) -->
springdoc-openapi-starter-webmvc-ui (đã cài 3.1.1)

<!-- Testing -->
org.springframework.boot:spring-boot-testcontainers (scope test)
org.springframework.boot:spring-boot-restclient (scope test)      <!-- Boot 4: support cho TestRestTemplate (xem 5.2.1) -->
org.testcontainers:testcontainers-postgresql (scope test)         <!-- Testcontainers 2.x: tên artifact mới -->
org.testcontainers:testcontainers-junit-jupiter (scope test)      <!-- không khai báo version — BOM của spring-boot-starter-parent quản lý -->
```

> **Vì sao không thêm `flyway-core`:** từ Boot 4, Flyway auto-configuration nằm ở `spring-boot-starter-flyway`. Chỉ khai báo `flyway-core` + `flyway-database-postgresql` → context khởi động bình thường nhưng **migration không chạy im lặng**. Test TC-HEALTH-03 là lớp chặn cuối phát hiện lỗi này.

> **Testcontainers 2.x đổi tên artifact (A4):** bản cũ `org.testcontainers:postgresql` / `org.testcontainers:junit-jupiter` bị đóng băng ở 1.21.x; với Boot 4.1 (Testcontainers 2.x) phải dùng `testcontainers-postgresql` / `testcontainers-junit-jupiter`. Package Java của container giữ nguyên `org.testcontainers.containers.*` — đã xác nhận `./mvnw test-compile` PASS (2026-10-20). Version do BOM của `spring-boot-starter-parent` quản lý, pom không pin (khác với ghi chú cũ yêu cầu import `testcontainers-bom` riêng — không cần).

> **Không cần thêm `spring-boot-starter-test` lõi:** mọi `spring-boot-starter-*-test` đều kéo nó theo transitive. Không thêm `data-jpa-test`/`flyway-test` ở Phase 0 vì chưa dùng slice annotation (`@DataJpaTest`, ...); các test Phase 0 đều chạy qua `@SpringBootTest`.

> **Lombok + devtools:** có sẵn trong skeleton, giữ nguyên (D-0-11). `ApiResponse` vẫn là `record` thuần, không bắt buộc Lombok.

---

## 3. Cấu trúc Repository & Thư mục

### 3.1. Cấu trúc thư mục gốc (target state sau Phase 0)

```
ecommerce-platform/               <- thư mục gốc repo
├── AGENTS.md                     # quy tắc làm việc với AI
├── README.md                     # hướng dẫn chạy dự án
├── docker-compose.yml            # PostgreSQL 16, Redis 7
├── .env.example                  # biến môi trường mẫu, không chứa secret
├── .gitignore
├── .github/
│   └── workflows/
│       └── ci.yml                # GitHub Actions CI
├── docs/
│   ├── project-overview.md
│   ├── error-codes.md            # đã tạo ở Phase 0 — có INTERNAL_SERVER_ERROR
│   ├── phase-0-foundation.md     # file này
│   ├── phase-1-auth.md           # Đã duyệt 2026-10-20 — đang triển khai
│   └── progress.md               # file local theo dõi tiến độ — KHÔNG commit (AGENTS.md)
├── Backend/                      # GIỮ tên theo D-0-07 (đã push lên repo; không đổi case)
│   ├── pom.xml
│   ├── mvnw                       # phải có mode executable trong git index (xem R-0-06)
│   ├── mvnw.cmd
│   ├── .mvn/wrapper/
│   └── src/
│       ├── main/
│       │   ├── java/com/app/backend/
│       │   │   ├── BackendApplication.java
│       │   │   ├── common/
│       │   │   │   ├── config/        # WebConfig (CORS), OpenApiConfig
│       │   │   │   ├── exception/     # GlobalExceptionHandler
│       │   │   │   ├── response/      # ApiResponse<T> record
│       │   │   │   └── security/      # SecurityConfig (permit-all endpoint public + CORS — D-0-06)
│       │   │   └── health/
│       │   │       ├── dto/           # HealthResponse.java
│       │   │       └── controller/    # HealthController
│       │   └── resources/
│       │       ├── application.yml    # thay thế application.properties (xóa file cũ)
│       │       ├── application-local.yml
│       │       └── db/migration/
│       │           └── V1__baseline.sql
│       └── test/
│           ├── java/com/app/backend/
│           │   ├── BackendApplicationTests.java   # contextLoads
│           │   ├── common/                        # BaseIntegrationTest
│           │   └── health/
│           │       └── HealthIntegrationTest.java # TC-HEALTH-01…04, TC-CORS-01
│           └── resources/
│               └── application-test.yml
└── frontend/
    ├── package.json                # scripts: dev, build, lint (oxlint), test (vitest run)
    ├── package-lock.json
    ├── vite.config.js              # build + test config (jsdom, setup file: src/setupTests.js)
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx                 # hiển thị trạng thái health (UP/DOWN, service, timestamp)
        ├── App.test.jsx            # Vitest smoke test (D-0-09) — 3 test: UP/DOWN/error
        ├── setupTests.js           # jest-dom matchers
        ├── api.js                  # axios instance + getHealth() — dùng chung cho Phase 1
        ├── index.css
        └── assets/
```

### 3.2. Quyết định về tên thư mục `Backend/`

- `project-overview.md` mục 4.1 và `AGENTS.md` mục 4 từng viết `backend/` (lowercase); thư mục thực tế trong repo là `Backend/` và **đã được push lên remote**.
- **Quyết định (D-0-07):** **giữ nguyên `Backend/`.** Không đổi case.
  - **Lý do:** code đã push; đổi case trên Windows đòi hỏi 2 lần `git mv` + mọi tham chiếu trong tài liệu/lệnh/CI phải sửa theo, chi phí sửa đổi lớn hơn lợi ích cho một dự án chạy CI trên một OS duy nhất.
  - **Hệ quả phải chấp nhận:**
    1. GitHub Actions chạy trên Linux (case-sensitive) → `working-directory` trong `ci.yml` **bắt buộc** là `Backend`.
    2. Mọi lệnh trong README và `AGENTS.md` mục 4 phải dùng `Backend/` để không chạy nhầm.
    3. **Đã cập nhật** `project-overview.md` và `AGENTS.md` về tên `Backend/` cùng ngày duyệt tài liệu này (Q-0-08) — không còn sai lệch giữa tài liệu và thư mục thực tế.
- Trên Windows, `git mv` không đổi được trực tiếp tên khác hoa/thường (nếu sau này có yêu cầu): `git mv Backend backend_tmp && git mv backend_tmp backend`.

### 3.3. Package Java

Giữ nguyên `com.app.backend` theo skeleton hiện tại. Cấu trúc feature-based:

```
com.app.backend
├── BackendApplication.java
├── common
│   ├── config
│   │   ├── WebConfig.java          # CORS configuration
│   │   └── OpenApiConfig.java      # Swagger/OpenAPI metadata
│   ├── exception
│   │   └── GlobalExceptionHandler.java
│   ├── response
│   │   └── ApiResponse.java        # generic wrapper <T>
│   └── security
│       └── SecurityConfig.java     # permit-all endpoint public Phase 0 + `.cors(withDefaults())` (D-0-06)
└── health
    ├── dto
    │   └── HealthResponse.java
    └── controller
        └── HealthController.java
```

> **Bắt buộc có `SecurityConfig`:** `spring-boot-starter-security` đã trong `pom.xml` → mặc định Spring Security block mọi request. Không có filter chain permit-all thì `GET /api/v1/health` trả 401 và TC-HEALTH-02 FAIL. Theo D-0-06, chain phải **(a)** permit-all các endpoint public Phase 0: `/api/v1/health`, `/actuator/health`, `/swagger-ui.html`, `/swagger-ui/**`, `/api-docs/**`; **(b)** bật CORS của Spring Security bằng `.cors(Customizer.withDefaults())` để `WebConfig` (mục 8.2) có hiệu lực khi request đi qua security chain — thiếu dòng này, preflight OPTIONS có thể không trả CORS header và TC-CORS-01 FAIL. Code hiện tại: `SecurityConfig.java`.

---

## 4. API Contract — Health Endpoint

### 4.1. Tổng quan

| Thuộc tính | Giá trị |
|---|---|
| Method | `GET` |
| Path | `/api/v1/health` |
| Authentication | **Không yêu cầu** (public endpoint) |
| Content-Type response | `application/json` |

### 4.2. Business Rules

| ID | Quy tắc |
|---|---|
| BR-HEALTH-01 | Endpoint phải trả HTTP 200 khi ứng dụng khởi động thành công. |
| BR-HEALTH-02 | Response phải tuân thủ cấu trúc `ApiResponse<T>` chuẩn (mục 4.2 của `project-overview.md`). |
| BR-HEALTH-03 | `timestamp` phải là UTC ISO 8601, được sinh tại thời điểm xử lý request — không hardcode. |
| BR-HEALTH-04 | Endpoint không được trả thông tin nhạy cảm (version DB, secret, connection string). |
| BR-HEALTH-05 | Trong phạm vi Phase 0, endpoint chỉ xác nhận ứng dụng có phản hồi; không thực hiện deep health check DB/Redis. |

### 4.3. Response thành công (HTTP 200)

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "ecommerce-backend"
  },
  "message": "Service is healthy",
  "errorCode": null,
  "timestamp": "2026-01-01T10:00:00Z"
}
```

**Mô tả các trường trong `data`:**

| Trường | Kiểu | Mô tả |
|---|---|---|
| `status` | `String` | Giá trị cố định `"UP"` khi endpoint phản hồi được |
| `service` | `String` | Tên định danh của service; giá trị cố định `"ecommerce-backend"` |

> **Lý do thiết kế đơn giản:** Health endpoint trong Phase 0 chỉ xác nhận ứng dụng có khởi động không. Spring Boot Actuator (`/actuator/health`) có thể bổ sung sau để kiểm tra DB/Redis deep health, nhưng không thay thế endpoint này vì path, format và access control khác nhau.

### 4.4. Response lỗi server (HTTP 500)

Nếu exception chưa bắt được, `GlobalExceptionHandler` trả:

```json
{
  "success": false,
  "data": null,
  "message": "An unexpected error occurred",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "timestamp": "2026-01-01T10:00:00Z"
}
```

> `errorCode` `INTERNAL_SERVER_ERROR` phải được ghi vào `docs/error-codes.md` khi tạo file ở Phase 0 (overview mục 4.2: mọi mã lỗi phải nằm trong danh mục này).

### 4.5. Thiết kế lớp `ApiResponse<T>`

```java
// Gợi ý cấu trúc — triển khai chính xác thuộc bước code
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String errorCode,
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode, Instant.now());
    }
}
```

**Serialization:** `Instant` phải serialize thành chuỗi ISO 8601 UTC. Cần cấu hình Jackson:

```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC
```

---

## 5. Kế hoạch Kiểm thử (Testing Strategy)

### 5.1. Nguyên tắc

- **Không dùng H2** cho integration test. Lý do: H2 không hỗ trợ `SELECT ... FOR UPDATE SKIP LOCKED`, window functions và một số kiểu dữ liệu PostgreSQL. Hành vi lock/transaction sẽ khác với production.
- Dùng **Testcontainers** để spin up PostgreSQL 16 và Redis 7 thật trong mỗi test run.
- Integration test chạy trong profile `test` riêng; không ảnh hưởng `application.yml` của local dev.

### 5.2. Cấu hình Testcontainers

#### 5.2.1. Base test class (dùng chung)

```java
// Đã triển khai — khớp BaseIntegrationTest.java (commit 17f8fc4)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate   // Boot 4: @SpringBootTest KHÔNG tự cấu hình bean TestRestTemplate (xem ghi chú dưới)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        postgres.start();   // khởi động 1 lần cho mọi lớp test con trong cùng JVM (D-0-03)
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

**Lý do `static { start(); }` thay vì `@Testcontainers` + `@Container`:** hai cách đều khởi động container ở tầng static (D-0-03). Code hiện tại chọn `static {}` để container khởi động **một lần duy nhất cho mọi lớp test con** trong cùng JVM (extension `@Container` sẽ start/stop theo từng test class); Ryuk tự dọn container khi JVM thoát.

> **TestRestTemplate trên Spring Boot 4 (A2):** class nằm ở package **`org.springframework.boot.resttestclient.TestRestTemplate`** (không còn `org.springframework.boot.test.web.client` như Boot 3). `@SpringBootTest` ở Boot 4 **không còn tự cấu hình bean** `TestRestTemplate` → bắt buộc thêm **`@AutoConfigureTestRestTemplate`** (`org.springframework.boot.resttestclient.autoconfigure`) và dependency test `spring-boot-restclient` (đã có trong `pom.xml`; class `resttestclient` resolve qua transitive — `./mvnw test-compile` PASS 2026-10-20). Lưu ý hướng di chuyển: Boot 4 giới thiệu `RestTestClient` (`@AutoConfigureRestTestClient`) để thay `TestRestTemplate` trong các bản sau — nếu cần ổn định dài hạn có thể chuyển sang MockMvc/RestTestClient; giai đoạn này giữ `TestRestTemplate` để ít thay đổi test.

> **Redis password lệch default (A1):** `application.yml` có default `changeme_redis` (D-0-10) nhưng container Redis trong test **không bật password** → gộp cấu hình sẽ lỗi kết nối. Cách dùng hiện tại: profile `test` đặt `spring.data.redis.password` rỗng trong `application-test.yml` (xem 5.2.2). Cách thay thế tương đương: `registry.add("spring.data.redis.password", () -> "")` trong `@DynamicPropertySource`.

**Reuse giữa các test class:** Cân nhắc bật `testcontainers.reuse.enable=true` trong `~/.testcontainers.properties` cho môi trường local. CI không dùng tính năng này.

**Lý do `@ActiveProfiles("test")`:** Không có annotation này, `application-test.yml` không được nạp và test sẽ đọc cấu hình local (`application.yml` + profile `local`). Bắt buộc có trong `BaseIntegrationTest`.

#### 5.2.2. `application-test.yml`

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    # URL được ghi đè bởi @DynamicPropertySource
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway quản lý schema; Hibernate chỉ validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  data:
    redis:
      password:               # [A1] để TRỐNG có chủ đích — container Redis trong test không bật password
      # host và port được ghi đè bởi @DynamicPropertySource

logging:
  level:
    com.app.backend: DEBUG
    org.testcontainers: INFO
```

> **Vì sao phải set `password` rỗng (A1):** `application.yml` gốc đặt `${REDIS_PASSWORD:changeme_redis}` (D-0-10) — khớp docker-compose khi dev local, nhưng **lệch** với container Testcontainers (không có password) → kết nối Redis test bị từ chối. Profile `test` ghi đè bằng giá trị rỗng. Đã áp trong code (commit `17f8fc4`); cách thay thế: `registry.add("spring.data.redis.password", () -> "")` ở `BaseIntegrationTest` (xem 5.2.1).

### 5.3. Test cases Phase 0

#### TC-HEALTH-01: Health endpoint trả HTTP 200 và đúng format

**Loại:** Integration test | **Class:** `HealthIntegrationTest`

```
Given: Ứng dụng khởi động thành công, PostgreSQL và Redis container đang chạy
When:  GET /api/v1/health (không có Authorization header)
Then:
  - HTTP status = 200
  - Content-Type = application/json
  - response.success = true
  - response.data.status = "UP"
  - response.data.service = "ecommerce-backend"
  - response.errorCode = null
  - response.message không rỗng
  - response.timestamp là chuỗi ISO 8601 UTC hợp lệ (không null, không blank)
  - response.timestamp không xa thời điểm gọi test quá 5 giây
```

#### TC-HEALTH-02: Health endpoint không yêu cầu authentication

```
Given: Không có Authorization header hoặc token không hợp lệ
When:  GET /api/v1/health
Then:  HTTP status = 200 (không phải 401 hay 403)
```

> **Lý do:** Spring Security mặc định block tất cả endpoint. Phase 0 phải cấu hình permit-all cho `/api/v1/health`. Test này xác nhận cấu hình Security đúng.

#### TC-HEALTH-03: Flyway migration chạy thành công khi khởi động

```
Given: PostgreSQL container khởi động từ image sạch
When:  Spring Boot application context load
Then:
  - Application context load thành công (không throw exception)
  - Bảng flyway_schema_history tồn tại trong database
  - Migration V1__baseline.sql đã được áp dụng (status = SUCCESS)
```

#### TC-HEALTH-04: Ứng dụng kết nối được Redis

```
Given: Redis container đang chạy
When:  Spring Boot application context load
Then:  Application context load thành công
       (RedisConnectionFactory bean khởi tạo không lỗi,
        ping() trả về PONG)
```

#### TC-CORS-01: CORS headers cho frontend origin

```
Given: Request từ origin http://localhost:5173 (Vite dev server)
When:  OPTIONS /api/v1/health (preflight) với Origin header
Then:
  - Response có header Access-Control-Allow-Origin: http://localhost:5173
  - Access-Control-Allow-Methods chứa GET, POST, PUT, DELETE
```

### 5.4. Phân loại và thời gian test

| Test | Loại | Tool | Thời gian ước tính |
|---|---|---|---|
| TC-HEALTH-01 | Integration | SpringBootTest + Testcontainers + TestRestTemplate (`@AutoConfigureTestRestTemplate`) | ~15–30s (cold start) |
| TC-HEALTH-02 | Integration | SpringBootTest + TestRestTemplate | ~1s (reuse context) |
| TC-HEALTH-03 | Integration | SpringBootTest + JdbcTemplate assertion | ~1s (reuse context) |
| TC-HEALTH-04 | Integration | SpringBootTest + RedisConnectionFactory (`ping()`) | ~1s (reuse context) |
| TC-CORS-01 | Integration | SpringBootTest + TestRestTemplate (preflight OPTIONS) | ~1s (reuse context) |

**Lưu ý:** Unit test với Mockito sẽ xuất hiện từ Phase 1 khi có service logic đáng test. Phase 0 không có service logic, integration test là phù hợp. Cả 5 test nằm trong `HealthIntegrationTest` (khớp code 2026-10-20) — riêng TestRestTemplate bắt buộc `@AutoConfigureTestRestTemplate` trên Boot 4 (xem 5.2.1).

### 5.5. Frontend test (D-0-09)

#### FE-SMOKE-01: App render và hiển thị trạng thái health

**Loại:** Vitest (jsdom) | **File:** `src/App.test.jsx`

```
Given: Mock module gọi API (axios/vitest mock) trả về response health hợp lệ
When:  Render <App />
Then:
  - Component render không throw
  - Hiển thị trạng thái "UP" và tên service từ response
  - Gọi GET /api/v1/health đúng một lần
```

- Chạy bằng `npm --prefix frontend test` (script `"test": "vitest run"`) — đúng lệnh có sẵn trong `AGENTS.md` mục 4.
- Triển khai hiện tại: `App.test.jsx` gồm 3 test (UP / DOWN / lỗi mạng) — `npm --prefix frontend test` PASS 3/3 (2026-10-20).
- CI job `frontend` chạy script này sau khi build.

---

## 6. Cấu hình Hạ tầng

### 6.1. `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: ecommerce_postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-ecommerce}
      POSTGRES_USER: ${POSTGRES_USER:-ecommerce_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-changeme}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-ecommerce_user} -d ${POSTGRES_DB:-ecommerce}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  redis:
    image: redis:7-alpine
    container_name: ecommerce_redis
    command: ["redis-server", "--requirepass", "${REDIS_PASSWORD:-changeme_redis}"]
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-changeme_redis}", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

**Lý do có `healthcheck`:** Đảm bảo services thực sự sẵn sàng nhận kết nối sau `docker compose up -d`.

### 6.2. `.env.example`

```dotenv
# Database — khớp default của docker-compose.yml và application.yml (D-0-10)
POSTGRES_DB=ecommerce
POSTGRES_USER=ecommerce_user
POSTGRES_PASSWORD=changeme

# Redis — khớp default của docker-compose.yml và application.yml (D-0-10)
REDIS_PASSWORD=changeme_redis

# Backend
SPRING_PROFILES_ACTIVE=local
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ecommerce
DB_USER=ecommerce_user
DB_PASSWORD=changeme
REDIS_HOST=localhost
REDIS_PORT=6379
```

> **Quy tắc (D-0-10 — default dev đồng bộ 3 nơi):** `docker-compose.yml`, `application.yml` và `.env.example` dùng **cùng một** default dev cho DB/Redis password, để `docker compose up -d` + `./mvnw spring-boot:run` chạy được ngay không cần export biến. `.env.example` được commit (chỉ chứa default dev đã công khai, không phải secret thật); file `.env` **không được commit** (đã có trong `.gitignore`). Trước khi deploy thật, thay default bằng secret riêng qua biến môi trường.

> **Cách `.env` được nạp (rà soát 2026-10-20 — C7):** Spring Boot **không** tự đọc file `.env`; file này chỉ được **docker-compose** nạp để interpolate `POSTGRES_*`/`REDIS_*` khi `docker compose up`. Hệ quả:
> - `SPRING_PROFILES_ACTIVE=local` trong `.env` **không** kích hoạt profile local cho `./mvnw spring-boot:run` — muốn bật, phải export vào env của process (IDE run config / shell). Cấu hình hiện tại: profile `local` đặt thẳng trong `spring-boot-maven-plugin` nên `spring-boot:run` tự bật (xem 6.3).
> - `DB_*`/`REDIS_*` trong `.env` cũng chỉ có tác dụng nếu được export vào env trước khi chạy Spring; mặc định không cần (D-0-10 đã đồng bộ default trong `application.yml`).
> - Integration test không phụ thuộc `.env` — mọi biến test do `application-test.yml` + `@DynamicPropertySource` cung cấp (5.2).

### 6.3. `application.yml`

```yaml
spring:
  application:
    name: ecommerce-backend
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:ecommerce}
    username: ${DB_USER:ecommerce_user}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:changeme_redis}   # default dev đồng bộ với docker-compose (D-0-10)
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC

server:
  port: 8080

# Spring Boot Actuator — chỉ expose /actuator/health (không expose info, metrics, ...)
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never   # không lộ chi tiết DB/Redis connection ra ngoài

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

**Lý do `open-in-view: false`:** Tắt Open Session In View — tránh lazy loading ngầm trong Controller, buộc nạp dữ liệu trong Service.

**Lý do `ddl-auto: validate`:** Flyway quản lý schema; Hibernate chỉ kiểm tra schema khớp entity khi khởi động. Tránh Hibernate tự sửa schema.

**Lý do `show-details: never` cho Actuator:** `/actuator/health` endpoint là public (không auth ở Phase 0); trả `{"status":"UP"}` là đủ. Không để lộ tên DB, Redis host hay stack trace qua Actuator response.

> **Cấu hình profile local (C7 — cập nhật 2026-10-20 theo chốt Q-0-12 / A2(a) của Phase 1):**
>
> Ban đầu (Q-0-11) đã thêm `spring.profiles.default: local` vào `application.yml`, nhưng dòng này bật luôn default dev khi deploy quên `SPRING_PROFILES_ACTIVE` — xung đột với fail-fast của Phase 1 (D-1-09). **Cấu hình hiện tại:** gỡ khỏi `application.yml`, đặt profile `local` trong `spring-boot-maven-plugin`:
>
> ```xml
> <plugin>
>     <groupId>org.springframework.boot</groupId>
>     <artifactId>spring-boot-maven-plugin</artifactId>
>     <configuration>
>         <profiles>
>             <profile>local</profile>
>         </profiles>
>     </configuration>
> </plugin>
> ```
>
> - Parameter `profiles` thuộc **goal `spring-boot:run`** (docs.spring.io/spring-boot/maven-plugin/run.html — xác nhận trên bản 4.1.1; user property `spring-boot.run.profiles`): `./mvnw spring-boot:run` tự bật `local` → `application-local.yml` (DEBUG logging, default dev) có hiệu lực không cần export env.
> - **Chỉ `spring-boot:run` bị ảnh hưởng** — `java -jar` (production) không có profile mặc định → thiếu env là fail-fast thật (điều kiện của Phase 1); chạy jar ở local cần `-Dspring.profiles.active=local`; IDE chọn profile trong run configuration.
> - Integration test không bị ảnh hưởng: surefire không qua goal `run`; `@ActiveProfiles("test")` đặt profile active.
> - Deploy thật: set `SPRING_PROFILES_ACTIVE` qua env (Phase 6 đặt cứng); kể cả quên, jar cũng **không** rơi vào profile `local` nữa.
> - **Xác nhận runtime 2026-10-20 (thời điểm còn `spring.profiles.default`):** log ghi `No active profile set, falling back to 1 default profile: "local"` + DEBUG `com.app.backend.*`; `/api/v1/health`, `/actuator/health`, `/swagger-ui.html` = 200. **Verify lại cùng ngày sau khi chuyển sang cấu hình plugin:** `./mvnw spring-boot:run` ghi `The following 1 profile is active: "local"` (không cần `spring.profiles.default`), app khởi động Tomcat 8080 OK.

### 6.4. Flyway Migration V1

**File:** `Backend/src/main/resources/db/migration/V1__baseline.sql`

```sql
-- Phase 0 baseline migration.
-- Xác nhận Flyway đang hoạt động đúng.
-- Không tạo bảng nghiệp vụ ở đây; bảng đầu tiên (users, roles) thuộc Phase 1.
SELECT 1;
```

> **Cảnh báo:** Sau khi migration này đã được áp dụng vào bất kỳ database nào, **tuyệt đối không sửa nội dung file `V1__baseline.sql`**. Flyway kiểm tra checksum và sẽ từ chối khởi động ứng dụng nếu checksum không khớp. Mọi thay đổi schema phải tạo file migration mới với số thứ tự tiếp theo.

---

## 7. CI — GitHub Actions

### 7.1. Cấu hình pipeline

**File:** `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: ["main", "develop"]
  pull_request:
    branches: ["main", "develop"]

jobs:
  backend:
    name: Backend — Build & Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        working-directory: Backend        # CHỮ HOA theo tên thư mục thật (D-0-07); Linux case-sensitive
        run: ./mvnw clean verify

  frontend:
    name: Frontend — Build & Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node 22
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Build
        working-directory: frontend
        run: npm run build

      - name: Test (Vitest)
        working-directory: frontend
        run: npm test
```

**Lý do chọn `temurin`:** Eclipse Temurin là OpenJDK build chính thức, miễn phí, ổn định, được GitHub Actions hỗ trợ tốt.

**Lý do cache Maven:** Tránh tải lại toàn bộ dependency (~200MB) mỗi lần chạy CI.

**Testcontainers trong CI:** GitHub Actions ubuntu-latest runner có Docker pre-installed. Testcontainers tự spin up container trong job `backend`. Không cần khai báo service riêng trong CI cho testing.

> **Bắt buộc trước khi push (R-0-06):** `Backend/mvnw` đang được commit ở mode `100644` (đã kiểm tra bằng `git ls-files -s`) và repo có `core.fileMode=false` → trên Linux runner, `./mvnw` sẽ fail `Permission denied`. Sửa một lần: `git update-index --chmod=+x Backend/mvnw` rồi commit cùng thay đổi Phase 0. (Chỉ áp sau khi file đã ở đúng nhánh muốn commit; lệnh này KHÔNG phá dữ liệu.)

### 7.2. Trigger strategy

- **push to main/develop:** Chạy CI đầy đủ. Đảm bảo nhánh chính luôn xanh.
- **pull_request to main/develop:** Chạy CI trước khi merge. Bắt lỗi sớm.

---

## 8. Frontend Skeleton

### 8.1. Yêu cầu tối thiểu Phase 0

- Node **22 LTS**, React **19**, Vite **bản mới nhất** do `npm create vite` sinh (D-0-08).
- Ứng dụng React + Vite khởi động tại `http://localhost:5173`.
- Gọi `GET /api/v1/health` khi trang tải.
- Hiển thị kết quả: status `UP`/`DOWN`, service name, timestamp.
- Xác nhận CORS hoạt động (không bị blocked bởi browser).
- Vitest chạy được với 1 smoke test FE-SMOKE-01 (D-0-09).

### 8.2. Cấu hình CORS backend

`WebConfig.java` phải cho phép origin `http://localhost:5173` (Vite dev server):

```java
// Gợi ý — triển khai chính xác thuộc bước code
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

> **Lưu ý:** Origin cụ thể (không dùng `*`) vì `allowCredentials(true)` yêu cầu origin không phải wildcard. Khi deploy production, thay bằng domain thật qua environment variable.

> **Bắt buộc kèm theo ở `SecurityConfig` (A3):** `WebConfig` chỉ có hiệu lực nếu `SecurityFilterChain` bật `.cors(Customizer.withDefaults())` — nếu không, request đi qua security chain sẽ không nhận CORS config của MVC và preflight OPTIONS có thể thiếu `Access-Control-Allow-*` (TC-CORS-01 FAIL). Đồng thời OPTIONS phải nằm trong permit-all. Code hiện tại đã đủ cả hai (`SecurityConfig.java` + `WebConfig.java`).

### 8.3. Thư viện cài đặt

```bash
npm install antd @ant-design/icons axios
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom
```

`package.json` phải có script:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "test": "vitest run"
  }
}
```

Cấu hình `test` (jsdom + setup file) đặt trong `vite.config.js` để dùng chung một config.

**Phiên bản thực tế đã cài** (khớp mục 2.1 — đối chiếu `package.json` 2026-10-19): `antd` 6.6.6, `@ant-design/icons` 6.3.4, `axios` 1.20.0, `react`/`react-dom` 19.3.1, `vite` 8.3.4, `vitest` 5.0.5, `@testing-library/react` 16.3.3, `jsdom` 29.1.1; lint bằng `oxlint` (script `lint`).

Không cài thêm thư viện state management (Redux, Zustand, Jotai) theo quy định mục 4.7 của `project-overview.md`.

---

## 9. Definition of Done — Phase 0

Checklist phải được kiểm tra đủ trước khi Phase 0 được chuyển sang `Done`. Chủ dự án xác nhận từng mục sau khi có bằng chứng kiểm tra phù hợp.

### 9.1. Hạ tầng & Môi trường

- [x] `docker compose up -d` khởi động thành công PostgreSQL 16 và Redis 7.
- [x] Cả hai service pass healthcheck (`docker compose ps` hiển thị `healthy`).
- [x] `.env.example` tồn tại ở thư mục gốc, không chứa secret thật.
- [x] `.env` (nếu có) đã được thêm vào `.gitignore`.

### 9.2. Backend

- [x] Thư mục giữ nguyên tên `Backend/` (D-0-07); mọi lệnh trong README và CI dùng đúng chữ hoa/thường.
- [x] `git ls-files -s Backend/mvnw` hiển thị mode `100755` (đã chạy `git update-index --chmod=+x Backend/mvnw` — R-0-06).
- [x] Spring Boot version trong `pom.xml` là **4.1.x** (đã chốt theo `project-overview.md` mục 3.1).
- [x] `pom.xml` có `spring-boot-starter-flyway` + `flyway-database-postgresql` (không chỉ `flyway-core`) và `spring-boot-starter-actuator`.
- [x] 3 test starter `webmvc-test` / `security-test` / `validation-test` **còn nguyên vẹn** (không bị xóa nhầm — chúng là starter đúng của Boot 4).
- [x] Backend khởi động thành công với `(cd Backend && ./mvnw spring-boot:run)` khi Docker services đang chạy.
- [x] Flyway migration V1 chạy thành công (log hiển thị `Successfully applied 1 migration`).
- [x] `GET /api/v1/health` trả HTTP 200 với JSON đúng format (kiểm tra bằng curl hoặc Swagger).
- [x] `GET /actuator/health` trả HTTP 200 và không lộ chi tiết (show-details: never — Q-0-02).
- [x] Swagger UI truy cập được tại `http://localhost:8080/swagger-ui.html` (springdoc 3.1.1 — đã xác nhận tương thích Boot 4, xem Q-0-09/R-0-07).
- [x] Không có secret hoặc password hardcode trong code hoặc file config được commit (default dev đã công khai ở `.env.example` được phép).

### 9.3. Kiểm thử

- [x] `(cd Backend && ./mvnw clean verify)` chạy thành công, không có test FAIL.
- [x] TC-HEALTH-01 PASS.
- [x] TC-HEALTH-02 PASS.
- [x] TC-HEALTH-03 PASS.
- [x] TC-HEALTH-04 PASS.
- [x] TC-CORS-01 PASS.
- [x] FE-SMOKE-01 PASS (`npm --prefix frontend test`).

### 9.4. CI

- [x] File `.github/workflows/ci.yml` tồn tại với hai job riêng.
- [x] Job `backend` chạy xanh trên GitHub Actions (bao gồm `./mvnw clean verify` với Testcontainers).
- [x] Job `frontend` chạy xanh trên GitHub Actions (build + `npm test`).
- [x] Không có secret trong CI logs.

### 9.5. Frontend

- [x] `npm --prefix frontend install` thành công.
- [x] `npm --prefix frontend run dev` khởi động tại `http://localhost:5173`.
- [x] Frontend gọi `GET /api/v1/health` và hiển thị kết quả trên UI.
- [x] Không có CORS error trong browser console.
- [x] `npm --prefix frontend test` chạy xanh (script `test` = `vitest run`).

### 9.6. Tài liệu

- [x] `README.md` ở thư mục gốc có hướng dẫn: yêu cầu cài đặt (Node 22, Java 21, Docker), chạy Docker services, chạy backend, chạy frontend, chạy test — lệnh dùng `Backend/` đúng chữ hoa.
- [x] `docs/error-codes.md` tồn tại và đã liệt kê `INTERNAL_SERVER_ERROR`.
- [x] Tài liệu phase này (`phase-0-foundation.md`) đã được review và chấp nhận trước khi viết code.

> **Tick ngày 2026-10-20 theo chỉ thị của chủ dự án** — bằng chứng từng mục tại `docs/progress.md` (34/34, chủ dự án xác nhận 2026-10-19: smoke test runtime + CI xanh trên push `17f8fc4`). Chạy kiểm chứng lại cùng ngày: `(cd Backend && ./mvnw clean verify)` → BUILD SUCCESS 6/6 test PASS; smoke runtime (`docker compose up -d` + `java -jar`): profile `local` tự kích hoạt (Q-0-11 — thời điểm đó; Q-0-12 sau đó chuyển sang cấu hình plugin, xem 6.3), `/api/v1/health` + `/actuator/health` + `/swagger-ui.html` = 200; `npm --prefix frontend test` 3/3 PASS; `npm --prefix frontend run lint` PASS.

---

## 10. Quyết định Kỹ thuật & Rủi ro

### 10.1. Quyết định đã chốt

| ID | Quyết định | Lý do | Phương án được cân nhắc |
|---|---|---|---|
| D-0-01 | Dùng `application.yml` thay vì `application.properties` | YAML hỗ trợ cấu trúc lồng nhau, dễ đọc, phổ biến trong Spring Boot hiện đại | `.properties` phẳng, khó đọc khi có nhiều cấu hình |
| D-0-02 | `ApiResponse` dùng Java `record` | Immutable, compact syntax, tự có `equals/hashCode/toString`, không cần Lombok | Class thường với `@Data` — thêm dependency không cần thiết cho wrapper đơn giản |
| D-0-03 | Static containers trong Testcontainers | Container khởi động một lần cho cả test class, giảm thời gian CI | Instance container (mỗi test) — chậm hơn đáng kể |
| D-0-04 | Ant Design **6.x** (đã cài 6.6.6) | Đủ component admin (Table, Form, Modal), không cần CSS tay, phù hợp DoD frontend; antd 6 là nhánh hiện hành cho React 19 (mục 2.3, B6) | Ant Design 5.x (thế hệ trước, chỉ bảo trì) / Material UI (bundle nhỏ hơn nhưng ít component admin sẵn có hơn) |
| D-0-05 | `ddl-auto: validate` | Flyway kiểm soát schema, Hibernate chỉ validate — tránh accident DDL | `ddl-auto: none` (không validate, khó phát hiện sai mapping) / `update` (nguy hiểm) |
| D-0-06 | Permit-all tạm thời cho endpoint public Phase 0 (`/api/v1/health`, `/actuator/health`, swagger) + bật CORS ở security (`.cors(Customizer.withDefaults())`) | Phase 0 chưa có auth; các endpoint này phải public để test, CI và xem Swagger; thiếu `.cors(...)` thì CORS của `WebConfig` không áp dụng khi qua security chain | Tắt Spring Security hoàn toàn — rủi ro quên bật lại ở Phase 1; hoặc chỉ permit `/api/v1/health` — swagger/actuator trả 401 |
| D-0-07 | **Giữ tên thư mục `Backend/`** (không đổi thành `backend/`) | Code đã push lên remote; đổi case trên Windows cần 2 lần `git mv` và sửa mọi tham chiếu tài liệu/CI — chi phí > lợi ích | Đổi sang `backend/` theo overview 4.1 (được cân nhắc nhưng chủ dự án quyết định không làm) |
| D-0-08 | Node **22 LTS** + React **19** + Vite bản mới nhất | Node 20 EOL 30/04/2026; vẫn thỏa mãn ràng buộc overview (Node 20+, React 18+) | Giữ Node 20/Vite 5/React 18 theo bản doc cũ — bị loại vì EOL và version đã lỗi thời |
| D-0-09 | Thêm Vitest + 1 smoke test ở Phase 0 | `AGENTS.md` mục 4 có lệnh `npm --prefix frontend test`; nếu Phase 0 không có runner thì lệnh chết | Hoãn sang Phase 1+ — lệnh AGENTS.md sai lệch ngay từ đầu |
| D-0-10 | Đồng bộ default dev password ở compose + `application.yml` + `.env.example` | Chạy `docker compose up -d` + `spring-boot:run` là chạy được ngay; tránh lỗi NOAUTH do hai default khác nhau; riêng integration test ghi đè password Redis rỗng ở profile `test` vì container Testcontainers không có password (A1 — 5.2.2) | Bắt buộc `.env` không default — an toàn hơn nhưng thêm bước dễ quên cho người xem demo |
| D-0-11 | Giữ Lombok và `spring-boot-devtools` có sẵn trong skeleton | Đã có từ đầu, không vi phạm quy tắc "không tự cài thêm dependency"; `ApiResponse` vẫn là `record` thuần | Xóa Lombok cho khớp bảng overview 3 — phải ghi nhận là thay đổi dependency ngoài mục đích Phase 0 |

### 10.2. Rủi ro đã xác định

| ID | Rủi ro | Mức độ | Biện pháp giảm thiểu |
|---|---|---|---|
| R-0-01 | Tên thư mục `Backend/` lệch với văn bản `project-overview.md`/`AGENTS.md` (đều ghi `backend/`) | **Đã xử lý** | Đã cập nhật `project-overview.md` + `AGENTS.md` về tên `Backend/` (Q-0-08, 2026-10-19); mọi lệnh README/CI giữ chữ hoa |
| R-0-02 | Boot 4: chỉ khai báo `flyway-core` → Flyway auto-config không kích hoạt, **migration chạy im lặng** | **Cao** | Dùng `spring-boot-starter-flyway` + `flyway-database-postgresql` (mục 2.4); TC-HEALTH-03 assert `flyway_schema_history` là lớp chặn cuối |
| R-0-03 | Testcontainers pull image lần đầu chậm (~2-3 phút) trên CI | **Thấp** | GitHub Actions cache Docker layers; chỉ chậm lần đầu |
| R-0-04 | `flyway-database-postgresql` phải khai báo riêng từ Flyway 10+ | **Trung bình** | Kiểm tra BOM version và thêm đúng artifact ID theo Flyway docs |
| R-0-05 | AI có thể sinh code theo kiểu Spring Boot 3 khi viết backend | **Cao** | Tuân thủ `AGENTS.md` mục 5 trước khi viết bất kỳ code backend nào; kiểm tra tên package, starter và annotation |
| R-0-06 | `Backend/mvnw` đang commit ở mode `100644` + `core.fileMode=false` → CI Linux báo `Permission denied` | **Đã xử lý** | Đã chạy `git update-index --chmod=+x Backend/mvnw` trước khi push; xác nhận `git ls-files -s` = `100755` (DoD 9.2; kiểm tra lại 2026-10-20) |
| R-0-07 | springdoc-openapi bản tương thích Spring Boot 4 chưa được xác nhận | **Đã xử lý** | springdoc **3.1.1** thêm vào `pom.xml`; Swagger UI load OK 2026-10-19 (Q-0-09, `progress.md` P0-9.2.10); `./mvnw test-compile` PASS 2026-10-20. Nhánh dự phòng (hoãn swagger khỏi DoD) không cần dùng |
| R-0-08 | Lệch default Redis/DB password giữa `docker-compose.yml` và `application.yml` gây NOAUTH khi chạy local | **Đã xử lý** | Đồng bộ default dev ở 3 nơi theo D-0-10; test profile đặt password Redis rỗng riêng (A1 — 5.2.2); kiểm tra tại DoD 9.1/9.2 |

### 10.3. Quyết định đã xác nhận (đóng câu hỏi)

| ID | Câu hỏi | Quyết định | Ngày chốt |
|---|---|---|---|
| Q-0-01 | Nhánh chính GitHub? | **`main`** — CI trigger trên `main` và `develop` | 2026-10-19 |
| Q-0-02 | Bật Spring Boot Actuator? | **Có** — chỉ expose `health`; `show-details: never` để không lộ thông tin nội bộ; thêm `spring-boot-starter-actuator` vào `pom.xml` | 2026-10-19 |
| Q-0-03 | Cách xử lý Redis/DB password cho local? | **Đồng bộ default dev ở cả 3 nơi** (compose, `application.yml`, `.env.example`) — D-0-10; không bắt buộc export env khi dev local | 2026-10-19 |
| Q-0-04 | Phiên bản Node/React/Vite? | **Node 22 LTS + React 19 + Vite bản mới nhất** — D-0-08 (Node 20 EOL 30/04/2026) | 2026-10-19 |
| Q-0-05 | Frontend có test runner ở Phase 0? | **Có — Vitest + FE-SMOKE-01** — D-0-09, để lệnh `npm --prefix frontend test` của AGENTS.md chạy được | 2026-10-19 |
| Q-0-06 | Giữ Lombok/devtools trong skeleton? | **Giữ** — D-0-11 | 2026-10-19 |
| Q-0-07 | Đổi tên `Backend/` → `backend/`? | **Không đổi** — D-0-07 (code đã push; chấp nhận sai lệch tên với overview/AGENTS) | 2026-10-19 |
| Q-0-08 | Sửa `project-overview.md` và `AGENTS.md` về tên `Backend/`? | **Có** — đã cập nhật cả 2 file cùng ngày; chấm dứt sai lệch (khớp với D-0-07) | 2026-10-19 |
| Q-0-09 | springdoc-openapi bản nào tương thích Spring Boot 4? | **3.1.1** — thêm vào `pom.xml`, Swagger UI xác nhận load được (đóng O-0-01/R-0-07) | 2026-10-19 |
| Q-0-10 | Phiên bản Ant Design thực tế? | **antd 6.6.6 + `@ant-design/icons` 6.3.4** (React 19.3.1) theo `package.json` — chốt 6.x, cập nhật mục 2.1/2.3/D-0-04 (B6) | 2026-10-20 |
| Q-0-11 | Thêm `spring.profiles.default: local` vào `application.yml` (C7)? | ~~Có~~ **Đã thay thế cùng ngày bởi Q-0-12** — áp 2026-10-20 rồi gỡ ngay khi review Phase 1 phát hiện dòng này bật luôn default dev khi deploy quên `SPRING_PROFILES_ACTIVE` (mâu thuẫn fail-fast D-1-09 của Phase 1) | 2026-10-20 |
| Q-0-12 | Thay `spring.profiles.default: local` bằng cấu hình `<profiles>` của `spring-boot-maven-plugin` (chỉ goal `spring-boot:run`)? | **Có** — theo chốt A2(a) trong review Phase 1; jar production không có profile mặc định → fail-fast thật; parameter `profiles` xác nhận trên docs.spring.io (Boot 4.1.1), xem 6.3 | 2026-10-20 |

### 10.4. Câu hỏi còn mở (giải quyết lúc triển khai)

| ID | Câu hỏi | Cách xử lý |
|---|---|---|
| O-0-01 | ~~springdoc-openapi bản nào tương thích Spring Boot 4?~~ | **Đã xử lý** (Q-0-09) — 3.1.1, Swagger UI load OK; R-0-07 đóng |
| O-0-02 | ~~`project-overview.md`/`AGENTS.md` vẫn ghi `backend/`~~ | **Đã xử lý** (Q-0-08) — cả 2 file đã sửa về `Backend/` |
| O-0-03 | ~~Version Ant Design / Testcontainers / springdoc chính xác tại thời điểm cài~~ | **Đã xử lý** (Q-0-10) — đã ghi vào mục 2.1: antd 6.6.6, springdoc 3.1.1, Boot 4.1.1, Testcontainers artifact 2.x theo BOM |
| O-0-04 | ~~Thêm `spring.profiles.default: local` vào file `application.yml` thật (C7)?~~ | **Đã xử lý** (Q-0-11 → Q-0-12 cùng ngày 2026-10-20) — cuối cùng: profile `local` cấu hình trong `spring-boot-maven-plugin`, không nằm trong `application.yml` |

---

## 11. Thứ tự Triển khai Đề xuất

Thứ tự thực hiện khi bắt đầu Phase 0:

1. **Chuẩn bị skeleton** (R-0-06): `git update-index --chmod=+x Backend/mvnw`; đối chiếu `pom.xml` giữ nguyên 3 test starter Boot 4 (mục 2.4).
2. **Tạo root files:** `docker-compose.yml`, `.env.example` (mặc định dev đồng bộ — D-0-10), cập nhật `.gitignore` nếu thiếu.
3. **Cấu hình backend:** bổ sung dependency theo mục 2.4 vào `pom.xml`; viết `application.yml` (thay và **xóa** `application.properties` cũ để tránh 2 file cấu hình cùng tồn tại; cấu hình profile local qua `spring-boot-maven-plugin` — C7/Q-0-12, xem 6.3), `application-local.yml`.
4. **Tạo Flyway migration V1** (`V1__baseline.sql`).
5. **Implement `ApiResponse<T>`** và `GlobalExceptionHandler`; ghi `INTERNAL_SERVER_ERROR` vào `docs/error-codes.md`.
6. **Implement `SecurityConfig`** (permit-all endpoint public Phase 0: `/api/v1/health`, `/actuator/health`, swagger + bật `.cors(Customizer.withDefaults())` — D-0-06), `WebConfig` (CORS) và `HealthController`.
7. **Implement `BaseIntegrationTest`** (Testcontainers 2.x + `@ActiveProfiles("test")` + `@AutoConfigureTestRestTemplate` — 5.2.1), `application-test.yml` (password Redis rỗng — 5.2.2) và các test case TC-HEALTH-01 đến TC-CORS-01.
8. **Chạy `(cd Backend && ./mvnw clean verify)`** — xác nhận tất cả test PASS.
9. **Khởi tạo frontend** (Node 22, Vite/React bản mới nhất), cài Ant Design + Axios + Vitest, implement gọi health endpoint và FE-SMOKE-01.
10. **Tạo `.github/workflows/ci.yml`** (working-directory `Backend`, Node 22, frontend chạy `npm test`) — push lên GitHub, xác nhận CI xanh.
11. **Viết README** với hướng dẫn đầy đủ (dùng `Backend/` đúng chữ hoa).
12. **Review checklist DoD** (mục 9) — báo cáo kết quả theo format AGENTS.md mục 3.

---

*Tài liệu soạn ngày 2026-10-19; cùng ngày được duyệt và cập nhật theo các quyết định D-0-07 → D-0-11 / Q-0-03 → Q-0-07. Rà soát kỹ thuật bổ sung ngày 2026-10-20 sau khi Phase 0 đã triển khai (commit `17f8fc4`): đối chiếu với code thật và sửa các mục A1–A4 / B5–B6 / C7–C9 (TestRestTemplate trên Boot 4, SecurityConfig permit-all + CORS, Testcontainers 2.x, mốc EOL Spring Boot 4.1, antd 6.x, springdoc 3.1.1, cách nạp `.env`/profile local, cây thư mục). Thay đổi code đi kèm đã qua xác nhận chủ dự án: `spring.profiles.default: local` áp vào `application.yml` ngày 2026-10-20 (O-0-04 → Q-0-11), cùng ngày chuyển sang cấu hình `spring-boot-maven-plugin` theo Q-0-12 sau review Phase 1 (A2(a)); DoD mục 9 tick cùng ngày theo chỉ thị chủ dự án (bằng chứng: `docs/progress.md`).*`
