# E-Commerce Platform — Project Overview

> Tài liệu tổng quan định hướng dự án, dành cho cả người và AI.
> - Quy tắc làm việc với AI: xem [`AGENTS.md`](../AGENTS.md) ở thư mục gốc.
> - Đặc tả chi tiết từng phase: xem `docs/phase-N-*.md`.
> - Các quyết định nghiệp vụ chi tiết phải được chốt trong tài liệu phase tương ứng **trước khi** triển khai.

---

## 1. Mục tiêu dự án

Xây dựng website bán hàng fullstack làm dự án portfolio cho vị trí Java Backend Fresher. Dự án tập trung vào thiết kế API, bảo mật, quản lý dữ liệu, xử lý giao dịch và các tình huống thực tế trong hệ thống thương mại điện tử.

### Mục tiêu kỹ thuật

- Xây dựng REST API có cấu trúc rõ ràng, dễ bảo trì.
- Áp dụng authentication và authorization bằng JWT và RBAC.
- Quản lý schema database bằng Flyway.
- Sử dụng Redis cho các nhu cầu cache và quản lý trạng thái token phù hợp.
- Đảm bảo tính nhất quán của đơn hàng và tồn kho khi có request đồng thời.
- Viết unit test và integration test cho các luồng quan trọng.
- Chạy được môi trường phát triển bằng Docker Compose.
- Có CI cơ bản (build + test tự động).
- Cung cấp Swagger/OpenAPI và README để người khác có thể chạy thử dự án.

---

## 2. Phạm vi dự án

### 2.1. Trong phạm vi

- Đăng ký, đăng nhập, refresh token và logout.
- Phân quyền theo vai trò (RBAC); tài khoản admin đầu tiên được seed.
- Quản lý sản phẩm và danh mục.
- Danh sách sản phẩm có phân trang, lọc và tìm kiếm.
- Giỏ hàng.
- Địa chỉ giao hàng (lưu snapshot vào đơn hàng khi checkout).
- Checkout và tạo đơn hàng.
- Kiểm soát tồn kho và xử lý concurrency.
- Thanh toán giả lập.
- Tự hủy đơn hàng chưa thanh toán khi hết hạn.
- Trang quản trị cơ bản (sản phẩm, danh mục, đơn hàng).
- Frontend React kết nối backend.
- Test, CI, tài liệu API và hướng dẫn triển khai.

### 2.2. Ngoài phạm vi

- Coupon/voucher và chương trình khuyến mãi.
- Review, rating và bình luận sản phẩm.
- Multi-vendor/marketplace.
- Biến thể sản phẩm (variant): 1 Product = 1 SKU.
- Tích hợp đơn vị vận chuyển thực tế.
- Cổng thanh toán thật, hoàn tiền.
- Xác minh email, quên mật khẩu, đăng nhập mạng xã hội.
- Đa tiền tệ, đa ngôn ngữ.
- Microservices hoặc hệ thống phân tán phức tạp không cần thiết cho phạm vi portfolio.

Không tự mở rộng phạm vi nếu chưa được yêu cầu.

---

## 3. Tech stack

| Lớp | Công nghệ | Phiên bản | Vai trò |
|---|---|---|---|
| Backend | Java | 21 (LTS) | Ngôn ngữ chính |
| Backend framework | Spring Boot | 4.1.x (bản 4.x mới nhất khi khởi tạo; xem mục 3.1) | Xây dựng ứng dụng backend |
| Build tool | Maven (dùng Maven Wrapper `Backend/mvnw`) | — | Build, quản lý dependency |
| Security | Spring Security | theo Spring Boot | Xác thực và phân quyền |
| Persistence | Spring Data JPA (Hibernate) | theo Spring Boot | Truy cập dữ liệu |
| Database | PostgreSQL | 16 | Lưu dữ liệu nghiệp vụ |
| Database migration | Flyway | theo Spring Boot | Quản lý phiên bản schema |
| Cache/token support | Redis | 7 | Cache hoặc lưu trạng thái token |
| Authentication | JWT access token + refresh token | — | Xác thực người dùng |
| Authorization | RBAC | — | Kiểm soát quyền truy cập |
| Frontend | React + Vite | React 18+, Node 20+ | Giao diện người dùng |
| Routing | React Router | — | Điều hướng frontend |
| HTTP client | Axios | — | Gọi REST API |
| UI | UI component library có sẵn (xem mục 4.7) | chốt ở Phase 0 | Giao diện, hạn chế viết CSS tay |
| API documentation | Swagger/OpenAPI (springdoc) | — | Mô tả và kiểm thử API |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL, Redis) | — | Unit và integration test |
| CI | GitHub Actions | — | Build và test tự động |
| Environment | Docker Compose | — | Chạy các dịch vụ phục vụ phát triển |

