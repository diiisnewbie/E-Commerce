# Phase 2 — Product & Category

> **Trạng thái:** Đã duyệt 2026-10-21 — chủ dự án chốt toàn bộ O-2-01 → O-2-05 và các điểm review (A/B/C/D) theo đề xuất trong tài liệu; triển khai theo mục 11.
> **Phụ thuộc:** Phase 1 (Đã Done — JWT/RBAC; `SecurityConfig` đã có `/api/v1/admin/**` → `hasRole("ADMIN")`, `open-in-view: false` ở `application.yml`, `GlobalExceptionHandler` đã có handler validation/404/405).
> **Tài liệu liên quan:** [`docs/project-overview.md`](./project-overview.md) (mục 4, 6.1, 7, 9), [`docs/phase-1-auth.md`](./phase-1-auth.md), [`AGENTS.md`](../AGENTS.md)
> **Không thêm dependency mới** — JPA Specification, Bean Validation đều đã trong starter hiện có.

---

## 1. Mục tiêu & Phạm vi

### 1.1. Mục tiêu

Quản lý danh mục (category) và sản phẩm (product): CRUD theo vai trò ADMIN, danh sách sản phẩm công khai có phân trang/lọc/tìm kiếm/sắp xếp ổn định, tồn kho ban đầu, validation chặt và quy tắc ngừng bán bằng trạng thái. Đây là catalog mà Phase 3 (giỏ hàng) và Phase 4 (checkout) sẽ đọc giá/tồn kho từ.

### 1.2. Trong phạm vi Phase 2

| Hạng mục | Chi tiết |
|---|---|
| Category CRUD | Admin tạo/sửa/xóa; danh sách công khai; xóa chỉ được khi chưa có sản phẩm |
| Product CRUD | Admin tạo/sửa (SKU bất biến, đổi giá/category/status, **kèm optimistic lock**); ngừng bán bằng `INACTIVE`, không hard delete |
| Danh sách công khai | `GET /api/v1/products` — phân trang ổn định (khóa phụ `id`), lọc danh mục/khoảng giá, tìm kiếm theo tên, sort whitelist |
| Admin list & chi tiết | `GET /api/v1/admin/products` (kèm `status`/`stock`/`version`), `GET /api/v1/admin/products/{id}` |
| Đổi trạng thái | `PATCH /api/v1/admin/products/{id}/status` — idempotent, không cần gửi cả form |
| Tồn kho ban đầu | Bảng `product_inventory` 1-1 với product (chốt overview mục 7); `initialStock` khi tạo |
| Seed demo | `db/seed/R__demo_catalog.sql` repeatable — **chỉ nạp ở profile local** (xem D-2-11) |
| Mã lỗi mới | `PRODUCT_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `SKU_ALREADY_EXISTS`, `CATEGORY_NAME_ALREADY_EXISTS`, `CATEGORY_HAS_PRODUCTS`, `CONCURRENT_MODIFICATION` (6 mã) |
| Frontend | Danh sách + chi tiết công khai (phân trang, lọc, search debounce, chống stale response); admin sản phẩm/danh mục (form giữ `version`, SKU khóa khi sửa, nút ngừng bán dùng PATCH) |

### 1.3. Ngoài phạm vi Phase 2

- Biến thể sản phẩm (variant) — overview 2.2: 1 Product = 1 SKU.
- Hard delete sản phẩm — FK `order_items` Phase 4; chỉ dùng `INACTIVE`.
- Biến động tồn kho sau tạo (bán/hoàn/kho admin) — Phase 4/5; Phase 2 chỉ `initialStock`.
- Đa cấp danh mục, slug/SEO, ảnh sản phẩm, thương hiệu.
- Phân trang `GET /categories` — **đã chốt (O-2-02): không phân trang**, trả tất cả.
- Đánh giá, khuyến mãi, coupon (ngoài phạm vi dự án — overview 2.2).

---

## 2. Quyết định Thiết kế

### 2.1. Quyết định đã chốt trong `project-overview.md` mục 7 (bắt buộc tuân thủ)

| Chủ đề | Quyết định (nguyên văn overview) |
|---|---|
| Mô hình sản phẩm | Không variant; 1 Product = 1 SKU |
| Tồn kho | Lưu ở bảng riêng (quan hệ 1-1 với product) để khóa dòng tồn kho không chặn thao tác sửa thông tin sản phẩm |
| Sản phẩm ngừng bán | Dùng trạng thái (ví dụ `ACTIVE`/`INACTIVE`), không xóa cứng sản phẩm đã xuất hiện trong đơn |
| Tiền tệ | Một đơn vị tiền tệ (VND); lưu `DECIMAL(19,2)` |
| Phân trang và sort | `page` bắt đầu từ 0, `size` mặc định 20, tối đa 50. Sort theo whitelist tên trường của API (mặc định: `price`, `name`, `createdAt`), ánh xạ sang trường entity ở service. Trường ngoài danh sách bị từ chối với 400, không để lỗi của Spring Data thành 500 và không lộ tên cột nội bộ |

### 2.2. Mô hình package (D-2-01)

```
com.app.backend
├── category          # Category entity, repository, service, controller, dto
├── product           # Product, ProductInventory, sort/spec, service, controller, dto, query
└── common            # mở rộng: PageResponse, BusinessException
```

Theo định hướng overview mục 4.1. `ProductInventory` nằm trong package `product` (1 entity, không tách riêng — tránh over-engineering).

### 2.3. Bảng `product_inventory` 1-1 (D-2-02)

Theo chốt overview: tồn kho ở bảng riêng để Phase 4 khóa dòng tồn kho bằng `SELECT … FOR UPDATE` trên `product_inventory` mà không lock `products` — admin sửa giá/tên không bị chặn bởi checkout đang giữ khóa tồn kho.

- Khóa chính = `product_id` (PK kiêm FK → `products`, tên `fk_inventory_product`, `ON DELETE CASCADE`).
- `quantity BIGINT NOT NULL DEFAULT 0`, CHECK `quantity >= 0` — bất biến "không âm" được DB bảo vệ ở mọi phase.
- Entity mapping **không** đặt association bên phía Product (xem mục 2.10) — tránh N+1/1-1 eager.
- Phase 2 chỉ ghi một lần khi tạo sản phẩm (`initialStock`); biến động số lượng sau thuộc Phase 4/5. **Ghi chú Phase 3/4:** thêm vào giỏ và checkout phải kiểm `status = ACTIVE` lúc đó — sản phẩm có thể ngừng bán sau khi đã nằm trong giỏ.

### 2.4. Ngừng bán và xóa (D-2-03)

- `ProductStatus`: `ACTIVE` / `INACTIVE`. "Xóa" = chuyển `INACTIVE`: biến mất khỏi public list/chi tiết ngay, dữ liệu và FK còn nguyên (Phase 4 snapshot giá vẫn trỏ được).
- **Không** có `DELETE /api/v1/products/{id}`.
- Category hard delete được nhưng chỉ khi chưa sản phẩm nào trỏ tới: service kiểm trước + FK `RESTRICT` (tên `fk_products_category`) là lớp phòng thủ thứ hai; race (xóa lúc product vừa được tạo trong request khác) rơi vào vi phạm FK → map sang 409 `CATEGORY_HAS_PRODUCTS` (xem 6.3 — không để 500).

### 2.5. Chuẩn hóa chuỗi và ràng buộc unique (D-2-04)

| Trường | Chuẩn hóa trước khi lưu/so sánh | Ràng buộc DB |
|---|---|---|
| `categories.name` | `trim()` + `toLowerCase(Locale.ROOT)` | `UNIQUE` + `CHECK (name = lower(name))` |
| `products.sku` | `trim()` + `toUpperCase(Locale.ROOT)` | `UNIQUE` + `CHECK (sku = upper(sku))` |
| `products.name`, `description` | `trim()`; `description` nullable | — |

CHECK là lớp phòng thủ thứ hai (giống `ck_users_email_lowercase`). **SKU bất biến:** DTO update không chứa `sku` (gửi kèm bị bỏ qua); không endpoint nào đổi SKU. Vi phạm unique khi `saveAndFlush` ném `DataIntegrityViolationException` → đọc **tên constraint có cấu trúc** (6.3) để map `SKU_ALREADY_EXISTS` / `CATEGORY_NAME_ALREADY_EXISTS`. `saveAndFlush` áp dụng cho **cả** service category (không chỉ product).

### 2.6. Phân trang — `PageResponse<T>` (D-2-05)

Không serialize `Page`/`PageImpl` của Spring Data. Tạo record:

```java
// common.response.PageResponse<T>
record PageResponse<T>(List<T> content, int page, int size,
                       long totalElements, int totalPages,
                       boolean first, boolean last) {}
