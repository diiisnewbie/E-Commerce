# Phase 0 — Khởi tạo (Foundation)

> **Trạng thái:** Not started  
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
- Bảo mật Spring Security đầy đủ (chỉ cấu hình permit-all tạm thời cho health endpoint).
- Schema bảng nghiệp vụ (users, products, v.v.) — thuộc Phase 1+.
- Tối ưu hiệu năng, responsive, animation frontend.

---

## 2. Quyết định Tech Stack

### 2.1. Phiên bản được chốt

| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| Java | **21** (LTS) | Bắt buộc; dùng Java record, sealed class và virtual thread nếu cần |
| Spring Boot | **4.1.x** (bản mới nhất nhánh 4.1 khi khởi tạo) | Spring Boot 3.x hết hỗ trợ OSS 30/06/2026; xem lý do đầy đủ ở `project-overview.md` mục 3.1 |
| Maven Wrapper | `mvnw` kèm theo Spring Initializr | Không yêu cầu cài Maven toàn cục |
| PostgreSQL | **16** (image `postgres:16-alpine`) | `alpine` nhẹ hơn, đủ cho dev |
| Redis | **7** (image `redis:7-alpine`) | `alpine` nhẹ hơn, đủ cho dev |
| Flyway | Theo Spring Boot BOM | Boot 4 bắt buộc `spring-boot-starter-flyway` + `flyway-database-postgresql`; **không** dùng `flyway-core` trần (xem mục 2.4) |
| Testcontainers | Theo BOM của Spring Boot 4.1 | Dùng `testcontainers-bom` do Boot quản lý; không pin cứng — xác nhận bằng `./mvnw dependency:tree` lúc cài |
| Node.js | **22 LTS** | Node 20 EOL 30/04/2026; overview quy định "20+" nên 22 LTS vẫn hợp lệ (D-0-08) |
| React | **19.x** | Sinh bởi `npm create vite` bản mới nhất; overview quy định "18+" nên 19 vẫn hợp lệ (D-0-08) |
| Vite | **bản mới nhất** khi scaffold | Không hạ về 5.x; giữ nguyên bản do create-vite sinh để tương thích Node 22 (D-0-08) |
| Vitest | Bản tương thích Vite đã chọn | Test runner frontend, thay cho việc để lệnh `npm --prefix frontend test` của AGENTS.md chết (D-0-09) |
| Ant Design | **5.x** (bản mới nhất) | UI library đã chốt ở mục 2.3; xác nhận bản hỗ trợ React 19 trước khi cài |
| Lombok, devtools | Theo Spring Boot BOM | Có sẵn từ skeleton, giữ nguyên (D-0-11) |
| springdoc-openapi | **3.x** | Phiên bản tương thích Spring Boot 4.x; xác nhận trước khi thêm vào `pom.xml` (xem R-0-07) |

### 2.2. Quyết định Spring Boot 4.1.x — Đã chốt

**Quyết định:** Dùng **Spring Boot 4.1.x** (nhánh 4.1, bản mới nhất khi khởi tạo project trên start.spring.io).

**Lý do** (chi tiết đầy đủ tại `project-overview.md` mục 3.1):
- Spring Boot 3.x (kể cả 3.5) hết hỗ trợ mã nguồn mở từ **30/06/2026** — không còn bản vá bảo mật miễn phí.
- Dự án khởi tạo tháng 10/2026 → nhánh 4.1.x là lựa chọn duy nhất còn được hỗ trợ OSS (hết hỗ trợ 31/12/2026, đủ để hoàn thành dự án portfolio).
- Spring Boot 4 chạy trên Spring Framework 7 và Spring Security 7, yêu cầu Java 17+ (dự án dùng Java 21).

**Lưu ý quan trọng khi code với Spring Boot 4:** Xem `AGENTS.md` mục 5 để tránh các lỗi thường gặp khi AI sinh code theo kiểu Spring Boot 3 (tên starter thay đổi, package Jackson 3, Security 7, v.v.).

### 2.3. UI Library Frontend — Ant Design 5.x

**Lý do chọn Ant Design:**
- Bộ component đầy đủ (Table, Form, Modal, Layout) đáp ứng toàn bộ nhu cầu frontend của dự án.
- Thiết kế nhất quán, không cần viết CSS tùy biến.
- Tài liệu tiếng Anh/Trung phong phú, cộng đồng lớn.
- Phù hợp với mục 4.7 (dùng UI library, hạn chế CSS tay).