Quy định:
- Phiên bản cụ thể được ghi vào `pom.xml` và `docker-compose.yml` ở Phase 0; cập nhật bảng này cho khớp.
- Không dùng H2 cho integration test; dùng PostgreSQL thật qua Testcontainers để hành vi lock và transaction giống môi trường chạy thật.
- Không thêm thư viện hoặc công nghệ ngoài stack nếu chưa được yêu cầu hoặc chưa thống nhất.

### 3.1. Lý do chọn Spring Boot 4.x

- Các nhánh Spring Boot 3.x (kể cả 3.5) đã hết hỗ trợ mã nguồn mở từ 30/06/2026, nên không còn bản vá bảo mật miễn phí.
- Spring Boot 4.0 sẽ hết hỗ trợ mã nguồn mở vào 31/12/2026, vì vậy dự án khởi tạo từ tháng 10/2026 dùng nhánh 4.1.x.
- Spring Boot 4 chạy trên Spring Framework 7 và Spring Security 7, tối thiểu Java 17 (dự án dùng Java 21).
- Kiểm tra số phiên bản chính xác trên start.spring.io khi tạo project, và ghi lại vào `Backend/pom.xml`.
- Các thư viện ngoài Spring (springdoc, Testcontainers, thư viện JWT) phải chọn bản tương thích với Spring Boot 4. Xác nhận ở Phase 0 trước khi thêm vào `pom.xml`.
- Những điểm khác biệt so với Spring Boot 3 mà AI hay sinh sai được liệt kê trong `AGENTS.md`, mục 5.

---

## 4. Quy ước kiến trúc và code

### 4.1. Kiến trúc backend

- Ưu tiên modular monolith, chia package theo feature.
- Mỗi feature quản lý các thành phần liên quan trong cùng khu vực package.
- Controller tiếp nhận HTTP request và trả response.
- Service xử lý nghiệp vụ.
- Repository truy cập dữ liệu.
- DTO dùng cho request/response; không trả JPA Entity trực tiếp ra API.
- Không đưa nghiệp vụ quan trọng vào Controller.
- Chỉ tạo abstraction khi có nhu cầu rõ ràng; tránh thiết kế quá mức.

### Cấu trúc repository

Một repository (monorepo) gồm hai thư mục mã nguồn là `Backend/` và `frontend/`. Tài liệu, hạ tầng và cấu hình dùng chung nằm ở thư mục gốc.

```
ecommerce-platform/
├── AGENTS.md                      # quy tắc làm việc với AI
├── README.md
├── docker-compose.yml             # PostgreSQL, Redis (dịch vụ phục vụ phát triển)
├── .env.example                   # biến môi trường mẫu, không chứa secret
├── .github/workflows/             # CI (GitHub Actions)
├── docs/                          # tài liệu dự án (xem mục 11)
├── Backend/                       # tên chuẩn của thư mục mã nguồn backend (xem D-0-07 ở phase-0)
│   ├── pom.xml
│   ├── mvnw
│   └── src/
│       ├── main/java/com/app/backend/
│       ├── main/resources/db/migration/   # Flyway migration
│       └── test/
└── frontend/
    ├── package.json
    └── src/
```

Quy định:
- Toàn bộ mã nguồn backend chỉ nằm trong `Backend/`, frontend chỉ nằm trong `frontend/`. Không tạo mã nguồn ở thư mục gốc.
- Backend và frontend build, test và chạy độc lập; chúng chỉ giao tiếp qua REST API.
- Nếu dùng Claude Code, thêm `CLAUDE.md` ở thư mục gốc chỉ chứa một dòng `@AGENTS.md` để không phải giữ hai bản nội dung.

Ví dụ package backend tham khảo (nằm trong `Backend/src/main/java/`):

```
com.app.backend
├── common
│   ├── config
│   ├── exception
│   ├── response
│   └── security
├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
├── user
├── address
├── product
├── category
├── cart
├── order
└── payment
```

Cấu trúc trên là định hướng, có thể điều chỉnh theo code thực tế nhưng phải nhất quán.

### 4.2. Response API

Response thành công và thất bại sử dụng cấu trúc thống nhất:

```json
{
  "success": true,
  "data": {},
  "message": "Request processed successfully",
  "errorCode": null,
  "timestamp": "2026-01-01T00:00:00Z"
}
```

Quy ước:
- `success`: request có được xử lý thành công hay không.
- `data`: dữ liệu trả về; có thể là `null` khi phù hợp.
- `message`: thông báo dễ hiểu.
- `errorCode`: mã lỗi ổn định để frontend xử lý; `null` nếu thành công. Mọi mã lỗi được liệt kê trong `docs/error-codes.md`; không tự tạo mã mới ngoài danh mục này.
- `timestamp`: thời điểm phản hồi theo UTC, định dạng ISO 8601.