```

- `page` 0-based; vượt `totalPages - 1` → 200 `content` rỗng.
- `size` mặc định 20, tối đa 50: **đã chốt (O-2-03)** vượt → 400 `VALIDATION_ERROR` (tường minh, không clamp im lặng).

### 2.7. Sort whitelist, khóa phụ và tìm kiếm (D-2-06)

**Sort (ổn định — review A3):**

- Param `sort` dạng `<trường>,<hướng>`; thiếu hướng → hướng **mặc định theo trường**: `createdAt` → `desc`, `name`/`price` → `asc` (thói quen REST); thiếu `sort` → `createdAt,desc`.
- Whitelist API → entity (dù hiện trùng tên — mapping tường minh để đổi cột không vỡ API):

  | API field | Entity field | Hướng mặc định |
  |---|---|---|
  | `price` | `price` | asc |
  | `name` | `name` | asc |
  | `createdAt` | `createdAt` | desc |

- **Luôn thêm `id` làm khóa phụ** — `price`, `name`, `createdAt` đều có thể trùng giá trị; không có khóa phụ thì thứ tự giữa các dòng trùng không xác định → trang sau có thể lặp/bỏ sót dòng (đặc biệt rõ với seed — `NOW()` = `transaction_timestamp()`, cả seed cùng `created_at`):

```java
Sort toSort(String field, Sort.Direction dir) {
    String entityField = ALLOWED.get(field);   // whitelist; null → 400 VALIDATION_ERROR (fields.sort)
    return Sort.by(dir, entityField).and(Sort.by(dir, "id"));
}
```

- Trường ngoài whitelist hoặc hướng khác `asc`/`desc` → 400 `VALIDATION_ERROR` với `fields.sort` (đã chốt O-2-04 — không mã lỗi riêng).

**Tìm kiếm:**

- **Không có `ILIKE` trong JPQL/Criteria** — dùng `cb.like(cb.lower(...), pattern, '\\')`:

```java
static Specification<Product> nameContains(String q) {
    String pattern = "%" + escapeLike(q.toLowerCase(Locale.ROOT)) + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
}
static String escapeLike(String s) {   // thứ tự: \ trước, rồi % và _
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
}
```

- Public search khớp **chỉ `name`**; admin search khớp `name` **hoặc `sku`** (admin thường tra theo SKU).
- `search` trim; rỗng → bỏ điều kiện; `@Size(max=100)`.
- **Giới hạn đã biết (ghi nhận):** search không dấu ("dien thoai" khớp "Điện thoại") **không hoạt động** — chuẩn hóa dấu (unaccent/pg_trgm) để phase sau nếu cần; seed dùng tên không dấu là có chủ đích để demo search khớp. Sắp xếp theo `name` tiếng Việt phụ thuộc collation — `postgres:16-alpine` **không** sắp dấu tiếng Việt đúng (đã kiểm chứng nhận biết); chấp nhận cho portfolio, ghi ở README.

**Lọc động:** kết hợp AND bằng `JpaSpecificationExecutor<Product>` + `Specification` (có sẵn Spring Data JPA); kiểm tra API Specification còn nguyên trên Spring Data JPA 4 khi code (AGENTS mục 5 — tránh factory deprecated).

### 2.8. Query object tách public/admin (D-2-12)

Hai record query **tách rời** để public không bao giờ nhận/không bao giờ ép status:

| Record | Trường | Ghi chú |
|---|---|---|
| `PublicProductQuery` | `page`, `size`, `sort`, `search`, `categoryId`, `minPrice`, `maxPrice` | **Không có trường `status`** — public luôn `ACTIVE` bằng cấu trúc |
| `AdminProductQuery` | kế thừa các trường trên + `status` (optional: `ACTIVE`/`INACTIVE`; bỏ trống = tất cả) | Ánh xạ `@Valid` |

Validate: `@Min(0) page`; `@Min(1) @Max(50) size`; `@Size(max=100) search`; `@DecimalMin("0") minPrice/maxPrice`. Lỗi binding/validation của query → 400 `VALIDATION_ERROR` kèm `data.fields` (xem 6.4 — đủ 3 handler, không để 500).

### 2.9. Tiền, version và mass assignment (D-2-07, D-2-09)

**Tiền (D-2-07 — sửa theo review A2):** `DECIMAL(19,2)` gồm cả 2 chữ số thập phân → chỉ chứa **17 chữ số nguyên**. Nhập lớn hơn → PostgreSQL `numeric field overflow` (nếu không chặn trước → 500); nhập 3 chữ số thập phân → Postgres **làm tròn im lặng**, giá lưu khác giá admin gõ. Vì vậy:

```java
@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2)
BigDecimal price;
```

DB CHECK `price > 0` là lớp phòng thủ thứ hai (TC-DB-01). `minPrice`/`maxPrice` kèm `@DecimalMin("0")`.

**Optimistic lock (D-2-09 — "đúng cách", không chỉ `@Version` thô):**

`@Version` một mình chỉ bắt được hai request chồng nhau trong cùng cửa sổ đọc-ghi. Kịch bản thật: admin A mở form lúc t0, admin B lưu lúc t1, admin A lưu lúc t2 — request của A tải entity đã có version của B rồi ghi đè, **không xung đột nào được phát hiện**. Vì vậy version phải **round-trip qua client**:

- Cột `version BIGINT NOT NULL DEFAULT 0`; entity `@Version` (bắt race trong transaction).
- `AdminProductResponse` **trả `version`** (4.8); `UpdateProductRequest` **bắt buộc** `@NotNull Long version`.
- Service so sánh trước khi ghi: `!req.version().equals(product.getVersion())` → 409 `CONCURRENT_MODIFICATION`; `OptimisticLockingFailureException` (race giữa check và commit) map cùng mã.
- Ghi `version` tăng 1 khi PUT thành công; PATCH status đổi thật cũng tăng (4.11). Frontend giữ `version` trong form sửa; nhận 409 → hiện "dữ liệu đã thay đổi, tải lại".

**Mass assignment (D-2-07b):** request tạo/sửa chỉ gồm các trường DTO nêu trong mục 4; `id`, `status` (trên create), `version` (trên create), `sku` (trên update) không nằm trong DTO → bị bỏ qua/không nhận (TC-PRD-21/30).

### 2.10. Entity mapping & chống N+1 (D-2-10 — review A8)

`open-in-view: false` (đã xác minh `application.yml:13`) — truy cập lazy ngoài transaction sẽ ném `LazyInitializationException`; map DTO **bên trong** transaction (`@Transactional(readOnly = true)` cho mọi hàm đọc — tránh cả dirty check không cần thiết).

```java
@Entity @Table(name = "products")
class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Version @Column(nullable = false) long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id") Category category;
    // ... các trường còn lại; KHÔNG có field inventory ở đây
}