**Trade-off được chấp nhận:** Bundle size lớn hơn Material UI (~2MB gzip). Chấp nhận vì đây là portfolio, không phải sản phẩm production tối ưu.

**Cấu hình:** Dùng tree-shaking mặc định của Vite + `@ant-design/icons` riêng (chỉ import icon cần dùng).

**Tương thích React 19:** Cài bản antd 5.x mới nhất (đã hỗ trợ React 19); nếu tại thời điểm khởi tạo antd đã có bản major mới ổn định, ghi lại phiên bản thực tế vào mục 2.1 thay vì đoán.

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

<!-- API Docs — xác nhận artifact ID tương thích Spring Boot 4 trước khi thêm -->
springdoc-openapi-starter-webmvc-ui (3.x)

<!-- Testing -->
org.springframework.boot:spring-boot-testcontainers (scope test)
org.testcontainers:testcontainers-bom (import scope, quản lý phiên bản)
org.testcontainers:postgresql (scope test)
org.testcontainers:junit-jupiter (scope test)
```

> **Vì sao không thêm `flyway-core`:** từ Boot 4, Flyway auto-configuration nằm ở `spring-boot-starter-flyway`. Chỉ khai báo `flyway-core` + `flyway-database-postgresql` → context khởi động bình thường nhưng **migration không chạy im lặng**. Test TC-HEALTH-03 là lớp chặn cuối phát hiện lỗi này.

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
│   ├── error-codes.md            # tạo ở Phase 0 với skeleton rỗng
│   ├── phase-0-foundation.md     # file này
│   └── phase-1-auth.md           # tạo trước khi làm Phase 1
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
│       │   │   │   └── security/      # SecurityConfig (permit-all /api/v1/health)
│       │   │   └── health/
│       │   │       └── controller/    # HealthController
│       │   └── resources/
│       │       ├── application.yml    # thay thế application.properties (xóa file cũ)
│       │       ├── application-local.yml
│       │       └── db/migration/
│       │           └── V1__baseline.sql
│       └── test/
│           ├── java/com/app/backend/
│           │   ├── common/            # BaseIntegrationTest
│           │   └── health/
│           │       └── HealthIntegrationTest.java
│           └── resources/
│               └── application-test.yml
└── frontend/
    ├── package.json                # scripts: dev, build, test (vitest run)
    ├── package-lock.json
    ├── vite.config.js              # build + test config (jsdom, setup file)
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── App.test.jsx            # Vitest smoke test (D-0-09)
        ├── test/setup.js           # jest-dom matchers
        ├── api/
        │   └── health.js           # gọi health endpoint
        └── components/
            └── HealthStatus.jsx    # hiển thị kết quả
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
│       └── SecurityConfig.java     # SecurityFilterChain: permit-all /api/v1/health
└── health
    └── controller
        └── HealthController.java
```

> **Bắt buộc có `SecurityConfig`:** `spring-boot-starter-security` đã trong `pom.xml` → mặc định Spring Security block mọi request. Không có filter chain permit-all thì `GET /api/v1/health` trả 401 và TC-HEALTH-02 FAIL. Đây là nội dung thực thi của quyết định D-0-06.

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
// Gợi ý — triển khai chính xác thuộc bước code
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

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

**Lý do dùng `static` containers:** Container khởi động một lần cho toàn bộ test class, giảm thời gian test đáng kể so với instance container (mỗi test method).

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
      # host và port được ghi đè bởi @DynamicPropertySource

logging:
  level:
    com.app.backend: DEBUG
    org.testcontainers: INFO
