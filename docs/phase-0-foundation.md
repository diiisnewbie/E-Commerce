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
| Cấu trúc repository | Chuẩn hóa thư mục `backend/`, `frontend/`, `docs/`, root files |
| Backend skeleton | Spring Boot application có thể khởi động, kết nối PostgreSQL, chạy Flyway |
| Docker Compose | Khởi động PostgreSQL 16 và Redis 7 cho môi trường phát triển |
| Flyway baseline | Migration đầu tiên tạo bảng `schema_version` sentinel (không có bảng nghiệp vụ) |
| Health endpoint | `GET /api/v1/health` trả đúng response contract |
| Testcontainers | Cấu hình integration test với PostgreSQL và Redis thật |
| CI GitHub Actions | Hai job riêng: backend (build + test), frontend (build) |
| Frontend skeleton | React + Vite khởi động và gọi được health endpoint (xác nhận CORS) |
| Tài liệu | README ban đầu, `.env.example` |

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
| Flyway | Theo Spring Boot BOM | Quản lý qua dependency `flyway-core` + `flyway-database-postgresql` |
| Testcontainers | **1.20.x** (bản ổn định mới nhất) | Dùng `testcontainers-bom` để quản lý phiên bản nhất quán |
| Node.js | **20 LTS** (hoặc mới hơn) | Frontend dev environment |
| React | **18.x** | Dùng React 18 với concurrent features |
| Vite | **5.x** | Build tool frontend |
| Ant Design | **5.x** | UI component library được chốt (xem mục 2.3) |
| springdoc-openapi | **3.x** | Phiên bản tương thích Spring Boot 4.x; xác nhận trên start.spring.io trước khi thêm vào `pom.xml` |

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

### 2.4. Dependency bổ sung cần thêm vào `pom.xml`

Skeleton hiện tại thiếu các dependency sau, cần bổ sung ở Phase 0:

```xml
<!-- Data -->
spring-boot-starter-data-jpa
spring-boot-starter-data-redis

<!-- DB Migration -->
flyway-core
flyway-database-postgresql

<!-- API Docs -->
<!-- Xác nhận artifact ID tương thích Spring Boot 4 trước khi thêm -->
springdoc-openapi-starter-webmvc-ui (3.x)

<!-- Testing -->
spring-boot-testcontainers
testcontainers (BOM)
testcontainers:postgresql
testcontainers:junit-jupiter
```

> **Ghi chú Spring Boot 4 — tên starter thay đổi:** Xem `AGENTS.md` mục 5 trước khi thêm bất kỳ dependency nào. Ví dụ: dùng `spring-boot-starter-webmvc` thay vì `spring-boot-starter-web`; package Jackson là `tools.jackson...` thay vì `com.fasterxml.jackson...`. Luôn tra tài liệu chính thức (Spring Boot 4.0 Migration Guide) thay vì dùng lại snippet từ Spring Boot 3.

> **Ghi chú về dependency test sai hiện tại:** `pom.xml` hiện có các artifact không tồn tại (`spring-boot-starter-security-test`, `spring-boot-starter-validation-test`, `spring-boot-starter-webmvc-test` với groupId `org.springframework.boot`). Cần xóa và thay bằng các starter test đúng của Spring Boot 4 (xem `AGENTS.md` mục 5 để biết artifact ID chính xác).

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
├── backend/                      # lowercase, không phải Backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/wrapper/
│   └── src/
│       ├── main/
│       │   ├── java/com/app/backend/
│       │   │   ├── BackendApplication.java
│       │   │   ├── common/
│       │   │   │   ├── config/        # WebConfig (CORS), OpenApiConfig
│       │   │   │   ├── exception/     # GlobalExceptionHandler
│       │   │   │   └── response/      # ApiResponse<T> record
│       │   │   └── health/
│       │   │       └── controller/    # HealthController
│       │   └── resources/
│       │       ├── application.yml    # thay thế application.properties
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
    ├── package.json
    ├── package-lock.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── api/
        │   └── health.js           # gọi health endpoint
        └── components/
            └── HealthStatus.jsx    # hiển thị kết quả
