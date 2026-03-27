
-- Dữ liệu mẫu cho Categories
INSERT INTO categories (name, description) VALUES 
('Rau ăn lá', 'Các loại rau xanh như xà lách, cải, muống...'),
('Củ quả', 'Các loại củ như cà rốt, khoai tây, bầu, bí...'),
('Gia vị', 'Hành, tỏi, ớt, gừng...'),
('Trái cây', 'Các loại quả tươi cho nhà hàng');

-- Dữ liệu mẫu cho Products
INSERT INTO products (sku, category_id, name, unit, default_price, stock_quantity, min_stock_level, is_active) VALUES
('RM-001', 1, 'Rau muống', 'kg', 15000, 100, 10, 1),
('XL-002', 1, 'Xà lách', 'kg', 25000, 50, 5, 1),
('CR-003', 2, 'Cà rốt', 'kg', 22000, 200, 20, 1),
('KT-004', 2, 'Khoai tây', 'kg', 28000, 150, 15, 1),
('HL-005', 3, 'Hành lá', 'bó', 5000, 80, 10, 1),
('OH-006', 3, 'Ớt hiểm', 'kg', 50000, 20, 2, 1);

-- Dữ liệu mẫu cho Customers
INSERT INTO customers (customer_code, name, phone, email, address, notes) VALUES
('KH-SV-001', 'Nhà hàng Sen Vàng', '0912345678', 'senvang@gmail.com', '123 Đường ABC, Hà Nội', 'Giao hàng trước 8h sáng'),
('KH-CB-002', 'Quán cơm Cô Ba', '0987654321', 'coba@gmail.com', '456 Đường XYZ, Hà Nội', 'Thanh toán theo tuần'),
('KH-ST-003', 'Bún chả Sinh Từ', '0901234567', null, '789 Đường Láng, Hà Nội', null),
('KH-NEW-001', 'Lẩu Phan - Cơ sở mới', '0999888777', 'phan@gmail.com', '99 Cầu Giấy, Hà Nội', 'Đăng ký qua form website');

-- Cập nhật status cho khách hàng đăng ký qua website
UPDATE customers SET status = 'pending' WHERE customer_code = 'KH-NEW-001';

-- Dữ liệu mẫu cho Customer Branches
INSERT INTO customer_branches (customer_id, branch_name, phone, address, notes) VALUES
(1, 'CN Sen Vàng Tây Hồ', '0911111111', '10 Thụy Khuê, Tây Hồ', 'Giao cổng sau'),
(1, 'CN Sen Vàng Hà Đông', '0922222222', '20 Quang Trung, Hà Đông', null),
(2, 'Cô Ba Cầu Giấy', '0933333333', '15 Xuân Thủy, Cầu Giấy', null);

-- Dữ liệu mẫu cho Users (Password: password123)
-- Admin
INSERT INTO users (username, password_hash, display_name, role) VALUES
('admin123', '$2a$10$4v3FMh3e4qQ/IZBSrvjLAOhw6lK/i7/46ozCo/kKCsJPSVHNCkCca', 'Hệ thống Admin', 'ADMIN');
-- Staff
INSERT INTO users (username, password_hash, display_name, role) VALUES
('staff01', '$2a$10$4v3FMh3e4qQ/IZBSrvjLAOhw6lK/i7/46ozCo/kKCsJPSVHNCkCca', 'Nguyễn Văn Nhân Viên', 'STAFF');

-- Dữ liệu mẫu cho Orders
INSERT INTO orders (order_code, customer_id, branch_id, user_id, total_amount, status, notes, order_date) VALUES
('HD260311-001', 1, 1, 2, 62000, 'paid', 'Đơn hàng giao sớm', '2026-03-11'),
('HD260312-001', 2, 3, 2, 450000, 'pending', 'Khách nợ', '2026-03-12');

-- Dữ liệu mẫu cho Order Items
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price) VALUES
(1, 1, 2, 15000, 30000),
(1, 3, 1, 22000, 22000),
(1, 5, 2, 5000, 10000),
(2, 6, 9, 50000, 450000);

-- Dữ liệu mẫu cho Customer Favorites
INSERT INTO customer_favorites (customer_id, product_id, default_quantity, frequency_score) VALUES
(1, 1, 5, 10),
(1, 3, 2, 8),
(2, 6, 1, 15);

-- Dữ liệu mẫu cho Stock Movements
INSERT INTO stock_movements (product_id, movement_type, quantity, notes) VALUES
(1, 'IN', 100, 'Nhập hàng đầu ngày'),
(3, 'IN', 200, 'Nhập hàng từ kho tổng');