Quy định thêm:
- Dùng HTTP status code đúng ngữ nghĩa.
- Lỗi validation phải chỉ ra trường không hợp lệ khi phù hợp.
- Không trả stack trace, thông tin database hoặc dữ liệu nhạy cảm cho client.
- Không dùng HTTP 200 cho mọi trường hợp lỗi.

### 4.3. Kiểu dữ liệu và thời gian

- Tiền trong Java dùng `BigDecimal`.
- Tiền trong PostgreSQL dùng `DECIMAL(19,2)`.
- Không dùng `double` hoặc `float` để tính tiền.
- Thống nhất lưu và trao đổi thời gian theo UTC.
- Dùng `Instant` cho timestamp tuyệt đối; chuyển sang múi giờ hiển thị ở frontend khi cần.
- ID, độ dài chuỗi, precision/scale và quy tắc null phải được xác định trong thiết kế từng entity.

### 4.4. Validation và exception

- Dùng validation ở tầng request DTO cho dữ liệu đầu vào.
- Service vẫn phải bảo vệ các quy tắc nghiệp vụ quan trọng.
- Dùng global exception handler để chuẩn hóa lỗi.
- Phân biệt lỗi validation, tài nguyên không tồn tại, xung đột nghiệp vụ, không xác thực và không đủ quyền.
- Không bắt exception rồi bỏ qua hoặc trả kết quả thành công giả.

### 4.5. Logging và cấu hình

- Đưa cấu hình môi trường ra environment variables hoặc file cấu hình phù hợp.
- Không commit mật khẩu, secret JWT, private key hoặc token.
- Không ghi password, access token, refresh token hay dữ liệu nhạy cảm vào log.
- Dùng log có ngữ cảnh; cân nhắc request/correlation ID để truy vết lỗi.
- Không để thông tin nhạy cảm xuất hiện trong message trả về client.

### 4.6. Quy ước đặt tên

| Đối tượng | Quy ước | Ví dụ |
|---|---|---|
| Bảng DB | `snake_case`, số nhiều (tránh trùng từ khóa SQL như `order`) | `users`, `products`, `orders`, `order_items` |
| Cột DB | `snake_case` | `created_at`, `unit_price` |
| Khóa chính | `id`, kiểu `BIGINT` tự tăng | — |
| Cột audit | `created_at`, `updated_at` (kiểu timestamp UTC) trên mọi bảng nghiệp vụ | — |
| File migration | `V<số>__<mô_tả_snake_case>.sql` | `V1__create_users_and_roles.sql` |
| Java class | `PascalCase`; hậu tố rõ nghĩa | `OrderService`, `CreateOrderRequest`, `OrderResponse` |
| Java method/field | `camelCase` | — |
| URL API | tiền tố `/api/v1`, danh từ số nhiều, `kebab-case` | `/api/v1/products`, `/api/v1/orders/{id}` |
| JSON field | `camelCase` | `unitPrice` |
| Mã lỗi | `UPPER_SNAKE_CASE` | `OUT_OF_STOCK`, `TOKEN_EXPIRED` |
| ID business rule (trong tài liệu phase) | `BR-<MODULE>-<số>` | `BR-CHK-01` |

### 4.7. Quy ước frontend

Frontend phục vụ việc demo backend, không phải trọng tâm đánh giá của dự án.

- Dùng UI component library thay vì tự viết giao diện; hạn chế CSS tùy biến.
- Quản lý state bằng React state/Context; không thêm Redux hoặc Zustand.
- Chỉ có một module Axios dùng chung: gắn token, tự refresh khi hết hạn, chuyển lỗi API thành thông báo dựa trên `errorCode`.
- Không tối ưu thẩm mỹ, responsive hay animation nếu chưa được yêu cầu.
- Khi trễ tiến độ, cắt phạm vi frontend trước; không cắt test hoặc tài liệu backend.

---

## 5. Bảo mật và quản lý token

### 5.1. Authentication và authorization

- Authentication xác định người dùng là ai.
- Authorization xác định người dùng được phép làm gì.
- Dùng Spring Security để bảo vệ API.
- Dùng RBAC để kiểm tra quyền theo vai trò.
- Backend là nơi quyết định quyền truy cập; ẩn nút trên frontend không thay thế được kiểm tra quyền ở backend.
- Mặc định từ chối truy cập nếu endpoint cần bảo vệ nhưng chưa có quy tắc cấp quyền rõ ràng.

### 5.2. Access token và refresh token

Tài liệu Phase 1 phải xác định cụ thể (giá trị mặc định ở mục 7):
- Thời hạn access token và refresh token.
- Cách cấp mới access token.
- Refresh token có được rotation hay không và cách xử lý token cũ.
- Cách lưu, tra cứu và thu hồi refresh token.
- Cơ chế logout và thời gian hiệu lực của blacklist/revocation.
- Cách xử lý token hết hạn, sai chữ ký, bị thu hồi hoặc không hợp lệ.
- Cách quản lý secret và key giữa các môi trường.

