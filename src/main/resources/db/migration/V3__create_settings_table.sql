-- 10. Bảng System Settings (Lưu các cấu hình hệ thống)
CREATE TABLE system_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    group_name VARCHAR(50) DEFAULT 'general' COMMENT 'profile, zalo, email, etc.',
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Init data cho Zalo template
INSERT INTO system_settings (setting_key, setting_value, group_name, description) VALUES
('zalo_header', 'Kính gửi anh/chị [Tên Khách Hàng],', 'zalo', 'Header cho tin nhắn Zalo'),
('zalo_footer', 'Trân trọng cảm ơn. Vui lòng thanh toán vào STK: 123456789 - VCB - Tên Chủ TK', 'zalo', 'Footer cho tin nhắn Zalo');

-- Init data cho Email config
INSERT INTO system_settings (setting_key, setting_value, group_name, description) VALUES
('email_smtp_host', 'smtp.gmail.com', 'email', 'SMTP Server Host'),
('email_smtp_port', '587', 'email', 'SMTP Server Port'),
('email_username', 'admin@greenlogix.com', 'email', 'SMTP Username (Email gửi)'),
('email_app_password', '', 'email', 'SMTP App Password'),
('email_admin_report', 'owner@greenlogix.com', 'email', 'Email nhận báo cáo doanh thu');