```

### 3.2. Ghi chú quan trọng về tên thư mục

- `project-overview.md` mục 4.1 dùng `backend/` (lowercase). Skeleton hiện tại nằm ở `Backend/` (Pascal case — không chuẩn trên Linux/macOS CI).
- **Quyết định:** Đổi tên thư mục `Backend/` -> `backend/` khi bắt đầu Phase 0.
- Trên Windows, `git mv Backend backend` có thể không đổi được case; cần dùng: `git mv Backend backend_tmp && git mv backend_tmp backend`.
- Quan trọng vì GitHub Actions CI chạy trên Linux runner; đường dẫn phân biệt hoa thường.

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
│   └── response
│       └── ApiResponse.java        # generic wrapper <T>
└── health
    └── controller
        └── HealthController.java
```

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
# Database
POSTGRES_DB=ecommerce
POSTGRES_USER=ecommerce_user
POSTGRES_PASSWORD=changeme_in_production

# Redis
REDIS_PASSWORD=changeme_redis_in_production

# Backend
SPRING_PROFILES_ACTIVE=local
DB_HOST=localhost
DB_PORT=5432
REDIS_HOST=localhost
REDIS_PORT=6379
```

> **Quy tắc:** `.env.example` được commit. File `.env` thật **không được commit** (thêm vào `.gitignore`).

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
      password: ${REDIS_PASSWORD:}   # bắt buộc khớp với docker-compose; rỗng = không auth
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

**File:** `backend/src/main/resources/db/migration/V1__baseline.sql`

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
        working-directory: backend
        run: ./mvnw clean verify

  frontend:
    name: Frontend — Build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Build
        working-directory: frontend
        run: npm run build
```

**Lý do chọn `temurin`:** Eclipse Temurin là OpenJDK build chính thức, miễn phí, ổn định, được GitHub Actions hỗ trợ tốt.

**Lý do cache Maven:** Tránh tải lại toàn bộ dependency (~200MB) mỗi lần chạy CI.

**Testcontainers trong CI:** GitHub Actions ubuntu-latest runner có Docker pre-installed. Testcontainers tự spin up container trong job `backend`. Không cần khai báo service riêng trong CI cho testing.

### 7.2. Trigger strategy

- **push to main/develop:** Chạy CI đầy đủ. Đảm bảo nhánh chính luôn xanh.
- **pull_request to main/develop:** Chạy CI trước khi merge. Bắt lỗi sớm.

---

## 8. Frontend Skeleton

### 8.1. Yêu cầu tối thiểu Phase 0

- Ứng dụng React + Vite khởi động tại `http://localhost:5173`.
- Gọi `GET /api/v1/health` khi trang tải.
- Hiển thị kết quả: status `UP`/`DOWN`, service name, timestamp.
- Xác nhận CORS hoạt động (không bị blocked bởi browser).

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
```

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

- [ ] Thư mục `backend/` là lowercase (không phải `Backend/`).
- [ ] Spring Boot version trong `pom.xml` là **4.1.x** (đã chốt theo `project-overview.md` mục 3.1).
- [ ] `pom.xml` không chứa artifact ID không tồn tại hoặc sai; các dependency test đã được thay bằng artifact đúng của Spring Boot 4 (xem `AGENTS.md` mục 5).
- [ ] Backend khởi động thành công với `(cd backend && ./mvnw spring-boot:run)` khi Docker services đang chạy.
- [ ] Flyway migration V1 chạy thành công (log hiển thị `Successfully applied 1 migration`).
- [ ] `GET /api/v1/health` trả HTTP 200 với JSON đúng format (kiểm tra bằng curl hoặc Swagger).
- [ ] Swagger UI truy cập được tại `http://localhost:8080/swagger-ui.html`.
- [ ] Không có secret hoặc password hardcode trong code hoặc file config được commit.

### 9.3. Kiểm thử

- [ ] `(cd backend && ./mvnw clean verify)` chạy thành công, không có test FAIL.
- [ ] TC-HEALTH-01 PASS.
- [ ] TC-HEALTH-02 PASS.
- [ ] TC-HEALTH-03 PASS.
- [ ] TC-HEALTH-04 PASS.
- [ ] TC-CORS-01 PASS.

### 9.4. CI

- [ ] File `.github/workflows/ci.yml` tồn tại với hai job riêng.
- [ ] Job `backend` chạy xanh trên GitHub Actions.
- [ ] Job `frontend` chạy xanh trên GitHub Actions.
- [ ] Không có secret trong CI logs.

### 9.5. Frontend

- [ ] `npm --prefix frontend install` thành công.
- [ ] `npm --prefix frontend run dev` khởi động tại `http://localhost:5173`.
- [ ] Frontend gọi `GET /api/v1/health` và hiển thị kết quả trên UI.
- [ ] Không có CORS error trong browser console.