Không tự chọn thời hạn token hoặc chính sách lưu trữ ngoài những gì đã ghi ở mục 7 hoặc trong tài liệu Phase 1.

Lưu ý: JWT có chữ ký không tự động bị thu hồi. Nếu yêu cầu logout có hiệu lực ngay, backend cần cơ chế thu hồi và kiểm tra phù hợp.

---

## 6. Quy tắc dữ liệu và nghiệp vụ

### 6.1. Sản phẩm và giá

- Backend không tin giá hoặc tổng tiền do frontend gửi lên.
- Khi checkout, backend đọc giá từ nguồn dữ liệu đáng tin cậy.
- `OrderItem` lưu snapshot tên/mã sản phẩm, đơn giá và số lượng tại thời điểm đặt hàng.
- Đơn hàng lưu snapshot địa chỉ giao hàng tại thời điểm đặt; sửa hoặc xóa địa chỉ sau đó không làm đổi đơn cũ.
- Việc sản phẩm bị ẩn, ngừng bán hoặc hết hàng phải được xử lý theo quy tắc đã thống nhất.

### 6.2. Đơn hàng và thanh toán

- Trạng thái đơn hàng và trạng thái thanh toán là hai khái niệm riêng.
- Các trạng thái và chuyển đổi trạng thái hợp lệ phải được ghi rõ trong tài liệu Phase 4 và Phase 5.
- Không cho phép chuyển trạng thái tùy ý chỉ vì frontend gửi request.
- Các thao tác tạo đơn, ghi nhận thanh toán, hủy đơn và hoàn tồn kho phải xử lý được request lặp.
- Mọi quyết định về thời điểm giữ/trừ tồn kho, thời gian chờ thanh toán và xử lý callback phải được xác định trong tài liệu phase tương ứng (giá trị mặc định ở mục 7).

### 6.3. Transaction và concurrency

- Xác định rõ ranh giới transaction cho các nghiệp vụ cập nhật nhiều bản ghi.
- Chọn chiến lược kiểm soát concurrency phù hợp với mô hình dữ liệu và database.
- Không giả định rằng chỉ kiểm tra tồn kho trước khi tạo đơn là đủ an toàn khi có request đồng thời.
- Không để tồn kho âm.
- Nếu một thao tác nghiệp vụ thất bại, dữ liệu liên quan phải giữ được trạng thái nhất quán.
- Tránh thực hiện side effect bên ngoài transaction theo cách khiến trạng thái database và trạng thái bên ngoài lệch nhau mà không có cơ chế xử lý.

Chiến lược cụ thể (pessimistic locking, optimistic locking hoặc cập nhật có điều kiện) phải được giải thích trong tài liệu Phase 4, kể cả lý do chọn và so sánh với phương án còn lại.

**Ranh giới transaction của checkout (mặc định):**

1. Một transaction duy nhất bao cả việc trừ kho và tạo đơn. Không tách trừ kho thành transaction riêng, vì sẽ có trường hợp đã trừ kho mà chưa có đơn.
2. Trong transaction, **đọc trước, khóa sau**: đọc giỏ hàng, sản phẩm, địa chỉ và tính tổng tiền trước; chỉ lấy khóa tồn kho ở cuối giai đoạn chuẩn bị.
3. Lấy khóa tất cả dòng tồn kho cần dùng bằng một truy vấn có `ORDER BY product_id`, rồi trừ kho, tạo `Order`/`OrderItem`, xóa giỏ hàng và commit.
4. Từ lúc giữ khóa đến lúc commit không gọi dịch vụ bên ngoài, không chờ, không xử lý nặng. Khóa được giữ đến hết transaction nên thời gian giữ khóa chính là độ dài phần còn lại của transaction.

### 6.4. Idempotency

Xem xét idempotency cho các luồng có nguy cơ được gửi lại:
- Tạo đơn hàng/checkout.
- Ghi nhận kết quả thanh toán.
- Tác vụ tự hủy đơn hàng.
- Hoàn trả tồn kho.

Một sự kiện được xử lý lặp không được tạo ra nhiều đơn, ghi nhận thanh toán nhiều lần hoặc hoàn kho nhiều lần. Cơ chế và phạm vi áp dụng phải được xác định trong tài liệu Phase 4 và Phase 5.

### 6.5. Xung đột giữa thanh toán, hủy đơn và hết hạn