@Entity @Table(name = "product_inventory")
class ProductInventory {
    @Id @Column(name = "product_id") Long productId;  // không association → không N+1, không 1-1 eager
    long quantity;
    // @PreUpdate ghi updatedAt (hoặc @UpdateTimestamp) — pattern User.java @PreUpdate
}
```

- **Không** map quan hệ 1-1 ngược phía Product (theo hướng `@OneToOne(mappedBy)` sẽ eager theo EAGER mặc định của 1-1 hoặc lazy không hoạt động đủ — N+1). Entity `ProductInventory` là "dòng dữ liệu thuần" với PK kiêm FK.
- `category` là `LAZY`; list trả `categoryName` → **join fetch một lần** bằng `@EntityGraph`:

```java
interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}
```

- Stock cho admin list gom một truy vấn `IN` (không N+1):

```java
@Transactional(readOnly = true)   // mọi hàm đọc
Page<AdminProductResponse> list(...) {
    Page<Product> page = productRepo.findAll(spec, pageable);
    Map<Long, Long> stock = inventoryRepo.findAllById(
            page.map(Product::getId).toList()).stream()
        .collect(toMap(ProductInventory::getProductId, ProductInventory::getQuantity));
    return page.map(p -> AdminProductResponse.of(p, stock.getOrDefault(p.getId(), 0L)));
}
```

Public list không truy vấn tồn kho. Ngưỡng kiểm chứng: `TC-PERF-01` — số câu SQL ≤ 2 (public) / ≤ 3 (admin), không tăng theo số dòng.

**Chưa làm:** cache `GET /categories` — cần thêm dependency cache và tăng rủi ro dữ liệu cũ; để Phase 6 nếu cần.

### 2.11. Exception dùng chung (D-2-08 — sửa theo review A1)

Record **không** kế thừa được `RuntimeException` (implicit extends `java.lang.Record`) → **phải là class**:

```java
package com.app.backend.common.exception;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        super(message, null, false, false);  // lỗi nghiệp vụ dự kiến: bỏ chi phí capture stack trace
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
```

`GlobalExceptionHandler` thêm `@ExceptionHandler(BusinessException.class)`. `AuthException` Phase 1 giữ nguyên, không sửa (AGENTS mục 2).

### 2.12. Seed demo tách khỏi migration (D-2-11 — sửa theo review A7)

**Không** đặt seed trong `V7__seed` chung. Lý do: migration chạy ở production và mọi integration test; test dùng **PostgreSQL container static dùng chung mọi test class** (`BaseIntegrationTest`) — seed trong `db/migration` sẽ sống mãi trong DB chung, làm lệch mọi assert kiểu "list trả đúng N dòng"; `category_id` hard-code 1/2/3 lệch sequence (ON CONFLICT vẫn tiêu thụ `nextval`).

Cách làm:

- File **repeatable** `Backend/src/main/resources/db/seed/R__demo_catalog.sql` — chỉ nạp ở profile **local**:

```yaml
# application-local.yml
spring:
  flyway:
    locations: classpath:db/migration,classpath:db/seed   # override THAY THẾ default — phải khai đủ cả hai