### 9.6. Tài liệu

- [ ] `README.md` ở thư mục gốc có hướng dẫn: yêu cầu cài đặt, chạy Docker services, chạy backend, chạy frontend, chạy test.
- [ ] `docs/error-codes.md` tồn tại (skeleton, chưa cần mã lỗi nghiệp vụ).
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
| D-0-06 | Permit-all tạm thời cho `/api/v1/health` | Phase 0 chưa có auth; endpoint phải public để test và CI | Tắt Spring Security hoàn toàn — rủi ro quên bật lại ở Phase 1 |

### 10.2. Rủi ro đã xác định

| ID | Rủi ro | Mức độ | Biện pháp giảm thiểu |
|---|---|---|---|
| R-0-01 | Tên thư mục `Backend/` (Pascal) trên Windows CI/CD chạy Linux | **Cao** | Đổi tên sang `backend/` ngay đầu Phase 0 trước khi push |
| R-0-02 | Dependency test sai trong `pom.xml` của skeleton (artifact ID không tồn tại) | **Trung bình** | Xóa 3 dependency sai, thay bằng artifact test đúng của Spring Boot 4 theo `AGENTS.md` mục 5 |
| R-0-03 | Testcontainers pull image lần đầu chậm (~2-3 phút) trên CI | **Thấp** | GitHub Actions cache Docker layers; chỉ chậm lần đầu |
| R-0-04 | `flyway-database-postgresql` phải khai báo riêng từ Flyway 10+ | **Trung bình** | Kiểm tra BOM version và thêm đúng artifact ID theo Flyway docs |
| R-0-05 | AI có thể sinh code theo kiểu Spring Boot 3 khi viết backend | **Cao** | Tuân thủ `AGENTS.md` mục 5 trước khi viết bất kỳ code backend nào; kiểm tra tên package, starter và annotation |

### 10.3. Quyết định đã xác nhận (đóng câu hỏi)

| ID | Câu hỏi | Quyết định |
|---|---|---|
| Q-0-01 | Nhánh chính GitHub? | **`main`** — CI trigger trên `main` và `develop` |
| Q-0-02 | Bật Spring Boot Actuator? | **Có** — chỉ expose `health`; `show-details: never` để không lộ thông tin nội bộ |
| Q-0-03 | Redis password? | **Có** — đọc từ env `${REDIS_PASSWORD}`; bắt buộc có trong `.env` và docker-compose; không có default rõ ràng ngoài dev local |

> Không còn câu hỏi mở. Tài liệu đã hoàn chỉnh — sẵn sàng triển khai Phase 0.

---

## 11. Thứ tự Triển khai Đề xuất

Thứ tự thực hiện khi bắt đầu Phase 0 (sau khi câu hỏi ở mục 10.3 đã được trả lời):

1. **Chuẩn bị skeleton** (R-0-01, R-0-02): Đổi tên thư mục `Backend/` → `backend/`; sửa các dependency test sai trong `pom.xml` theo `AGENTS.md` mục 5.
2. **Tạo root files:** `docker-compose.yml`, `.env.example`, cập nhật `.gitignore`.
3. **Cấu hình backend:** `application.yml`, bổ sung dependency đầy đủ trong `pom.xml`.
4. **Tạo Flyway migration V1.**
5. **Implement `ApiResponse<T>`** và `GlobalExceptionHandler`.
6. **Implement `WebConfig`** (CORS) và `HealthController`.
7. **Implement `BaseIntegrationTest`** (Testcontainers) và các test case TC-HEALTH-01 đến TC-CORS-01.
8. **Chạy `(cd backend && ./mvnw clean verify)`** — xác nhận tất cả test PASS.
9. **Khởi tạo frontend** với Vite, cài Ant Design + Axios, implement gọi health endpoint.
10. **Tạo `.github/workflows/ci.yml`** — push lên GitHub, xác nhận CI xanh.
11. **Viết README** với hướng dẫn đầy đủ.
12. **Review checklist DoD** (mục 9) — báo cáo kết quả theo format AGENTS.md mục 3.

---

*Tài liệu soạn ngày 2026-10-19. Không sửa sau khi Phase 0 bắt đầu triển khai, trừ khi có quyết định thay đổi từ chủ dự án được ghi rõ lý do.*
