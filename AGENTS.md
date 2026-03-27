# Hướng Dẫn Đại Lý Backend GreenLogix

## Tổng Quan Kiến Trúc
Đây là ứng dụng Spring Boot 3.5.11 để quản lý bán rau củ cho các nhà hàng. Các thực thể cốt lõi:
- **Users**: Nhân viên/khách hàng với vai trò (ADMIN/STAFF/CUSTOMER), liên kết với khách hàng nếu vai trò là CUSTOMER
- **Customers**: Các nhà hàng với customer_code duy nhất, thông tin liên hệ
- **Products**: Rau củ với SKU, danh mục, stock_quantity, min_stock_level, default_price
- **Orders**: Đơn hàng bán với các mục, trạng thái (pending/paid/cancelled), total_amount
- **OrderItems**: Các mục hàng với quantity, unit_price, total_price
- **StockMovements**: Điều chỉnh kho (in/out)
- **OrderLogs**: Nhật ký kiểm tra với JSON old_data/new_data cho các thay đổi
- **CustomerFavorites**: Gợi ý mua hàng với frequency_score

Kiến trúc phân lớp: Controllers → Services (interfaces + Impl) → Repositories → Entities.

## Quy Ước Chính
- **Database**: MySQL 8.4, schema `db_green`, Flyway migrations trong `src/main/resources/db/migration/`
- **Security**: Xác thực JWT, phiên stateless, CORS được bật, vai trò qua @PreAuthorize
- **Data Types**: BigDecimal cho giá trị tiền tệ (precision 15,2) và số lượng (precision 10,2 cho order items)
- **Entities**: Lombok @Getter/@Setter, @JsonIgnoreProperties cho lazy loading, @OnDelete actions
- **DTOs**: DTOs yêu cầu/phản hồi riêng biệt trong package `dto/`
- **Pagination**: Spring Data Pageable với sắp xếp tùy chỉnh (ví dụ: `?sort=id,desc`)
- **Validation**: Jakarta @Valid, @NotNull, @Size trên entities và DTOs
- **Logging**: Thay đổi đơn hàng được ghi với action_type (CREATE/UPDATE_ITEM/ADD_ITEM/CANCEL/PAYMENT)

## Quy Trình Phát Triển
- **Build**: `./gradlew build` (bao gồm tests, loại trừ với `-x test`)
- **Run Locally**: Đặt biến môi trường (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET), `./gradlew bootRun`
- **Docker**: `docker-compose up --build` (bao gồm MySQL 8.4, migrations tự động chạy)
- **Database Reset**: Xóa schema `db_green`, khởi động lại app (Flyway baseline-on-migrate=true)
- **API Testing**: Import `postman_collection.json`, đặt base URL thành http://localhost:8080

## Điểm Tích Hợp
- **External DB**: Cấu hình qua biến môi trường, không ghi đè ORM ngoại trừ MySQL8Dialect
- **JWT**: JwtUtils tùy chỉnh, thời gian hết hạn có thể cấu hình (mặc định 1h)
- **Flyway**: Migrations chạy khi khởi động, baseline được bật cho DBs hiện có

## Mẫu Code
- **Controllers**: @CrossOrigin("*"), @RequestMapping("/api/{resource}"), ResponseEntity<Page<T>> cho danh sách
- **Services**: Interface trong `service/`, Impl trong `service/` với @Service, @Transactional cho cập nhật
- **Repositories**: Spring Data JPA, truy vấn tùy chỉnh qua @Query nếu cần
- **Error Handling**: GlobalExceptionHandler cho phản hồi nhất quán
- **Stock Updates**: Tự động giảm kho qua StockMovementService khi đơn hàng được xác nhận hoặc thanh toán trong OrderServiceImpl.

Tệp tham khảo: `entity/` cho mô hình miền, `controller/OrderController.java` cho mẫu CRUD, `V1__init_project.sql` cho schema, `docker-compose.yml` cho triển khai.

## Biến Môi Trường Quan Trọng

- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` — cấu hình kết nối MySQL. `docker-compose.yml` và ứng dụng sử dụng những biến này. Ví dụ: DB_URL=jdbc:mysql://localhost:3306/db_green?createDatabaseIfNotExist=true
- `JWT_SECRET` — khóa bí mật cho JWT (KHÔNG commit vào git). Ví dụ: JWT_SECRET=your_long_secret_here
- `JWT_EXPIRATION` — thời gian sống token (ms), mặc định 3600000 (1 giờ).
- `FLYWAY_ENABLED` — bật/tắt Flyway (true/false). Mặc định true.

- CORS (mapping Spring properties): Spring property `cors.allowed-origins` được ánh xạ từ env `CORS_ALLOWED_ORIGINS`. Các biến liên quan:
  - `CORS_ALLOWED_ORIGINS` (ví dụ: http://localhost:5173,http://localhost:3000)
  - `CORS_ALLOWED_METHODS` (ví dụ: GET,POST,PUT,PATCH,DELETE,OPTIONS)
  - `CORS_ALLOWED_HEADERS` (ví dụ: authorization,content-type,x-auth-token)
  - `CORS_EXPOSED_HEADERS` (ví dụ: x-auth-token)
  - `CORS_ALLOW_CREDENTIALS` (true/false)

- Lưu ý: tên biến môi trường thường ở dạng UPPERCASE; Spring biến `.` thành `_` khi đọc từ env (ví dụ `cors.allowed-origins` → `CORS_ALLOWED_ORIGINS`).

## Ví dụ nhanh

- Chạy local (PowerShell): nạp `.env` rồi chạy
```powershell
Get-Content .env |
  ForEach-Object {
	if ($_ -and -not $_.TrimStart().StartsWith('#')) {
	  $parts = $_ -split '=', 2
	  if ($parts.Count -eq 2) { $envName = $parts[0].Trim(); $envVal = $parts[1].Trim(); $env:$envName = $envVal }
	}
  }
.\gradlew.bat bootRun
```

- Chạy Docker Compose (đọc `.env` tự động):
```powershell
docker-compose up --build
```

## Cấu hình CORS cho Docker Compose

Khi chạy bằng Docker Compose, backend container nhận các biến môi trường từ file `.env` (nằm cùng thư mục với `docker-compose.yml`) hoặc từ biến môi trường của hệ thống. Nếu bạn muốn chỉ định giá trị cụ thể cho CORS trong `docker-compose.yml`, thêm các biến vào phần `environment` của service `backend`. Ví dụ:

```yaml
services:
  backend:
    environment:
      - CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
      - CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS
      - CORS_ALLOWED_HEADERS=authorization,content-type,x-auth-token
      - CORS_EXPOSED_HEADERS=x-auth-token
      - CORS_ALLOW_CREDENTIALS=true
```

Ghi chú:
- Docker Compose sẽ ưu tiên giá trị đặt trực tiếp trong `docker-compose.yml`; nếu bạn dùng `.env`, đặt biến vào đó cũng sẽ được đọc tự động.
- Sau khi sửa `.env` hoặc `docker-compose.yml`, chạy lại `docker-compose up --build` hoặc `docker-compose up -d --force-recreate` để áp dụng thay đổi.
- Tránh dùng `*` cho `CORS_ALLOWED_ORIGINS` khi `CORS_ALLOW_CREDENTIALS=true` vì trình duyệt sẽ chặn.


