
-- 1. Bảng Users (Người sử dụng hệ thống/Người bán)
CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       display_name VARCHAR(100) NOT NULL,
                       role VARCHAR(20) DEFAULT 'STAFF' COMMENT 'ADMIN, STAFF, CUSTOMER',
                       customer_id INT DEFAULT NULL COMMENT 'Liên kết nếu role là CUSTOMER',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Customers (Khách hàng - Nhà hàng/Quán ăn)
CREATE TABLE customers (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           customer_code VARCHAR(50) NOT NULL UNIQUE,
                           name VARCHAR(150) NOT NULL,
                           phone VARCHAR(20) NOT NULL,
                           email VARCHAR(150),
                           address VARCHAR(255),
                           notes TEXT,
                           status VARCHAR(20) DEFAULT 'approved' COMMENT 'pending, approved, rejected',
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2b. Bảng Customer Branches (Các chi nhánh của khách hàng)
CREATE TABLE customer_branches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    branch_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

-- Thêm Foreign Key cho bảng Users liên kết đến Customers
-- Lưu ý: Thực hiện sau khi cả 2 bảng đã được tạo
ALTER TABLE users ADD FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

-- 3. Bảng Categories (Nhóm danh mục rau củ)
CREATE TABLE categories (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            description TEXT
);

-- 4. Bảng Products (Danh sách Sản phẩm/Rau củ)
CREATE TABLE products (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          sku VARCHAR(50) NOT NULL UNIQUE,
                          category_id INT,
                          name VARCHAR(150) NOT NULL,
                          unit VARCHAR(50) NOT NULL COMMENT 'kg, bó, thùng...',
                          default_price DECIMAL(15, 2) NOT NULL DEFAULT 0,
                          stock_quantity DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT 'Số lượng tồn kho',
                          min_stock_level DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT 'Mức cảnh báo sắp hết',
                          is_active BOOLEAN DEFAULT TRUE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 5. Bảng Orders (Đơn hàng)
CREATE TABLE orders (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        order_code VARCHAR(50) NOT NULL UNIQUE,
                        customer_id INT NOT NULL,
                        branch_id INT DEFAULT NULL COMMENT 'Chi nhánh giao hàng',
                        user_id INT NOT NULL,
                        total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
                        status VARCHAR(50) DEFAULT 'pending' COMMENT 'pending: chưa thanh toán, paid: đã thanh toán, cancelled: đã huỷ',
                        notes TEXT,
                        order_date DATE NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (customer_id) REFERENCES customers(id),
                        FOREIGN KEY (branch_id) REFERENCES customer_branches(id) ON DELETE SET NULL,
                        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 6. Bảng Order_Items (Chi tiết Đơn hàng)
CREATE TABLE order_items (
                             id INT AUTO_INCREMENT PRIMARY KEY,
                             order_id INT NOT NULL,
                             product_id INT NOT NULL,
                             quantity DECIMAL(10, 2) NOT NULL COMMENT 'Có thể bán theo số thập phân như 1.5kg',
                             unit_price DECIMAL(15, 2) NOT NULL,
                             total_price DECIMAL(15, 2) NOT NULL,
                             FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 7. (Optional) Bảng Order_Logs (Lưu lịch sử cập nhật để theo dõi "Cập nhật đơn khi khách gọi thêm")
CREATE TABLE order_logs (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            order_id INT NOT NULL,
                            user_id INT NOT NULL,
                            action_type VARCHAR(50) NOT NULL COMMENT 'CREATE, UPDATE_ITEM, ADD_ITEM, CANCEL, PAYMENT',
                            old_data JSON,
                            new_data JSON,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (order_id) REFERENCES orders(id),
                            FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 8. Bảng Customer_Favorites (Phục vụ "Gợi ý mua hàng thông minh")
CREATE TABLE customer_favorites (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    customer_id INT NOT NULL,
                                    product_id INT NOT NULL,
                                    default_quantity DECIMAL(10, 2) NOT NULL DEFAULT 1,
                                    frequency_score INT DEFAULT 1 COMMENT 'Điểm tần suất để sắp xếp độ ưu tiên, tự động tăng khi đặt',
                                    last_ordered_at TIMESTAMP,
                                    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
                                    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                                    UNIQUE(customer_id, product_id)
);

-- 9. Bảng Stock_Movements (Lịch sử biến động kho)
CREATE TABLE stock_movements (
                                 id INT AUTO_INCREMENT PRIMARY KEY,
                                 product_id INT NOT NULL,
                                 movement_type VARCHAR(50) NOT NULL COMMENT 'IN, OUT, ADJUSTMENT',
                                 quantity DECIMAL(15, 2) NOT NULL,
                                 notes TEXT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
