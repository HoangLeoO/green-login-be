INSERT INTO system_settings (setting_key, setting_value, group_name, description) VALUES
('vnp_tmn_code', '2QXG8Q0C', 'vnpay', 'Mã định danh website (TmnCode)'),
('vnp_hash_secret', 'GETITFROMVNPAY', 'vnpay', 'Chuỗi bí mật (HashSecret)'),
('vnp_api_url', 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html', 'vnpay', 'URL thanh toán VNPay Sandbox'),
('vnp_return_url', 'http://localhost:5173/payment-result', 'vnpay', 'URL nhận kết quả sau khi khách trả tiền');
