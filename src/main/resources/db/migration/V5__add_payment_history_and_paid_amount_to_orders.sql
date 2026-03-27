-- Thêm cột số tiền đã trả vào bảng orders để theo dõi chi tiết
ALTER TABLE orders 
ADD COLUMN paid_amount DECIMAL(15, 2) DEFAULT 0.00;

-- Bảng lưu lịch sử thanh toán nợ của khách hàng (Phiếu thu)
CREATE TABLE customer_payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL COMMENT 'Số tiền nộp (Ví dụ: 50,000)',
    payment_method VARCHAR(50) NOT NULL COMMENT 'CASH, BANK, MOMO...',
    transaction_id VARCHAR(100) NULL COMMENT 'Mã tham chiếu giao dịch',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    user_id INT NOT NULL COMMENT 'Nhân viên thu tiền',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Bảng trung gian lưu vết gạch nợ (Liên kết phiếu thu với các đơn hàng cụ thể)
CREATE TABLE payment_allocations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    order_id INT NOT NULL,
    allocated_amount DECIMAL(15, 2) NOT NULL COMMENT 'Số tiền gạch nợ cho đơn này',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES customer_payments(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