- Mọi chuyển trạng thái đơn là câu lệnh cập nhật có điều kiện, ví dụ `UPDATE orders SET status = 'PAID' WHERE id = ? AND status = 'PENDING'`; chỉ coi là thành công khi số dòng được cập nhật là 1.
- Thanh toán, hủy thủ công và tác vụ hết hạn đều đi qua cơ chế này. Bên chạy trước thắng; bên sau nhận 0 dòng và xử lý theo chính sách, không ghi đè.
- Hoàn kho chỉ thực hiện cho những đơn mà chính transaction đó vừa chuyển sang `CANCELLED`, và nằm cùng transaction với việc đổi trạng thái.
- Thanh toán thành công đến sau khi đơn đã `CANCELLED`: không khôi phục đơn (kho đã được giải phóng và có thể đã bán cho người khác). Ghi nhận payment, đánh dấu cần hoàn tiền thủ công và ghi log mức cảnh báo.
- Tác vụ hết hạn chỉ hủy đơn quá `expires_at` cộng thêm khoảng ân hạn ngắn để giảm xung đột.
- Tính đúng đắn không phụ thuộc vào việc chỉ có một instance chạy tác vụ hết hạn. Distributed lock (ví dụ ShedLock) chỉ giúp giảm quét trùng và không nằm trong phạm vi hiện tại.

---

## 7. Quyết định mặc định

Đây là điểm xuất phát để AI và người viết tài liệu phase không phải đoán. Có thể thay đổi, nhưng chỉ thay đổi trong tài liệu phase tương ứng, kèm lý do, và cập nhật lại bảng này nếu ảnh hưởng nhiều phase.

| Chủ đề | Mặc định | Chốt trong |
|---|---|---|
| Mô hình sản phẩm | Không variant; 1 Product = 1 SKU | Phase 2 |
| Tồn kho | Lưu ở bảng riêng (quan hệ 1-1 với product) để khóa dòng tồn kho không chặn thao tác sửa thông tin sản phẩm | Phase 2, 4 |
| Sản phẩm ngừng bán | Dùng trạng thái (ví dụ `ACTIVE`/`INACTIVE`), không xóa cứng sản phẩm đã xuất hiện trong đơn | Phase 2 |
| Tiền tệ | Một đơn vị tiền tệ (VND); lưu `DECIMAL(19,2)` | Phase 2 |
| Phân trang và sort | `page` bắt đầu từ 0, `size` mặc định 20, tối đa 50. Sort theo whitelist tên trường của API (mặc định: `price`, `name`, `createdAt`), ánh xạ sang trường entity ở service. Trường ngoài danh sách bị từ chối với 400, không để lỗi của Spring Data thành 500 và không lộ tên cột nội bộ | Phase 2 |
| Vai trò | `ROLE_USER`, `ROLE_ADMIN`; đăng ký công khai chỉ tạo `ROLE_USER`; admin đầu tiên được seed qua migration/biến môi trường | Phase 1 |
| Ký JWT | HS256, secret đọc từ biến môi trường | Phase 1 |
| Thời hạn token | Access token 15 phút; refresh token 7 ngày | Phase 1 |
| Access token | Chứa `jti` và `sid` (mã phiên đăng nhập). Refresh thành công cấp access token mới nhưng **không** blacklist access token cũ: cùng một phiên, tối đa 15 phút còn hiệu lực | Phase 1 |
| Refresh token | Có rotation; lưu dạng hash trong Redis kèm TTL. Dùng lại refresh token cũ đã rotate thì thu hồi cả phiên (`sid`) | Phase 1 |
| Logout và thu hồi phiên | Đưa `sid` vào blacklist Redis với TTL bằng thời hạn access token (15 phút) và xóa refresh token của phiên. Mỗi request kiểm tra `sid` có bị thu hồi không, nên mọi access token cùng phiên bị từ chối ngay | Phase 1 |
| Giỏ hàng | Lưu phía server, yêu cầu đăng nhập; không giữ tồn kho | Phase 3 |
| Hết hàng | Chặn khi thêm vào giỏ và khi checkout; không backorder | Phase 3, 4 |
| Thời điểm trừ kho | Trừ kho ngay khi tạo đơn `PENDING` (giữ hàng) | Phase 4 |
| Concurrency | Pessimistic lock: khóa các dòng tồn kho cần dùng bằng một truy vấn `ORDER BY product_id ... FOR UPDATE` (thứ tự cố định để tránh deadlock). Phương án so sánh trong tài liệu: cập nhật có điều kiện (`UPDATE ... SET quantity = quantity - ? WHERE product_id = ? AND quantity >= ?`) và optimistic lock (`@Version`) | Phase 4 |
| Transaction checkout | Một transaction duy nhất cho trừ kho và tạo đơn; đọc trước, khóa sau; giữ khóa ngắn nhất có thể (xem mục 6.3) | Phase 4 |
| Trạng thái đơn | `PENDING` → `PAID` → `SHIPPING` → `COMPLETED`; `PENDING` → `CANCELLED`. Đơn đã `PAID` không hủy trong phạm vi dự án | Phase 4, 5 |
| Trạng thái thanh toán | Tách riêng khỏi trạng thái đơn (ví dụ `PENDING`/`SUCCESS`/`FAILED`), kèm cờ cần hoàn tiền thủ công cho thanh toán đến muộn sau khi đơn bị hủy | Phase 5 |
| Idempotency checkout | Header `Idempotency-Key` do client gửi; cùng key và cùng user trả lại kết quả của lần đầu | Phase 4 |
| Idempotency thanh toán | Mỗi giao dịch có mã tham chiếu duy nhất; callback lặp không đổi kết quả đã ghi | Phase 5 |
| Mock payment | Endpoint giả lập callback hỗ trợ kết quả thành công/thất bại | Phase 5 |
| Hết hạn đơn | 15 phút kể từ khi tạo đơn (`expires_at`); tác vụ `@Scheduled` chạy mỗi phút, chỉ hủy đơn quá hạn cộng ân hạn 30 giây. Chuyển trạng thái bằng cập nhật có điều kiện nên chạy lặp hoặc chạy 2 instance vẫn không hoàn kho hai lần | Phase 5 |
| Xung đột thanh toán và hết hạn | Bên chạy trước thắng (cập nhật có điều kiện); thanh toán đến muộn không khôi phục đơn, được ghi nhận và đánh dấu cần hoàn tiền thủ công (xem mục 6.5) | Phase 5 |
| Hoàn kho | Hoàn khi đơn `PENDING` bị hủy (thủ công, thanh toán thất bại hoặc hết hạn), đúng một lần | Phase 4, 5 |
| UI frontend | Dùng UI component library (đề xuất Ant Design cho form, bảng, modal); chốt ở Phase 0 | Phase 0 |
| State và gọi API frontend | React Context + một module Axios dùng chung; không dùng Redux | Phase 1 |

