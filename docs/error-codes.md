# Error Codes

Danh mục mã lỗi dùng trong response `errorCode` của `ApiResponse`.

## Format

Mỗi mã lỗi theo pattern: `DOMAIN_ERROR_CODE` — viết hoa, gạch dưới, mô tả lỗi nghiệp vụ cụ thể.

## Danh mục mã lỗi

| Mã lỗi | HTTP Status | Mô tả |
|---|---|---|
| `INTERNAL_SERVER_ERROR` | 500 | Lỗi server không xác định; `GlobalExceptionHandler` bắt exception chưa được xử lý riêng |
| `VALIDATION_ERROR` | 400 | Request DTO không hợp lệ (bean validation) hoặc body JSON hỏng; `data` chứa chi tiết trường khi có |
| `NOT_FOUND` | 404 | Route không tồn tại (`NoResourceFoundException`) |
| `METHOD_NOT_ALLOWED` | 405 | Method sai cho route (`HttpRequestMethodNotSupportedException`) |
| `INVALID_CREDENTIALS` | 401 | Email không tồn tại, sai mật khẩu hoặc tài khoản bị khóa (cùng một mã — chống dò tài khoản) |
| `TOKEN_EXPIRED` | 401 | Access/refresh token hết hạn; client nên gọi refresh |
| `TOKEN_INVALID` | 401 | Token thiếu, sai chữ ký, sai cấu trúc, sai `type` hoặc hash không khớp |
| `TOKEN_REVOKED` | 401 | Phiên (`sid`) đã logout, bị thu hồi do dùng lại refresh token, hoặc user đã bị xóa/khóa |
| `ACCESS_DENIED` | 403 | Đã xác thực nhưng thiếu vai trò cần thiết |
| `EMAIL_ALREADY_EXISTS` | 409 | Email đã được đăng ký |
| `PRODUCT_NOT_FOUND` | 404 | Sản phẩm không tồn tại, hoặc tồn tại nhưng `INACTIVE` khi gọi endpoint công khai |
| `CATEGORY_NOT_FOUND` | 404 | Danh mục không tồn tại |
| `SKU_ALREADY_EXISTS` | 409 | SKU đã được dùng cho sản phẩm khác (không phân biệt hoa/thường) |
| `CATEGORY_NAME_ALREADY_EXISTS` | 409 | Tên danh mục đã tồn tại (không phân biệt hoa/thường) |
| `CATEGORY_HAS_PRODUCTS` | 409 | Danh mục còn sản phẩm (hoặc race vi phạm FK) — không xóa được |
| `CONCURRENT_MODIFICATION` | 409 | `version` client gửi không khớp, hoặc entity bị đổi giữa chừng (optimistic lock) |