```

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
| TC-HEALTH-01 | Integration | SpringBootTest + Testcontainers + TestRestTemplate | ~15–30s (cold start) |
| TC-HEALTH-02 | Integration | SpringBootTest + MockMvc | ~1s (reuse context) |
| TC-HEALTH-03 | Integration | SpringBootTest + JdbcTemplate assertion | ~1s (reuse context) |
| TC-HEALTH-04 | Integration | SpringBootTest + RedisTemplate | ~1s (reuse context) |
| TC-CORS-01 | Integration | SpringBootTest + MockMvc | ~1s (reuse context) |

**Lưu ý:** Unit test với Mockito sẽ xuất hiện từ Phase 1 khi có service logic đáng test. Phase 0 không có service logic, integration test là phù hợp.

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

Không cài thêm thư viện state management (Redux, Zustand, Jotai) theo quy định mục 4.7 của `project-overview.md`.

---

## 9. Definition of Done — Phase 0

Checklist phải được kiểm tra đủ trước khi Phase 0 được chuyển sang `Done`. Chủ dự án xác nhận từng mục sau khi có bằng chứng kiểm tra phù hợp.

### 9.1. Hạ tầng & Môi trường

- [ ] `docker compose up -d` khởi động thành công PostgreSQL 16 và Redis 7.
- [ ] Cả hai service pass healthcheck (`docker compose ps` hiển thị `healthy`).
- [ ] `.env.example` tồn tại ở thư mục gốc, không chứa secret thật.
- [ ] `.env` (nếu có) đã được thêm vào `.gitignore`.

### 9.2. Backend

- [ ] Thư mục giữ nguyên tên `Backend/` (D-0-07); mọi lệnh trong README và CI dùng đúng chữ hoa/thường.
- [ ] `git ls-files -s Backend/mvnw` hiển thị mode `100755` (đã chạy `git update-index --chmod=+x Backend/mvnw` — R-0-06).
- [ ] Spring Boot version trong `pom.xml` là **4.1.x** (đã chốt theo `project-overview.md` mục 3.1).
- [ ] `pom.xml` có `spring-boot-starter-flyway` + `flyway-database-postgresql` (không chỉ `flyway-core`) và `spring-boot-starter-actuator`.
- [ ] 3 test starter `webmvc-test` / `security-test` / `validation-test` **còn nguyên vẹn** (không bị xóa nhầm — chúng là starter đúng của Boot 4).
- [ ] Backend khởi động thành công với `(cd Backend && ./mvnw spring-boot:run)` khi Docker services đang chạy.
- [ ] Flyway migration V1 chạy thành công (log hiển thị `Successfully applied 1 migration`).
- [ ] `GET /api/v1/health` trả HTTP 200 với JSON đúng format (kiểm tra bằng curl hoặc Swagger).
- [ ] `GET /actuator/health` trả HTTP 200 và không lộ chi tiết (show-details: never — Q-0-02).
- [ ] Swagger UI truy cập được tại `http://localhost:8080/swagger-ui.html` (nếu springdoc tương thích Boot 4 — xem R-0-07).
- [ ] Không có secret hoặc password hardcode trong code hoặc file config được commit (default dev đã công khai ở `.env.example` được phép).

### 9.3. Kiểm thử

- [ ] `(cd Backend && ./mvnw clean verify)` chạy thành công, không có test FAIL.
- [ ] TC-HEALTH-01 PASS.
- [ ] TC-HEALTH-02 PASS.
- [ ] TC-HEALTH-03 PASS.
- [ ] TC-HEALTH-04 PASS.
- [ ] TC-CORS-01 PASS.
- [ ] FE-SMOKE-01 PASS (`npm --prefix frontend test`).

### 9.4. CI

- [ ] File `.github/workflows/ci.yml` tồn tại với hai job riêng.
- [ ] Job `backend` chạy xanh trên GitHub Actions (bao gồm `./mvnw clean verify` với Testcontainers).
- [ ] Job `frontend` chạy xanh trên GitHub Actions (build + `npm test`).
- [ ] Không có secret trong CI logs.

### 9.5. Frontend

- [ ] `npm --prefix frontend install` thành công.
- [ ] `npm --prefix frontend run dev` khởi động tại `http://localhost:5173`.
- [ ] Frontend gọi `GET /api/v1/health` và hiển thị kết quả trên UI.
- [ ] Không có CORS error trong browser console.
- [ ] `npm --prefix frontend test` chạy xanh (script `test` = `vitest run`).

### 9.6. Tài liệu

- [ ] `README.md` ở thư mục gốc có hướng dẫn: yêu cầu cài đặt (Node 22, Java 21, Docker), chạy Docker services, chạy backend, chạy frontend, chạy test — lệnh dùng `Backend/` đúng chữ hoa.
- [ ] `docs/error-codes.md` tồn tại và đã liệt kê `INTERNAL_SERVER_ERROR`.
- [ ] Tài liệu phase này (`phase-0-foundation.md`) đã được review và chấp nhận trước khi viết code.

---

## 10. Quyết định Kỹ thuật & Rủi ro

### 10.1. Quyết định đã chốt