---

## 8. Chiến lược kiểm thử và CI

- **Unit test:** JUnit 5 + Mockito cho logic trong service (tính tiền, kiểm tra trạng thái).
- **Integration test:** Spring Boot Test + Testcontainers (PostgreSQL, Redis) cho repository, security và luồng API.
- **Concurrency test:** chạy nhiều luồng cùng mua một sản phẩm trên PostgreSQL thật; kiểm tra tồn kho không âm và số đơn thành công không vượt tồn kho.
- **Race test (Phase 5):** chạy song song thanh toán và tác vụ hết hạn trên cùng một đơn; kiểm tra đúng một bên thắng và kho được hoàn đúng một lần hoặc không hoàn.
- **Test bảo mật:** mỗi endpoint được bảo vệ cần có ít nhất một test được phép và một test bị từ chối (chưa đăng nhập, sai vai trò).
- **CI (GitHub Actions):** mỗi lần push/PR chạy hai job riêng: build và toàn bộ test cho `Backend/`, build (và test nếu có) cho `frontend/`. Thiết lập ở Phase 0 và giữ xanh trong suốt dự án.

---

## 9. Roadmap và tiêu chí hoàn thành

Backend là trọng tâm. Mỗi phase có thêm một lát cắt frontend tối thiểu (theo mục 4.7) để luôn có thứ chạy và demo được; nếu trễ tiến độ thì cắt frontend trước, phần backend vẫn demo được bằng Swagger. Trạng thái của từng phase chỉ ghi ở mục 13.

### Phase 0 — Khởi tạo

Mục tiêu: dựng skeleton dự án và môi trường phát triển.

Backend/hạ tầng:
- Tạo cấu trúc repository gồm `Backend/`, `frontend/`, `docs/` (xem mục 4.1).
- Khởi tạo backend Java 21 + Spring Boot 4.1.x (Maven) trong `Backend/`.
- Tạo `docker-compose.yml` ở thư mục gốc (PostgreSQL, Redis).
- Kết nối PostgreSQL; cấu hình Flyway.
- Tạo health endpoint.
- Cấu hình CI cơ bản (GitHub Actions).
- Tạo README ban đầu và `.env.example` ở thư mục gốc, không chứa secret.

Frontend:
- Khởi tạo React + Vite trong `frontend/`, gọi được health endpoint của backend (xác nhận CORS).

Definition of Done:
- `docker compose up` khởi động được các dịch vụ cần thiết.
- Backend kết nối database thành công.
- Flyway migration chạy thành công.
- Health endpoint phản ánh trạng thái ứng dụng phù hợp.
- Frontend hiển thị được kết quả gọi backend.
- CI chạy xanh trên nhánh chính.
- Hướng dẫn chạy dự án được ghi trong README.

### Phase 1 — Authentication & Authorization

Mục tiêu: hoàn thiện xác thực và phân quyền.

Backend:
- Đăng ký, đăng nhập.
- Cấp access token và refresh token; refresh token.
- Logout và thu hồi token theo thiết kế.
- RBAC, bảo vệ endpoint, seed tài khoản admin.
- Chuẩn hóa lỗi bảo mật.

Frontend:
- Trang đăng ký/đăng nhập, lưu và gắn token, tự refresh khi hết hạn, route cần đăng nhập.

