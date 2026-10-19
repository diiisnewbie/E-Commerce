# AGENTS.md — Quy tắc làm việc với AI

Dự án: Website E-commerce Fullstack (Java 21 + Spring Boot 4.1.x, React + Vite, PostgreSQL, Redis).

- Tổng quan, phạm vi, quy ước code, quyết định mặc định và roadmap: `docs/project-overview.md`.
- Đặc tả chi tiết từng phase: `docs/phase-N-*.md`.
- File này chỉ chứa quy trình và quy tắc hành động. Không lặp lại nội dung của `docs/project-overview.md` ngoài một số quy tắc rủi ro cao được nhắc lại có chủ đích ở mục 2. Khi hai nơi mâu thuẫn, **dừng lại và hỏi**.

---

## 1. Trước khi sửa code

- Đọc `docs/project-overview.md` để nắm các ràng buộc chung và quyết định mặc định.
- Đọc tài liệu `docs/phase-N-*.md` tương ứng với phase được giao. Nếu chưa có tài liệu phase, **không viết code**; báo lại để viết tài liệu trước.
- Kiểm tra cấu trúc repository và code hiện có trước khi đề xuất thay đổi.
- Xác định các file dự kiến thêm/sửa/xóa và giải thích lý do ngắn gọn.
- Nếu thiếu thông tin nghiệp vụ hoặc gặp điểm mâu thuẫn có thể làm thay đổi thiết kế: dừng lại và hỏi trước khi triển khai. Không tự suy diễn yêu cầu chưa được xác nhận.

---

## 2. Trong khi sửa code

**Phạm vi và quy ước**
- Chỉ thực hiện phase được giao; không sửa logic của các phase đã hoàn thành nếu không có yêu cầu rõ ràng.
- Không tự cài thêm thư viện/dependency ngoài danh mục tại mục 3 của `docs/project-overview.md`.
- Dự án dùng Spring Boot 4.x, không phải 3.x. Xem mục 5 trước khi viết code backend để tránh sinh code theo kiểu Spring Boot 3.
- Không đổi kiến trúc modular monolith, response format chuẩn, mã lỗi (trong `docs/error-codes.md`) hay quy ước đặt tên.
- Frontend tuân thủ mục 4.7 của `docs/project-overview.md` (dùng UI library, không thêm Redux/Zustand, không viết nhiều CSS tùy biến).
- Không mở rộng sang các mục "ngoài phạm vi" của dự án.
- Mã backend chỉ đặt trong `backend/`, mã frontend chỉ đặt trong `frontend/`; không tạo mã nguồn ở thư mục gốc. Cấu trúc đầy đủ: mục 4.1 của `docs/project-overview.md`.

**Bảo mật và dữ liệu**
- Không trả Entity trực tiếp ra API; bắt buộc map sang DTO.
- Không hardcode secret/token vào source code, file cấu hình, log hoặc response.
- Mỗi thay đổi database schema phải tạo file Flyway migration mới; không sửa migration đã áp dụng.

**Tính toàn vẹn của mã**
- Không comment out, sửa hoặc xóa test hiện có chỉ để làm test pass. Nếu test mâu thuẫn với tài liệu phase, được đề xuất sửa nhưng phải nêu rõ trong báo cáo và chờ xác nhận.
- Không chèn comment dạng `// ... existing code ...` làm mất nội dung gốc khi ghi file.
- Không chạy lệnh phá hủy dữ liệu hoặc lịch sử (`git reset --hard`, `git push --force`, `docker compose down -v`, xóa volume/database) khi chưa được xác nhận. Dọn thư mục build (`target/`, `node_modules/`) thì được phép.
- Không tự `git commit` hoặc `git push` trừ khi được yêu cầu.

**Cách làm việc**
- Ưu tiên thay đổi nhỏ, rõ ràng, dễ review và dễ rollback.
- Nếu phát hiện lỗi ngoài phạm vi của phase hiện tại: ghi nhận và báo lại, không tự sửa chéo.

---

## 3. Sau khi sửa code

Báo cáo theo cấu trúc sau:

1. **Tóm tắt thay đổi:** mô tả ngắn gọn nội dung đã thực hiện.
2. **File tác động:** các file thêm/sửa/xóa.
3. **Quyết định thiết kế đáng chú ý** (nếu có).
4. **Kiểm thử đã chạy:** lệnh build/test đã chạy trên terminal và kết quả (PASS/FAIL).
5. **Kiểm thử chưa chạy được:** test nào chưa chạy và lý do (ví dụ thiếu Docker service).
6. **Rủi ro và việc còn tồn đọng.**
7. **Việc cố ý chưa làm:** những hạng mục liên quan nhưng nằm ngoài phase hoặc chờ xác nhận.

Lưu ý:
- Với thay đổi nhỏ (sửa tên, sửa lỗi chính tả, chỉnh cấu hình nhỏ), có thể rút gọn mục 2, 3, 6, 7. Mục 4 và 5 luôn phải có.
- Không tuyên bố test hoặc build thành công nếu chưa thực sự chạy lệnh.
- Không tự chuyển trạng thái phase sang `Done` trong bảng roadmap; chỉ chủ dự án chuyển sau khi có bằng chứng kiểm tra.

---

## 4. Lệnh thường dùng

> Cập nhật và hoàn thiện sau khi hoàn thành Phase 0.

Chạy mọi lệnh từ thư mục gốc của repo và không đổi thư mục làm việc của terminal (dùng subshell hoặc `--prefix`), để lệnh sau không chạy nhầm trong `backend/` hay `frontend/`.

```bash
# Hạ tầng (PostgreSQL, Redis) - docker-compose.yml ở thư mục gốc
docker compose up -d

# Backend
(cd backend && ./mvnw clean verify)        # Build + toàn bộ unit và integration test
(cd backend && ./mvnw spring-boot:run)     # Khởi động ứng dụng backend

# Frontend
npm --prefix frontend install
npm --prefix frontend run dev              # Chạy dev server
npm --prefix frontend test                 # Chạy frontend test
```

---

## 5. Lưu ý về Spring Boot 4 (khác với Spring Boot 3)

Phần lớn tài liệu và ví dụ trên mạng viết cho Spring Boot 3. Với dự án này, không dùng cách viết kiểu Boot 3 ở các điểm sau. Nếu không chắc, tra tài liệu chính thức (Spring Boot 4.0 Migration Guide) thay vì đoán.

**Starter đổi tên và tách nhỏ**
- Dùng `spring-boot-starter-webmvc`, không dùng `spring-boot-starter-web`.
- Test được tách theo công nghệ: ví dụ `@AutoConfigureMockMvc` nằm ở `org.springframework.boot.webmvc.test.autoconfigure` và cần dependency test tương ứng (`spring-boot-starter-webmvc-test`), không còn ở `org.springframework.boot.test.autoconfigure.web.servlet`.
- Không dùng `spring-boot-starter-classic` hay `spring-boot-starter-test-classic` (chỉ dành cho việc nâng cấp dự án cũ).
- Flyway, Redis, Security... có thể cần starter riêng để auto-configuration hoạt động. Nếu migration hoặc cấu hình không chạy mà không báo lỗi, kiểm tra dependency starter trước tiên.

**Jackson 3**
- `ObjectMapper` và các lớp Jackson khác dùng package `tools.jackson...`, không dùng `com.fasterxml.jackson...`.
- Annotation như `@JsonProperty` có thể vẫn ở package cũ; build để xác nhận thay vì đoán.
- Không thêm `jackson-databind` thủ công; phiên bản do Spring Boot quản lý.

**Spring Security 7 và các thư viện đi kèm**
- Không sao chép cấu hình `SecurityFilterChain` từ tài liệu viết cho Security 6; kiểm tra API còn tồn tại.
- Chỉ thêm springdoc, Testcontainers, thư viện JWT ở bản đã xác nhận tương thích với Spring Boot 4.

**Khi gặp lỗi biên dịch lạ ở import hoặc dependency:** nguyên nhân thường là dùng tên starter hoặc package của Spring Boot 3. Sửa theo hướng Boot 4, không hạ phiên bản Spring Boot để né lỗi.