| ID | Quyết định | Lý do | Phương án được cân nhắc |
|---|---|---|---|
| D-0-01 | Dùng `application.yml` thay vì `application.properties` | YAML hỗ trợ cấu trúc lồng nhau, dễ đọc, phổ biến trong Spring Boot hiện đại | `.properties` phẳng, khó đọc khi có nhiều cấu hình |
| D-0-02 | `ApiResponse` dùng Java `record` | Immutable, compact syntax, tự có `equals/hashCode/toString`, không cần Lombok | Class thường với `@Data` — thêm dependency không cần thiết cho wrapper đơn giản |
| D-0-03 | Static containers trong Testcontainers | Container khởi động một lần cho cả test class, giảm thời gian CI | Instance container (mỗi test) — chậm hơn đáng kể |
| D-0-04 | Ant Design 5.x | Đủ component admin (Table, Form, Modal), không cần CSS tay, phù hợp DoD frontend | Material UI (bundle nhỏ hơn nhưng ít component admin sẵn có hơn) |
| D-0-05 | `ddl-auto: validate` | Flyway kiểm soát schema, Hibernate chỉ validate — tránh accident DDL | `ddl-auto: none` (không validate, khó phát hiện sai mapping) / `update` (nguy hiểm) |
| D-0-06 | Permit-all tạm thời cho `/api/v1/health` | Phase 0 chưa có auth; endpoint phải public để test và CI; thực thi bằng `common/security/SecurityConfig.java` | Tắt Spring Security hoàn toàn — rủi ro quên bật lại ở Phase 1 |
| D-0-07 | **Giữ tên thư mục `Backend/`** (không đổi thành `backend/`) | Code đã push lên remote; đổi case trên Windows cần 2 lần `git mv` và sửa mọi tham chiếu tài liệu/CI — chi phí > lợi ích | Đổi sang `backend/` theo overview 4.1 (được cân nhắc nhưng chủ dự án quyết định không làm) |
| D-0-08 | Node **22 LTS** + React **19** + Vite bản mới nhất | Node 20 EOL 30/04/2026; vẫn thỏa mãn ràng buộc overview (Node 20+, React 18+) | Giữ Node 20/Vite 5/React 18 theo bản doc cũ — bị loại vì EOL và version đã lỗi thời |
| D-0-09 | Thêm Vitest + 1 smoke test ở Phase 0 | `AGENTS.md` mục 4 có lệnh `npm --prefix frontend test`; nếu Phase 0 không có runner thì lệnh chết | Hoãn sang Phase 1+ — lệnh AGENTS.md sai lệch ngay từ đầu |
| D-0-10 | Đồng bộ default dev password ở compose + `application.yml` + `.env.example` | Chạy `docker compose up -d` + `spring-boot:run` là chạy được ngay; tránh lỗi NOAUTH do hai default khác nhau | Bắt buộc `.env` không default — an toàn hơn nhưng thêm bước dễ quên cho người xem demo |
| D-0-11 | Giữ Lombok và `spring-boot-devtools` có sẵn trong skeleton | Đã có từ đầu, không vi phạm quy tắc "không tự cài thêm dependency"; `ApiResponse` vẫn là `record` thuần | Xóa Lombok cho khớp bảng overview 3 — phải ghi nhận là thay đổi dependency ngoài mục đích Phase 0 |

### 10.2. Rủi ro đã xác định

| ID | Rủi ro | Mức độ | Biện pháp giảm thiểu |
|---|---|---|---|
| R-0-01 | Tên thư mục `Backend/` lệch với văn bản `project-overview.md`/`AGENTS.md` (đều ghi `backend/`) | **Đã xử lý** | Đã cập nhật `project-overview.md` + `AGENTS.md` về tên `Backend/` (Q-0-08, 2026-10-19); mọi lệnh README/CI giữ chữ hoa |
| R-0-02 | Boot 4: chỉ khai báo `flyway-core` → Flyway auto-config không kích hoạt, **migration chạy im lặng** | **Cao** | Dùng `spring-boot-starter-flyway` + `flyway-database-postgresql` (mục 2.4); TC-HEALTH-03 assert `flyway_schema_history` là lớp chặn cuối |
| R-0-03 | Testcontainers pull image lần đầu chậm (~2-3 phút) trên CI | **Thấp** | GitHub Actions cache Docker layers; chỉ chậm lần đầu |
| R-0-04 | `flyway-database-postgresql` phải khai báo riêng từ Flyway 10+ | **Trung bình** | Kiểm tra BOM version và thêm đúng artifact ID theo Flyway docs |
| R-0-05 | AI có thể sinh code theo kiểu Spring Boot 3 khi viết backend | **Cao** | Tuân thủ `AGENTS.md` mục 5 trước khi viết bất kỳ code backend nào; kiểm tra tên package, starter và annotation |
| R-0-06 | `Backend/mvnw` đang commit ở mode `100644` + `core.fileMode=false` → CI Linux báo `Permission denied` | **Cao** | Chạy `git update-index --chmod=+x Backend/mvnw` trước khi push; DoD 9.2 kiểm tra lại bằng `git ls-files -s` |
| R-0-07 | springdoc-openapi bản tương thích Spring Boot 4 chưa được xác nhận | **Trung bình** | Xác nhận artifact + version đúng lúc thêm vào `pom.xml`. Nếu chưa có bản compatible: **không giữ cố DoD "Swagger UI"** — dừng lại hỏi chủ dự án để hoãn swagger sang phase sau |
| R-0-08 | Lệch default Redis/DB password giữa `docker-compose.yml` và `application.yml` gây NOAUTH khi chạy local | **Đã xử lý** | Đồng bộ default dev ở 3 nơi theo D-0-10; kiểm tra tại DoD 9.1/9.2 |

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