Definition of Done:
- Luồng đăng ký/đăng nhập hợp lệ hoạt động.
- Mật khẩu không được lưu ở dạng plaintext.
- Token sai, hết hạn hoặc bị thu hồi được xử lý đúng.
- Endpoint được bảo vệ theo vai trò đã định nghĩa.
- Kiểm thử được cả trường hợp được phép và bị từ chối.
- Chính sách refresh/logout được mô tả trong tài liệu phase.

### Phase 2 — Product & Category

Mục tiêu: quản lý danh mục và sản phẩm.

Backend:
- CRUD category và product theo quyền.
- Danh sách sản phẩm có phân trang, lọc và tìm kiếm.
- Tồn kho ban đầu của sản phẩm.
- Validation dữ liệu.
- Quy tắc hiển thị/ngừng bán sản phẩm.

Frontend:
- Trang danh sách (phân trang, lọc, tìm kiếm có debounce) và chi tiết sản phẩm; trang quản trị sản phẩm/danh mục cơ bản.

Definition of Done:
- Người dùng không đủ quyền không thể thực hiện thao tác quản trị.
- API phân trang và lọc hoạt động theo hợp đồng đã định nghĩa.
- Dữ liệu đầu vào không hợp lệ được từ chối.
- Xử lý đúng trường hợp tài nguyên không tồn tại.
- Có test cho nghiệp vụ và các trường hợp lỗi chính.

### Phase 3 — Cart

Mục tiêu: triển khai giỏ hàng.

Backend:
- Xem giỏ hàng, thêm sản phẩm, cập nhật số lượng, xóa sản phẩm.
- Kiểm tra sản phẩm còn được phép mua.
- Tính subtotal/total theo quy tắc đã thống nhất.
- Quản lý địa chỉ giao hàng của người dùng.

Frontend:
- Trang giỏ hàng, nút thêm vào giỏ, quản lý địa chỉ.

Definition of Done:
- Người dùng chỉ thao tác được trên giỏ hàng thuộc quyền của mình.
- Số lượng phải hợp lệ.
- Giá trị tiền được tính bằng `BigDecimal` ở backend.
- Xử lý đúng sản phẩm không tồn tại, ngừng bán hoặc không hợp lệ.
- Có test cho các thao tác giỏ hàng và quyền truy cập.

### Phase 4 — Checkout, Order & Concurrency

Mục tiêu: tạo đơn hàng an toàn và giữ tính nhất quán tồn kho.

Backend:
- Checkout từ giỏ hàng.
- Backend xác minh sản phẩm, giá và số lượng; tính tổng tiền phía server.
- Tạo `Order` và `OrderItem`; lưu snapshot dữ liệu (sản phẩm, giá, địa chỉ) tại thời điểm đặt hàng.
- Kiểm soát tồn kho và request đồng thời.
- Định nghĩa trạng thái đơn hàng.
- Xử lý rollback và request lặp (idempotency).

Frontend:
- Trang checkout, danh sách và chi tiết đơn hàng của người dùng; admin xem danh sách đơn.

Definition of Done:
- Không tin tổng tiền do frontend gửi lên.
- Không tạo đơn thành công nếu vi phạm quy tắc tồn kho.
- Test concurrency chứng minh tồn kho không âm và không bán vượt số lượng có sẵn theo mô hình đã chọn.
- Lỗi giữa transaction không để lại dữ liệu nghiệp vụ dở dang.
- Giá và thông tin cần thiết của `OrderItem` không bị thay đổi ngược khi sản phẩm được cập nhật sau này.
- Có test cho checkout thành công, thất bại và request lặp.
- Chiến lược concurrency được giải thích trong tài liệu.

### Phase 5 — Mock Payment & Order Expiration

Mục tiêu: mô phỏng thanh toán và xử lý đơn chưa thanh toán quá hạn.

Backend:
- Tạo payment giả lập; mô phỏng thanh toán thành công và thất bại.
- Xác thực điều kiện trước khi cập nhật trạng thái.
- Định nghĩa thời hạn thanh toán; tự hủy đơn quá hạn; xử lý tồn kho khi đơn bị hủy.
- Chống xử lý lặp và xử lý xung đột giữa thanh toán với tác vụ hết hạn.
- Admin cập nhật trạng thái giao hàng (`SHIPPING`, `COMPLETED`) theo state machine.

Frontend:
- Màn hình thanh toán giả lập, hiển thị thời gian còn lại của đơn, trạng thái đơn hết hạn/đã hủy.

Definition of Done:
- Trạng thái payment và order được cập nhật theo quy tắc đã định nghĩa.
- Không ghi nhận thanh toán thành công nhiều lần cho cùng một giao dịch.
- Tác vụ hết hạn có thể chạy lặp mà không hủy/hoàn kho sai.
- Xử lý được tình huống payment và expiration xảy ra gần như đồng thời.
- Tồn kho được giải phóng đúng theo chính sách đã chọn.
- Có test cho thành công, thất bại, hết hạn, request lặp và xung đột trạng thái.

