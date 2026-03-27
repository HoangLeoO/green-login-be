-- Add Bank Settings for VietQR
INSERT INTO system_settings (setting_key, setting_value, group_name, description) VALUES
('bank_code', 'VCB', 'payment', 'Mã ngân hàng (VD: VCB, TCB, MB)'),
('bank_account', '123456789', 'payment', 'Số tài khoản nhận tiền'),
('bank_account_name', 'CONG TY GREENLOGIX', 'payment', 'Tên chủ tài khoản (Không dấu)'),
('vietqr_template', 'qr_only', 'payment', 'Template của VietQR (compact, qr_only, etc.)');