### 10.4. Câu hỏi còn mở (giải quyết lúc triển khai)

| ID | Câu hỏi | Cách xử lý |
|---|---|---|
| O-0-01 | springdoc-openapi bản nào tương thích Spring Boot 4? | Xác nhận lúc thêm dependency. Nếu chưa có → dừng, hỏi chủ dự án để hoãn Swagger khỏi DoD Phase 0 (R-0-07) |
| O-0-02 | ~~`project-overview.md`/`AGENTS.md` vẫn ghi `backend/`~~ | **Đã xử lý** (Q-0-08) — cả 2 file đã sửa về `Backend/` |
| O-0-03 | Version Ant Design / Testcontainers / springdoc chính xác tại thời điểm cài | Ghi lại phiên bản thực tế đã cài vào mục 2.1 sau khi `pom.xml`/`package.json` ổn định (overview mục 3 bắt buộc khớp bảng) |

---

## 11. Thứ tự Triển khai Đề xuất

Thứ tự thực hiện khi bắt đầu Phase 0:

1. **Chuẩn bị skeleton** (R-0-06): `git update-index --chmod=+x Backend/mvnw`; đối chiếu `pom.xml` giữ nguyên 3 test starter Boot 4 (mục 2.4).
2. **Tạo root files:** `docker-compose.yml`, `.env.example` (mặc định dev đồng bộ — D-0-10), cập nhật `.gitignore` nếu thiếu.
3. **Cấu hình backend:** bổ sung dependency theo mục 2.4 vào `pom.xml`; viết `application.yml` (thay và **xóa** `application.properties` cũ để tránh 2 file cấu hình cùng tồn tại), `application-local.yml`.
4. **Tạo Flyway migration V1** (`V1__baseline.sql`).
5. **Implement `ApiResponse<T>`** và `GlobalExceptionHandler`; ghi `INTERNAL_SERVER_ERROR` vào `docs/error-codes.md`.
6. **Implement `SecurityConfig`** (permit-all `/api/v1/health` — D-0-06), `WebConfig` (CORS) và `HealthController`.
7. **Implement `BaseIntegrationTest`** (Testcontainers + `@ActiveProfiles("test")`) và các test case TC-HEALTH-01 đến TC-CORS-01.
8. **Chạy `(cd Backend && ./mvnw clean verify)`** — xác nhận tất cả test PASS.
9. **Khởi tạo frontend** (Node 22, Vite/React bản mới nhất), cài Ant Design + Axios + Vitest, implement gọi health endpoint và FE-SMOKE-01.
10. **Tạo `.github/workflows/ci.yml`** (working-directory `Backend`, Node 22, frontend chạy `npm test`) — push lên GitHub, xác nhận CI xanh.
11. **Viết README** với hướng dẫn đầy đủ (dùng `Backend/` đúng chữ hoa).
12. **Review checklist DoD** (mục 9) — báo cáo kết quả theo format AGENTS.md mục 3.

---

*Tài liệu soạn ngày 2026-10-19; cùng ngày được duyệt và cập nhật theo các quyết định D-0-07 → D-0-11 / Q-0-03 → Q-0-07. Không sửa sau khi Phase 0 bắt đầu triển khai, trừ khi có quyết định thay đổi từ chủ dự án được ghi rõ lý do.*