### Phase 6 — Hoàn thiện & Deployment

Mục tiêu: hoàn thiện trải nghiệm sử dụng và khả năng chạy thử dự án.

Phạm vi công việc:
- Hoàn thiện trang quản trị cơ bản.
- Xử lý loading, empty state và lỗi API nhất quán trên toàn bộ frontend.
- Cấu hình môi trường cho triển khai.
- Hoàn thiện README và tài liệu API.
- Dữ liệu demo.

Definition of Done:
- Các luồng chính chạy end-to-end.
- Frontend xử lý được lỗi xác thực và lỗi API.
- Không phụ thuộc vào secret hardcode trong source.
- Hướng dẫn cấu hình và chạy dự án rõ ràng.
- README nêu chức năng, kiến trúc, công nghệ, cách chạy, cách test và các giới hạn đã biết.
- Có dữ liệu demo an toàn hoặc hướng dẫn tạo dữ liệu ban đầu.

---

## 10. Definition of Done chung

Một phase chỉ được đánh dấu Done khi:

- [ ] Chức năng trong phạm vi phase đáp ứng yêu cầu đã thống nhất.
- [ ] Validation và các trường hợp lỗi chính được xử lý.
- [ ] Có unit test và/hoặc integration test phù hợp với thay đổi.
- [ ] Build thành công và các test liên quan đã chạy; nếu không chạy được phải nêu rõ lý do.
- [ ] CI chạy xanh.
- [ ] API, response và HTTP status nhất quán; mã lỗi mới đã được thêm vào `docs/error-codes.md`.
- [ ] Kiểm tra quyền truy cập ở backend khi có liên quan.
- [ ] Flyway migration được cập nhật khi schema thay đổi.
- [ ] Swagger/OpenAPI và tài liệu phase được cập nhật khi cần.
- [ ] Không làm hỏng các phase đã hoàn thành.
- [ ] Không commit secret hoặc dữ liệu nhạy cảm.
- [ ] README được cập nhật khi có thay đổi ảnh hưởng đến cách chạy.

Không đánh dấu Done chỉ vì code đã được sinh ra hoặc ứng dụng khởi động thành công.

---

## 11. Tài liệu dự án

```
AGENTS.md                              # ở thư mục gốc repo; quy tắc làm việc với AI
docs/                                  # ở thư mục gốc repo
├── project-overview.md                # file này
├── error-codes.md                     # danh mục errorCode dùng chung
├── phase-0-foundation.md
├── phase-1-auth.md
├── phase-2-product-category.md
├── phase-3-cart.md
├── phase-4-checkout-order-concurrency.md
├── phase-5-payment-expiration.md
└── phase-6-frontend-deployment.md
```

Mỗi tài liệu phase nên có:
- Mục tiêu và phạm vi.
- Quy tắc nghiệp vụ (đánh số `BR-<MODULE>-<số>`).
- API contract.
- Data model và ràng buộc.
- Luồng thành công và thất bại (kèm Given/When/Then cho các luồng cốt lõi).
- Bảo mật và phân quyền.
- Transaction/concurrency nếu có.
- Kế hoạch kiểm thử.
- Definition of Done.
- Các quyết định đã chốt (kèm lý do) và các câu hỏi còn mở.

Chỉ viết tài liệu cho phase sắp làm; không viết trước toàn bộ.

---

## 12. Quy tắc làm việc với AI

Xem [`AGENTS.md`](../AGENTS.md). Quy tắc chỉ được ghi ở đó để tránh hai bản lệch nhau.

---

## 13. Trạng thái tổng thể

Trạng thái dùng thống nhất: `Not started`, `In progress`, `Blocked`, `Done`.

| Phase | Tên | Trạng thái | Ghi chú |
|---|---|---|---|
| 0 | Khởi tạo | Not started | |
| 1 | Authentication & Authorization | Not started | |
| 2 | Product & Category | Not started | |
| 3 | Cart | Not started | |
| 4 | Checkout, Order & Concurrency | Not started | |
| 5 | Mock Payment & Order Expiration | Not started | |
| 6 | Hoàn thiện & Deployment | Not started | |

Quy tắc cập nhật: chỉ chuyển trạng thái khi có bằng chứng kiểm tra phù hợp (test đã chạy, CI xanh). Nếu phase bị chặn bởi yêu cầu chưa rõ hoặc lỗi phụ thuộc, dùng `Blocked` và ghi lý do vào cột Ghi chú.

---

## Nguyên tắc cốt lõi

Ưu tiên tính đúng đắn, tính nhất quán dữ liệu, bảo mật và khả năng giải thích thiết kế hơn số lượng công nghệ.
