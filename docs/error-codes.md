# Error Codes

Danh mục mã lỗi dùng trong response `errorCode` của `ApiResponse`.

## Format

Mỗi mã lỗi theo pattern: `DOMAIN_ERROR_CODE` — viết hoa, gạch dưới, mô tả lỗi nghiệp vụ cụ thể.

## Danh mục mã lỗi

| Mã lỗi | HTTP Status | Mô tả |
|---|---|---|
| `INTERNAL_SERVER_ERROR` | 500 | Lỗi server không xác định; `GlobalExceptionHandler` bắt exception chưa được xử lý riêng |