```

- `application-test.yml` đã ghim `locations: classpath:db/migration` (Phase 1) → test **không** thấy seed; test tự tạo dữ liệu cần thiết và assert trên dữ liệu mình tạo.
- Seed lấy `id` bằng **subquery theo name**, không hard-code số (xem 3.4).
- **Rủi ro đã ghi nhận:** (1) đổi schema bảng seed → phải sửa seed cùng lúc (Flyway checksum); (2) ai đó dùng lại pattern `spring.profiles.default: local` (đã bị loại ở Q-1-02/O-1-03) sẽ khiến deploy quên `SPRING_PROFILES_ACTIVE` nạp nhầm `db/seed` vào production — **cấm** reintroduce `spring.profiles.default`; profile demo Phase 6 phải set `SPRING_PROFILES_ACTIVE` tường minh (demo = local + seed), không dùng "endpoint seed" (thêm bề mặt ghi dữ liệu có thể bị lạm dụng).

Sau khi bỏ V7 khỏi chuỗi versioned, **migration tiếp theo (Phase 3) bắt đầu bằng V7**.

---

## 3. Data Model & Migration

### 3.1. `V4__create_categories.sql`

```sql
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT ck_categories_name_lowercase CHECK (name = lower(name))
);
```

### 3.2. `V5__create_products.sql`

```sql
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50)   NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    price       DECIMAL(19,2) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    category_id BIGINT        NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_sku_uppercase CHECK (sku = upper(sku)),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_products_price_positive CHECK (price > 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_active_created
    ON products (created_at DESC, id DESC) WHERE status = 'ACTIVE';
```

- `fk_products_category` **có tên** để map vi phạm FK (race xóa category) sang 409 có chủ đích (6.3).
- Partial index `WHERE status = 'ACTIVE'` — cột 2 giá trị không cần index thường; truy vấn chính của public list là `WHERE status='ACTIVE' ORDER BY created_at DESC, id DESC`. Giữ `idx_products_category_id` (lọc + kiểm FK khi xóa category).
- `version` cho optimistic lock round-trip (D-2-09).

### 3.3. `V6__create_product_inventory.sql`

```sql
CREATE TABLE product_inventory (
    product_id BIGINT PRIMARY KEY,
    quantity   BIGINT      NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_inventory_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
);
```

### 3.4. Seed demo — `db/seed/R__demo_catalog.sql` (repeatable, local-only — D-2-11)

```sql
INSERT INTO categories (name, description)
VALUES ('dien thoai', 'Dien thoai va phu kien'),
       ('laptop', 'Laptop, may tinh xach tay'),
       ('phu kien', 'Phu kien cong nghe')
ON CONFLICT (name) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'PHONE-001', 'Smartphone Nova X1', 'Man 6.5", 128GB', 4990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'dien thoai'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'PHONE-002', 'Smartphone Nova Lite', 'Man 6.1", 64GB', 2990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'dien thoai'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'LAPTOP-001', 'Laptop Aero 14', 'Core i5, 16GB, 512GB', 15990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'laptop'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'LAPTOP-002', 'Laptop Aero 16 Pro', 'Core i7, 32GB, 1TB', 29990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'laptop'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'ACC-001', 'Tai nghe SoundPods', 'Bluetooth 5.3', 1290000, 'INACTIVE', c.id
FROM categories c WHERE c.name = 'phu kien'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_inventory (product_id, quantity)
SELECT p.id, 50 FROM products p
WHERE p.sku IN ('PHONE-001', 'PHONE-002', 'LAPTOP-001', 'LAPTOP-002')
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO product_inventory (product_id, quantity)
SELECT p.id, 0 FROM products p WHERE p.sku = 'ACC-001'
ON CONFLICT (product_id) DO NOTHING;
```

- Lấy `category_id` bằng subquery theo `name` — không hard-code 1/2/3.
- Tên/spell không dấu có chủ đích: collation alpine + search `lower()` không chuẩn hóa dấu (2.7) — demo search khớp ngay.
- 1 sp `INACTIVE` + 1 sp `quantity=0` để demo ngừng bán/hết hàng. Nội dung seed sẽ được đối chiếu lại với code thật khi triển khai.

### 3.5. Quy tắc dữ liệu

| Trường | Quy tắc |
|---|---|
| `categories.name` | Bắt buộc, 2–100 ký tự; chuẩn lowercase; unique (case-insensitive) |
| `categories.description` | Tối đa 500, nullable |
| `products.sku` | Bắt buộc, 3–50 ký tự `[A-Za-z0-9_-]`; chuẩn uppercase; unique; **bất biến sau tạo** |
| `products.name` | Bắt buộc, 2–200 ký tự |
| `products.description` | Nullable, TEXT |
| `products.price` | Bắt buộc, `> 0`, **17 chữ số nguyên + tối đa 2 thập phân** (validate `@Digits(17,2)` — 2.9) |
| `products.status` | `ACTIVE` (mặc định) hoặc `INACTIVE`; đổi qua PUT hoặc PATCH status |
| `products.category_id` | Bắt buộc, phải tồn tại (`CATEGORY_NOT_FOUND`) |
| `products.version` | `BIGINT NOT NULL DEFAULT 0`; tăng mỗi lần ghi thành công; round-trip qua client (D-2-09) |
| `product_inventory.quantity` | `>= 0`; ghi nhận khi tạo; biến động sau thuộc Phase 4/5 |
| `created_at` / `updated_at` | Entity dùng `@PrePersist`/`@PreUpdate` (pattern `User.java`) — `updated_at` phải đổi sau PUT/PATCH (TC-PRD-29) |

---

## 4. API Contract

Tất cả response dùng `ApiResponse<T>` / `PageResponse<T>` bọc trong `data`; timestamp UTC ISO 8601. Mã lỗi mới thêm vào `docs/error-codes.md` trước hoặc cùng lúc merge.

### 4.1. Mã lỗi bổ sung cho `docs/error-codes.md` (6 mã)

| Mã lỗi | HTTP | Mô tả |
|---|---|---|
| `PRODUCT_NOT_FOUND` | 404 | Sản phẩm không tồn tại, hoặc `INACTIVE` khi gọi endpoint công khai |
| `CATEGORY_NOT_FOUND` | 404 | Danh mục không tồn tại |
| `SKU_ALREADY_EXISTS` | 409 | SKU đã được dùng (không phân biệt hoa/thường) |
| `CATEGORY_NAME_ALREADY_EXISTS` | 409 | Tên danh mục đã tồn tại (không phân biệt hoa/thường) |
| `CATEGORY_HAS_PRODUCTS` | 409 | Danh mục còn sản phẩm (hoặc race vi phạm FK) — không xóa được |
| `CONCURRENT_MODIFICATION` | 409 | `version` client gửi không khớp (PUT) hoặc entity bị đổi giữa chừng (`OptimisticLockingFailureException`) |

Lỗi validation đầu vào (thiếu trường, price ≤ 0, size > 50, sort sai, query sai kiểu, `minPrice > maxPrice`) dùng `VALIDATION_ERROR` (400) với `data.fields` như Phase 1.

### 4.2. Validate tham số query — không để 500 (review A5)

`PublicProductQuery` / `AdminProductQuery` (2.8) bind bằng `@Valid` tại controller. Mọi đường lỗi phải về **400 `VALIDATION_ERROR` kèm `data.fields`** (vd `fields.size`, `fields.sort`, `fields.page`, `fields.categoryId`, `fields.minPrice`):

- Vi phạm ràng buộc trên record (`@Min`/`@Max`/`@Size`/`@DecimalMin`) → `HandlerMethodValidationException`;
- Sai kiểu (`size=abc`, `categoryId=abc`, `page=abc`) → `MethodArgumentTypeMismatchException`;
- Binding object query lỗi chuyển đổi → `BindException`.

Cả ba được `GlobalExceptionHandler` bắt riêng (6.4) — **không** rơi vào catch-all `Exception.class` thành 500. `GET /api/v1/products/abc` (kiểu số) cũng 400 theo cách này.

### 4.3. `GET /api/v1/products` — công khai

Params (`PublicProductQuery` — **không có `status`**, xem D-2-12):

| Param | Kiểu | Mặc định / Ràng buộc |
|---|---|---|
| `page` | int | `0`; âm → 400 |
| `size` | int | `20`; > 50 → 400 (O-2-03) |
| `sort` | string | `createdAt,desc`; field ∈ {`price`,`name`,`createdAt`}; luôn kèm khóa phụ `id` |
| `search` | string | max 100; khớp `name` (không phân biệt hoa/thường, escape wildcard) |
| `categoryId` | long | Lọc theo danh mục |
| `minPrice` / `maxPrice` | decimal | ≥ 0; `minPrice > maxPrice` → 400 |

Response **200** — `PageResponse<ProductResponse>` (public, **không có `stock`/`status`**, TC-PRD-16):

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 3,
        "sku": "LAPTOP-001",
        "name": "Laptop Aero 14",
        "description": "Core i5, 16GB, 512GB",
        "price": 15990000.00,
        "categoryId": 2,
        "categoryName": "laptop",
        "createdAt": "2026-10-21T03:00:00Z",
        "updatedAt": "2026-10-21T03:00:00Z"
      }
    ],
    "page": 0, "size": 20, "totalElements": 4, "totalPages": 1, "first": true, "last": true
  },
  "message": "Products retrieved successfully",
  "errorCode": null,
  "timestamp": "2026-10-21T10:00:00Z"
}
```

Chỉ sản phẩm `ACTIVE`. Lỗi: 400 `VALIDATION_ERROR`.

### 4.4. `GET /api/v1/products/{id}` — công khai

**200** = `ProductResponse`. `INACTIVE` hoặc không tồn tại → **404 `PRODUCT_NOT_FOUND`** (cùng mã — đã chốt O-2-05, không lộ sản phẩm ngừng bán).

### 4.5. `GET /api/v1/categories` — công khai

**200** = mảng `CategoryResponse` `{id, name, description}` — **tất cả** danh mục, `name asc`; không phân trang (đã chốt O-2-02). Lỗi type: sai kiểu param (nếu gửi) → 400.

### 4.6. `POST /api/v1/admin/categories` — `ROLE_ADMIN`

Request: `{ "name": "Điện thoại", "description": "Điện thoại và phụ kiện" }`

**201** = `CategoryResponse` (name đã lowercase). Lỗi: 400, 409 `CATEGORY_NAME_ALREADY_EXISTS`, 401/403.

### 4.7. `PUT /api/v1/admin/categories/{id}` — `ROLE_ADMIN`

Request: `{ "name": "...", "description": "..." }` (cả hai bắt buộc; không PATCH từng phần).

**200** = `CategoryResponse`. Giữ nguyên name, chỉ đổi description → **200, không 409** (trùng chính nó không phải xung đột — TC-CAT-10). Lỗi: 400, 404 `CATEGORY_NOT_FOUND`, 409 `CATEGORY_NAME_ALREADY_EXISTS`, 401/403.

### 4.8. `DELETE /api/v1/admin/categories/{id}` — `ROLE_ADMIN`

**200** `data: null`. Lỗi: 404, 409 `CATEGORY_HAS_PRODUCTS` (gồm cả race vi phạm FK — 6.3), 401/403.

### 4.9. `GET /api/v1/admin/products` — `ROLE_ADMIN`

Params (`AdminProductQuery`): như 4.3, thêm `status` (optional). Response **200** = `PageResponse<AdminProductResponse>`:

```json
{ "id": 5, "sku": "ACC-001", "name": "Tai nghe SoundPods", "description": "...",
  "price": 1290000.00, "status": "INACTIVE", "categoryId": 3, "categoryName": "phu kien",
  "stock": 0, "version": 2,
  "createdAt": "...", "updatedAt": "..." }
```

Admin `search` khớp cả `name` và `sku`.

### 4.10. `GET /api/v1/admin/products/{id}` — `ROLE_ADMIN`

**200** = `AdminProductResponse` đầy đủ (dùng cho form sửa khi reload). Lỗi: 404 `PRODUCT_NOT_FOUND`, 401/403.

### 4.11. `POST /api/v1/admin/products` — `ROLE_ADMIN`

Request:

```json
{ "sku": "phone-003", "name": "Smartphone Nova Pro", "description": "Man 6.7\", 256GB",
  "price": 8990000, "categoryId": 1, "initialStock": 30 }
```

`id`/`status`/`version` gửi kèm trong body bị **bỏ qua** (mass assignment — TC-PRD-30).

**201** = `AdminProductResponse` (SKU uppercase, `status=ACTIVE`, `stock=initialStock`, `version=0`). Lỗi: 400 (thiếu trường, price ≤ 0 / quá precision, initialStock < 0, SKU sai định dạng), 404 `CATEGORY_NOT_FOUND`, 409 `SKU_ALREADY_EXISTS`, 401/403.

### 4.12. `PUT /api/v1/admin/products/{id}` — `ROLE_ADMIN`

Request (**không** có `sku`; **bắt buộc** `version`):

```json
{ "name": "Smartphone Nova Pro 2026", "description": "Man 6.7\", 256GB",
  "price": 7990000, "categoryId": 1, "status": "ACTIVE", "version": 3 }
```

**200** = `AdminProductResponse` với `version` **tăng 1**. `version` không khớp (đã bị PUT/PATCH khác đổi) → **409 `CONCURRENT_MODIFICATION`**; thiếu `version` → 400. Lỗi khác: 400 (validate), 404 (`PRODUCT_NOT_FOUND`/`CATEGORY_NOT_FOUND`), 401/403. Sửa giá không ảnh hưởng đơn cũ (Phase 4 snapshot — overview 6.1).

### 4.13. `PATCH /api/v1/admin/products/{id}/status` — `ROLE_ADMIN`

Request: `{ "status": "INACTIVE" }` — **không** gửi `version`, **không** gửi cả form.

**200** = `AdminProductResponse` đầy đủ (FE cập nhật thẳng dòng bảng từ response):

- Đổi trạng thái thật (`ACTIVE`↔`INACTIVE`) → `version` **tăng 1**.
- Gửi trùng trạng thái hiện tại (no-op, vd `ACTIVE`→`ACTIVE`) → **200**, `version` **không** đổi (idempotent).

Lỗi: 400 (sai enum status → `fields.status`), 404 `PRODUCT_NOT_FOUND`, 401/403.

---

## 5. Luồng Nghiệp vụ (Given/When/Then)

### 5.1. Public duyệt danh sách (ổn định phân trang)

- **Given** catalog có 5 sản phẩm `ACTIVE` cùng `price` + 1 `INACTIVE`.
- **When** `GET /api/v1/products?page=0&size=2&sort=price,asc`, lặp qua các trang đến hết.
- **Then** hợp các trang = đúng 5 dòng, **không lặp, không sót** (khóa phụ `id` — TC-PRD-18); chỉ `ACTIVE`.

### 5.2. Public xem chi tiết sản phẩm ngừng bán

- **Given** sản phẩm id=5 `INACTIVE`.
- **When** `GET /api/v1/products/5`.
- **Then** 404 `PRODUCT_NOT_FOUND` (O-2-05).

### 5.3. Public truyền `status` (tham số không tồn tại)

- **When** `GET /api/v1/products?status=INACTIVE`.
- **Then** param bị **bỏ qua hoàn toàn** — response chỉ chứa `ACTIVE` (D-2-12: `PublicProductQuery` không có trường này; TC-PRD-17).

### 5.4. Admin tạo sản phẩm

- **When** `POST /api/v1/admin/products` hợp lệ (`initialStock=30`).
- **Then** 201; một transaction tạo `products` + `product_inventory.quantity=30`; SKU uppercase; `version=0`; xuất hiện ngay trong public list.

### 5.5. Admin ngừng bán (PATCH idempotent)

- **When** `PATCH /admin/products/{id}/status` `{"status":"INACTIVE"}`.
- **Then** 200; `version` +1; biến mất khỏi public list/detail (404) ngay; admin list vẫn thấy. Gửi lại `INACTIVE` → 200, `version` giữ nguyên.

### 5.6. Hai admin cập nhật song song (optimistic lock round-trip)

- **Given** admin A mở form (nhận `version=3`); admin B PUT thành công (`version` → 4).
- **When** admin A PUT với `version=3`.
- **Then** 409 `CONCURRENT_MODIFICATION` — FE hiện "dữ liệu đã thay đổi, tải lại"; B không bị ghi đè. PUT lại với `version=4` → 200, `version` → 5.

### 5.7. Admin xóa danh mục (kể cả race)

- **When** `DELETE .../categories/{id}` còn sản phẩm → 409 `CATEGORY_HAS_PRODUCTS`; hai request song song "xóa category trống" và "tạo product thuộc category đó" → một bên 200/201, bên kia 409 (FK có tên map sang 409 — TC-CAT-12), không 500.

### 5.8. Sort trường ngoài whitelist / query sai kiểu

- **When** `?sort=password,asc` → 400 `fields.sort`; `?size=abc` → 400 `fields.size`; `GET /products/abc` → 400 (4.2).

### 5.9. USER gọi endpoint admin

- **When** token role `USER` gọi bất kỳ endpoint `/admin/**` nào → 403 `ACCESS_DENIED`; không token → 401 (TC-AUTHZ-01 parameterized).

---

## 6. Bảo mật & Phân quyền

### 6.1. `SecurityConfig` — thay đổi duy nhất

Phase 1 đã che phủ `/api/v1/admin/**` bằng `hasRole("ADMIN")` — mọi endpoint admin mới (gồm `GET .../{id}`, `PATCH .../status`) được bảo vệ **không cần sửa matcher admin**. Chỉ thêm:

```java
.requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
```

Chỉ GET công khai; mọi POST/PUT/DELETE/PATCH rơi vào `.anyRequest().authenticated()` (mặc định từ chối — overview 5.1).

### 6.2. Bảng phân quyền

| Endpoint | Yêu cầu |
|---|---|
| `GET /products`, `GET /products/{id}`, `GET /categories` | Công khai |
| `GET/POST/PUT/DELETE` `/admin/categories/**` | `ROLE_ADMIN` |
| `GET/POST/PUT/PATCH` `/admin/products/**` | `ROLE_ADMIN` |

Mọi endpoint admin có cặp test 401/403 (TC-AUTHZ-01 tham số hóa — tự phủ cả endpoint phase sau).

### 6.3. Constraint/FK map có cấu trúc (review A9)

Thay `message.contains("uk_...")` (R-2-01) bằng đọc tên constraint từ exception:

```java
static Optional<String> constraintName(Throwable ex) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
        if (t instanceof org.hibernate.exception.ConstraintViolationException c
                && c.getConstraintName() != null) {
            return Optional.of(c.getConstraintName());
        }
    }
    return Optional.empty();
}
```

Ánh xạ (kiểm tra giá trị thực tế trả về — có thể kèm schema/tích tắc tùy dialect; assert bằng test integration):

| Tên constraint | Kết quả |
|---|---|
| `uk_products_sku` | 409 `SKU_ALREADY_EXISTS` |
| `uk_categories_name` | 409 `CATEGORY_NAME_ALREADY_EXISTS` |
| `fk_products_category` | 409 `CATEGORY_HAS_PRODUCTS` (race xóa category) hoặc 404 theo ngữ cảnh service đã kiểm trước |
| `ck_products_price_positive`, `ck_inventory_quantity_non_negative` | 400 `VALIDATION_ERROR` (lớp phòng thủ thứ hai nếu service bỏ sót) |
| còn lại | 500 như cũ, có log |

Service dùng `saveAndFlush` (category **và** product) để exception ném ngay trong transaction. Không log dữ liệu payload (overview 4.5).

### 6.4. `GlobalExceptionHandler` mở rộng

- `BusinessException` → status/errorCode của exception (2.11).
- `HandlerMethodValidationException`, `MethodArgumentTypeMismatchException`, `BindException` → 400 `VALIDATION_ERROR`, dựng `data.fields` từ tên tham số/trường (vd `fields.size`, `fields.sort`, `fields.categoryId`, `fields.status`) — cùng format `fields` của handler validation Phase 1.
- Mở rộng handler `DataIntegrityViolationException` theo 6.3.

---

## 7. Frontend

### 7.1. Phạm vi (overview 4.7 — antd, không thêm state lib)

| Hạng mục | Chi tiết |
|---|---|
| Route công khai | `/products` (danh sách), `/products/:id` (chi tiết) |
| Route admin | `/admin/products`, `/admin/categories` — bọc `RequireAdmin` |
| Danh sách sản phẩm | antd `Table` + `Pagination` server-side; `Select` danh mục; `Input` search **debounce 300ms**; sort theo cột (name/price/createdAt); **đổi filter/search → page về 0** (FE-PRD-04); **AbortController** hủy request cũ khi param đổi — phản hồi cũ về muộn không được ghi đè phản hồi mới (FE-PRD-05) |
| Chi tiết sản phẩm | Card thông tin; không có nút giỏ hàng (Phase 3) |
| Admin sản phẩm | Table (cột `status`, `stock`); Thanh Sửa → Modal load từ `GET /admin/products/{id}` (**reload form không mất dữ liệu**); ô **SKU disabled** khi sửa (FE-PRD-06); form giữ `version` và gửi trong PUT; 409 `CONCURRENT_MODIFICATION` → message "dữ liệu đã thay đổi, vui lòng tải lại" + refetch; nút **Ngừng bán/Kích hoạt** gọi `PATCH .../status` rồi cập nhật dòng bảng từ response trả về (không PUT cả form) |
| Admin danh mục | Table + Modal Thêm/Sửa (PUT), Popconfirm Xóa; hiển thị đúng `CATEGORY_HAS_PRODUCTS` (FE-PRD-06) |
| Axios dùng chung | Mở rộng `src/api.js` — thêm API helpers; **giữ nguyên** interceptor/token/refresh Phase 1 |

Hiển thị lỗi theo `errorCode` đã biết; `VALIDATION_ERROR` → hiện `data.fields`.

### 7.2. Frontend test (Vitest)

| ID | Nội dung |
|---|---|
| FE-PRD-01 | `ProductsPage` render (mock list); đổi trang gọi API đúng `page`; filter danh mục gọi API đúng `categoryId` |
| FE-PRD-02 | Search gõ nhanh → API chỉ gọi sau khi ngừng gõ (debounce), `search` = chuỗi cuối |
| FE-PRD-03 | `/admin/products` với user `USER` → redirect; với `ADMIN` → render table |
| FE-PRD-04 | Đổi bộ lọc/search → `page` về 0 trong request kế tiếp |
| FE-PRD-05 | Gõ search nhanh 2 lần, mock lần 1 về muộn hơn lần 2 → UI hiển thị kết quả lần 2 (không bị ghi đè) |
| FE-PRD-06 | Form sửa sản phẩm: ô SKU disabled; mock 409 `CATEGORY_HAS_PRODUCTS` khi xóa danh mục → hiển thị message đúng |

---

## 8. Kế hoạch Kiểm thử

### 8.1. Nguyên tắc

- Test integration kế thừa **`BaseIntegrationTest`** (package `common` — Testcontainers PostgreSQL/Redis static dùng chung; profile `test` ghim `flyway.locations=classpath:db/migration` nên **không nạp seed** — D-2-11).
- Test **tự tạo dữ liệu cần thiết** (category/product riêng với giá trị unique có hậu tố test); không assert trên dữ liệu seed.
- Mỗi endpoint admin có ≥1 test được phép và ≥1 bị từ chối (TC-AUTHZ-01 tham số hóa phủ tất cả).
- Đơn vị: unit cho sort parser và mapping `Page` → `PageResponse` — không mock Spring Data.

### 8.2. Integration test cases

| ID | Case | Kỳ vọng |
|---|---|---|
| TC-CAT-01 | Admin tạo category hợp lệ | 201; name lowercase |
| TC-CAT-02 | Tạo trùng name (khác hoa/thường) | 409 `CATEGORY_NAME_ALREADY_EXISTS` |
| TC-CAT-03 | Name 1 ký tự / > 100 ký tự / trống | 400 `VALIDATION_ERROR` |
| TC-CAT-04 | USER tạo category; không token | 403; 401 |
| TC-CAT-05 | Đổi name hợp lệ → 200; đổi sang name đã tồn tại → 409 | Đúng hai nhánh |
| TC-CAT-06 | Xóa category trống | 200; list không còn |
| TC-CAT-07 | Xóa category còn product | 409 `CATEGORY_HAS_PRODUCTS` |
| TC-CAT-08 | Update/Delete id không tồn tại | 404 `CATEGORY_NOT_FOUND` |
| TC-CAT-09 | `GET /categories` không token | 200, đủ danh mục, `name asc` |
| TC-CAT-10 | PUT giữ nguyên name, chỉ đổi description | 200, **không** 409 |
| TC-CAT-11 | Hai request song song tạo cùng tên category | Một 201, một 409; không 500 |
| TC-CAT-12 | Xóa category trống race với request tạo product thuộc category đó (2 thread, barrier) | 409 `CATEGORY_HAS_PRODUCTS` (FK có tên), không 500 |
| TC-PRD-01 | Admin tạo product hợp lệ (`initialStock=30`) | 201; SKU uppercase; `stock=30`; `version=0`; vào public list |
| TC-PRD-02 | Tạo SKU trùng (khác hoa/thường) | 409 `SKU_ALREADY_EXISTS` |
| TC-PRD-03 | price=0 / price=-1 / initialStock=-1 | 400, không 500 |
| TC-PRD-04 | categoryId không tồn tại | 404 `CATEGORY_NOT_FOUND`; không tạo được product nào |
| TC-PRD-05 | Public list mặc định | Chỉ `ACTIVE`; sort `createdAt desc`; `totalElements` đúng dữ liệu test tự tạo |
| TC-PRD-06 | Filter `categoryId` + `minPrice` + `maxPrice` + `search` (chữ HOA/thường; chuỗi chứa `%`) | Chỉ dòng khớp AND; `%` bị escape |
| TC-PRD-07 | `sort=price,asc`, `sort=name,desc`; `sort=password,asc`, `sort=price,sideways` | Thứ tự đúng; lỗi → 400 `fields.sort` |
| TC-PRD-08 | `GET /products/{id}`: ACTIVE → 200; INACTIVE → 404; id không tồn tại → 404 | Cùng mã `PRODUCT_NOT_FOUND` |
| TC-PRD-09 | Admin list | Thấy cả `INACTIVE`; có `stock`/`version`; filter `status` hoạt động |
| TC-PRD-10 | Admin PUT product (name/price/category/status) | 200; đổi categoryId không tồn tại → 404 |
| TC-PRD-11 | Chuyển `INACTIVE` (qua PUT hoặc PATCH) | Public list/detail 404 ngay; admin vẫn thấy; chuyển lại `ACTIVE` → public thấy lại |
| TC-PRD-12 | USER gọi `POST /admin/products` | 403; không token → 401 |
| TC-PRD-13 | `size=51` → 400; `size=50` → 200; `page=999` → 200 `content` rỗng | Hợp đồng phân trang |
| TC-PRD-14 | `minPrice=100&maxPrice=50` | 400 `fields.minPrice` |
| TC-PRD-15 | `search=khongtantai` | 200 `content` rỗng |
| TC-PRD-16 | Public list/detail **không** chứa `stock`/`status` | Assert body |
| TC-PRD-17 | `GET /products?status=INACTIVE` (và `status=ACTIVE`) | Param bị bỏ qua — chỉ trả `ACTIVE`; không 400 cũng không lộ `INACTIVE` |
| TC-PRD-18 | 5 product cùng giá; `size=2&sort=price,asc`; duyệt hết trang | Hợp các trang = đủ 5, không lặp, không sót |
| TC-PRD-19 | `price=100.555`; price 18 chữ số nguyên; `price=1e30` | 400 `VALIDATION_ERROR`, không 500, không lưu giá đã làm tròn |
| TC-PRD-20 | `size=abc`, `page=-1`, `categoryId=abc`, `minPrice=abc`, `GET /products/abc` | 400 `VALIDATION_ERROR`; `data.fields` chứa đúng key (`fields.size`, `fields.page`, `fields.categoryId`, `fields.minPrice`) |
| TC-PRD-21 | PUT với `"sku": "OTHER"` trong body | 200; SKU không đổi |
| TC-PRD-22 | PUT `status="DELETED"`; price ≤ 0; thiếu name | 400 (enum / validation) |
| TC-PRD-23 | PUT/GET admin id không tồn tại | 404 `PRODUCT_NOT_FOUND` |
| TC-PRD-24 | SKU sai định dạng (`ab`, có khoảng trắng, ký tự đặc biệt) | 400 |
| TC-PRD-25 | Hai request song song cùng SKU | Một 201, một 409 (mô hình TC-AUTH-27) |
| TC-PRD-26 | Search chỉ `_`, `\`, khoảng trắng; chuỗi 101 ký tự | `_`/`\` không thành wildcard; blank = không lọc; > 100 → 400 |
| TC-PRD-27 | Chỉ `minPrice`; chỉ `maxPrice`; `minPrice == maxPrice` | Biên bao gồm hai đầu |
| TC-PRD-28 | Catalog test rỗng | 200, `content=[]`, `totalPages=0`, `first`/`last` đúng |
| TC-PRD-29 | PUT thành công | `updatedAt` mới > cũ; `createdAt` giữ nguyên |
| TC-PRD-30 | POST kèm `"status":"INACTIVE"` và/hoặc `"id"` trong body | Bị bỏ qua: `status=ACTIVE`, id sinh mới |
| TC-PRD-31 | GET `/admin/products/{id}`: tồn tại → 200 (đủ `stock`/`version`); không tồn tại → 404 | Dùng được cho form sửa reload |
| TC-PRD-32 | PATCH status: đổi thật → 200 trả `AdminProductResponse` đầy đủ, `version` +1; no-op (ACTIVE→ACTIVE) → 200 `version` giữ nguyên; sai enum → 400 `fields.status`; id không tồn tại → 404 | Idempotent (4.13) |
| TC-PRD-33 | PUT `version` đúng → 200 `version` +1; PUT `version` cũ (sau khi PATCH/PUT khác đã đổi) → 409 `CONCURRENT_MODIFICATION`; PUT thiếu `version` → 400 | Round-trip D-2-09 |
| TC-AUTHZ-01 | **Tham số hóa** (`@ParameterizedTest` + `@MethodSource`): mọi endpoint admin (categories CRUD + products GET-by-id/POST/PUT/PATCH/list) không token → 401; token USER → 403 | Cặp 401/403 cho **tất cả** endpoint admin, tự phủ cả phase sau |
| TC-DB-01 | `JdbcTemplate` insert trực tiếp vi phạm CHECK: price=0, quantity=-1, sku chữ thường | DB từ chối — chứng minh phòng thủ lớp hai |
| TC-PERF-01 | Bật `hibernate.generate_statistics`; public list 20 dòng; admin list 20 dòng | Số câu SQL **≤ 2** (public: count + content) và **≤ 3** (admin: + truy vấn stock `IN`); không tăng theo số dòng. Dùng `<=` vì Spring Data bỏ COUNT khi trang đầu chưa đầy |

### 8.3. Unit test cases

| ID | Case | Kỳ vọng |
|---|---|---|
| TC-PRD-UT-01 | Parse sort: `price,asc` hợp lệ và **luôn kèm `id` khóa phụ**; thiếu hướng → default đúng theo trường (`name`→asc, `createdAt`→desc); `password,asc` → lỗi; `price,sideways` → lỗi | Whitelist + hướng mặc định + tiebreaker (D-2-06) |
| TC-PRD-UT-02 | Map `Page` → `PageResponse`: `totalPages` (0 khi trống, ceil khi lẻ), `first`/`last`, content giữ thứ tự | Đúng công thức; DTO không phụ thuộc object Spring |

### 8.4. Tổng dự kiến

48 integration (TC-CAT-01..12, TC-PRD-01..33, TC-AUTHZ-01, TC-DB-01, TC-PERF-01) + 2 unit = **50 test mới**; tổng suite backend ≈ 39 (Phase 1) + 50 = **89**. Con số cuối chốt trong báo cáo khi chạy `./mvnw clean verify` (nếu lệch do cắt case phải ghi rõ lý do — không cắt im lặng).

---

## 9. Definition of Done — Phase 2

### 9.1. Chức năng backend

- [ ] CRUD category theo quyền: tạo/sửa/xóa (chỉ khi trống, kể cả race FK → 409), danh sách công khai không phân trang.
- [ ] CRUD product theo quyền: tạo (kèm `initialStock`), PUT (đổi giá/category/status, **kèm `version`**), GET-by-id admin, PATCH status idempotent; admin list thấy cả `INACTIVE` + `stock` + `version`.
- [ ] Public list phân trang **ổn định** (khóa phụ `id`) + lọc category/khoảng giá + search + sort whitelist đúng hợp đồng; `?status` bị bỏ qua.
- [ ] Public detail `INACTIVE` → 404; không lộ sản phẩm ngừng bán.
- [ ] Optimistic lock round-trip: `version` cũ → 409 `CONCURRENT_MODIFICATION`; đúng → 200 và tăng.
- [ ] SKU bất biến; tồn kho ban đầu ghi đúng `product_inventory`; price bị chặn ở DTO (`@Digits(17,2)`) và CHECK DB.

### 9.2. Bảo mật

- [ ] Mọi endpoint admin mới từ chối 401/403 đúng (TC-AUTHZ-01); chỉ ADMIN làm được.
- [ ] Public GET không cần token; không method ghi nào bị permitAll nhầm.
- [ ] Không lộ `stock`/`status` qua public API.

### 9.3. Kiểm thử

- [ ] `(cd Backend && ./mvnw clean verify)` PASS toàn bộ.
- [ ] Đủ các TC mục 8 (hoặc ghi rõ case cut + lý do).
- [ ] TC-PERF-01 chứng minh không N+1 (≤ ngưỡng quy định).
- [ ] Query sai kiểu/ràng buộc → 400 `VALIDATION_ERROR` kèm `fields`, không 500 (TC-PRD-20).
- [ ] CI hai job xanh sau push.

### 9.4. Frontend

- [ ] Danh sách/chi tiết công khai hoạt động với backend thật (phân trang, lọc, search debounce, chống stale response).
- [ ] Admin sản phẩm/danh mục hoạt động với admin seed: form sửa load được từ GET-by-id (reload không mất dữ liệu), SKU khóa khi sửa, PUT kèm `version` (409 → message tải lại), nút ngừng bán dùng PATCH cập nhật dòng từ response.
- [ ] USER bị chặn client-side ở route admin.
- [ ] `npm --prefix frontend test` xanh (FE-PRD-01..06).

### 9.5. Tài liệu & cấu hình

- [ ] `docs/error-codes.md` đủ 6 mã mới (4.1).
- [ ] Swagger hiển thị endpoint mới, thử được cả public và admin.
- [ ] README: tính năng Phase 2, seed local-only (R__), giới hạn search không dấu/collation, cảnh báo không reintroduce `spring.profiles.default`.
- [ ] Không commit secret; test phase 0/1 còn xanh; `updated_at` hoạt động (TC-PRD-29).

---

## 10. Quyết định, Rủi ro & Câu hỏi

### 10.1. Quyết định đã chốt (tóm tắt)

| ID | Quyết định | Lý do chính |
|---|---|---|
| D-2-01 | Package `category` + `product`; `ProductInventory` trong `product` | Overview 4.1 |
| D-2-02 | `product_inventory` 1-1 (PK = FK có tên), CHECK `quantity >= 0` | Overview mục 7; bất biến âm ở DB |
| D-2-03 | Product chỉ `INACTIVE`; category hard delete khi trống (+ FK RESTRICT có tên) | Chuẩn bị FK Phase 4 |
| D-2-04 | Lowercase name / uppercase SKU + CHECK; SKU bất biến; `saveAndFlush` cả hai service | Pattern Phase 1; chặn race unique |
| D-2-05 | `PageResponse<T>` DTO riêng; size > 50 → 400 (O-2-03) | Contract ổn định, tường minh |
| D-2-06 | Sort whitelist + **khóa phụ `id`**; hướng mặc định theo trường; `cb.like`/`cb.lower` + escape; admin search + sku; giới hạn không dấu/collation ghi nhận | Phân trang ổn định (A3); Criteria đúng chuẩn |
| D-2-07 | `@Digits(17,2)` + `@DecimalMin("0")` giá; public không `stock` | DECIMAL(19,2) chỉ 17 chữ số nguyên (A2) |
| D-2-08 | `BusinessException` là **class extends RuntimeException** (không phải record) | Record không throw được (A1) |
| D-2-09 | `@Version` + version round-trip qua client; mã `CONCURRENT_MODIFICATION` | Bắt cả kịch bản form mở lâu (A-B-A) |
| D-2-10 | Entity mapping: lazy category + `@EntityGraph`; `ProductInventory` thuần `productId`; `@Transactional(readOnly)`; không cache categories | Chống N+1 với OSIV=false (A8) |
| D-2-11 | Seed `R__demo_catalog.sql` local-only; Phase 6 = profile demo; cấm `spring.profiles.default` | Không làm nhiễu test/production (A7) |
| D-2-12 | Tách `PublicProductQuery` / `AdminProductQuery` | Public không nhận `status` **bằng cấu trúc** (A6) |

### 10.2. Rủi ro đã xác định

| ID | Rủi ro | Mức | Biện pháp giảm thiểu |
|---|---|---|---|
| R-2-01 | Đọc tên constraint từ `ConstraintViolationException` — giá trị có thể kèm schema/tích tắc theo dialect | Thấp | Đọc `getConstraintName()` có cấu trúc (6.3); assert giá trị thật bằng integration test; sai chỉ rơi về 500 |
| R-2-02 | Search LIKE với `%`/`_` người dùng nhập | Thấp | Escape (D-2-06, TC-PRD-26) |
| R-2-03 | PermitAll GET mở rộng | Trung bình | Chỉ `HttpMethod.GET`; test 401/403 đầy đủ |
| R-2-04 | Seed R__ lệch schema khi sửa bảng | Thấp | Ghi chú quy tắc: sửa schema seed thì sửa seed cùng commit; checksum Flyway phát hiện file đổi |
| R-2-05 | Reintroduce `spring.profiles.default: local` → seed nạp nhầm production | Trung bình | README + tài liệu cấm (liên hệ O-1-03); jar production không có profile mặc định |
| R-2-06 | `@Version` một mình không bắt được xung đột form mở lâu | Trung bình | Version round-trip bắt buộc ở PUT (D-2-09); FE hiện message tải lại khi 409 |
| R-2-07 | Collation alpine không sắp dấu tiếng Việt đúng; search không dấu không hoạt động | Thấp | Ghi nhận giới hạn; seed tên không dấu có chủ đích; unaccent để phase sau |
| R-2-08 | TC-PERF-01 phụ thuộc statistics/flush — có thể nhạy với cấu hình Hibernate | Thấp | Assert `<=` ngưỡng (không ==); dùng session statistics `PrepareStatementCount` |

### 10.3. Quyết định đã chốt (từ review và đề xuất mặc định — 2026-10-21)

| ID | Nội dung | Phương án đã chốt |
|---|---|---|
| O-2-01 | Seed demo catalog | **Có** — `db/seed/R__demo_catalog.sql` repeatable, **chỉ profile local**; Phase 6 dùng profile demo; không endpoint seed (D-2-11) |
| O-2-02 | `GET /categories` phân trang? | **Không** — trả tất cả, `name asc` |
| O-2-03 | `size` > 50 | **400** `VALIDATION_ERROR` |
| O-2-04 | Sort sai trường | **`VALIDATION_ERROR`** với `fields.sort` |
| O-2-05 | Public detail `INACTIVE` | **404** cùng mã với không tồn tại |
| (review) | `GET /admin/products/{id}` + `PATCH .../status` + `@Version` round-trip | **Nhận cả ba** — sửa được UX thật; version làm đúng cách (D-2-09) |
| (review) | Handler query lỗi + tách Public/Admin query | **Nhận** — không để 500; public không nhận `status` bằng cấu trúc (D-2-12, 4.2, 6.4) |

### 10.4. Câu hỏi còn mở

Không còn câu hỏi nào mở — toàn bộ O-2-01 → O-2-05 và các điểm review (A/B/C/D) đã được chủ dự án chốt ngày 2026-10-21 và chuyển vào 10.1/10.3.

---

## 11. Thứ tự Triển khai Đề xuất

1. **Tài liệu lỗi:** thêm 6 mã vào `docs/error-codes.md` (4.1).
2. **Migration:** `V4` categories → `V5` products (kèm `version`, FK có tên, partial index) → `V6` product_inventory; **không** có V7 seed — tạo `db/seed/R__demo_catalog.sql` và khai `spring.flyway.locations` ở `application-local.yml` (cả hai path). **Phase 3 sẽ bắt đầu bằng V7.**
3. **Common:** `PageResponse`, `BusinessException` (class — 2.11); mở rộng `GlobalExceptionHandler` (`BusinessException` + 3 handler query lỗi có `data.fields` + đọc `getConstraintName()`).
4. **Category module:** entity (`@PreUpdate`) → repository → DTO → service (chuẩn hóa name, `saveAndFlush`, FK race → 409) → controller admin + public.
5. **Product module:** `Product` (`@Version`, lazy category) + `ProductInventory` (thuần `productId`) → repository (`JpaSpecificationExecutor` + `@EntityGraph`) → sort parser (tiebreaker `id`) + Specification (escape like) → query records (`PublicProductQuery`/`AdminProductQuery`) → DTO (create/update/admin/public — đúng mass-assignment rules) → service (`@Transactional(readOnly)` đọc, stock gom `IN`, version check) → controller (public GET, admin GET/list/POST/PUT/PATCH).
6. **Security:** thêm 1 dòng permitAll GET (6.1); không đổi matcher admin.
7. **Tests:** 2 UT + 48 integration (mục 8); `(cd Backend && ./mvnw clean verify)` xanh; test phase 0/1 không vỡ; test tự tạo dữ liệu (không phụ thuộc seed).
8. **Frontend:** mở rộng `api.js`; `ProductsPage` (debounce + AbortController + filter→page 0), `ProductDetailPage`, `AdminProductsPage` (GET-by-id, version trong form, SKU disabled, PATCH status), `AdminCategoriesPage`; routes + header; FE-PRD-01..06; `npm run lint` + `npm test` + `npm run build`.
9. **README:** tính năng Phase 2, seed local-only, giới hạn search/collation, cảnh báo `spring.profiles.default`.
10. **Báo cáo AGENTS mục 3** → cập nhật `docs/progress.md` → commit + push khi chủ dự án yêu cầu → CI xanh → tick DoD.

---

*Tài liệu soạn 2026-10-21 theo mẫu phase-1-auth.md; cùng ngày được rà soát theo review chủ dự án (A1–A10, B, C, D) — toàn bộ tiếp nhận và cập nhật: BusinessException là class, `@Digits(17,2)`, khóa phụ sort, tách query public/admin, seed R__ local-only, entity mapping chống N+1, constraint name có cấu trúc, version round-trip, PATCH idempotent, 50 test mới (48 integration + 2 unit). Trạng thái: **chờ chủ dự án duyệt lần cuối** trước khi triển khai theo mục 11.*
