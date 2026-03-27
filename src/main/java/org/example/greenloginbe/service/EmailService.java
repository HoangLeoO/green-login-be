package org.example.greenloginbe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.greenloginbe.dto.OrderResponse;
import org.example.greenloginbe.entity.Order;
import org.example.greenloginbe.entity.OrderItem;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SettingService settingService;

    private JavaMailSenderImpl getDynamicMailSender() {
        if (!(mailSender instanceof JavaMailSenderImpl)) {
            return (JavaMailSenderImpl) mailSender;
        }
        
        JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
        
        // Lấy cấu hình từ DB theo đúng Key trong file Migration V3
        String host = settingService.getSettingValue("email_smtp_host", null);
        String port = settingService.getSettingValue("email_smtp_port", null);
        String user = settingService.getSettingValue("email_username", null);
        String pass = settingService.getSettingValue("email_app_password", null);
        
        if (host != null && !host.isEmpty()) sender.setHost(host);
        if (port != null && !port.isEmpty()) {
            try {
                sender.setPort(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                log.error("Invalid MAIL_PORT in settings: {}", port);
            }
        }
        if (user != null && !user.isEmpty()) sender.setUsername(user);
        if (pass != null && !pass.isEmpty()) sender.setPassword(pass);

        // Bổ sung các thuộc tính quan trọng cho Gmail/Outlook
        java.util.Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        
        return sender;
    }

    public void sendEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, true);
    }

    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        try {
            JavaMailSenderImpl dynamicSender = getDynamicMailSender();
            log.info("Attempting to send email via host: {}, user: {}", dynamicSender.getHost(), dynamicSender.getUsername());
            
            MimeMessage message = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(dynamicSender.getUsername(), "GreenLogix System");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, isHtml);
            
            dynamicSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            // Vì chạy Async nên exception này sẽ chỉ xuất hiện ở log, không làm đứng giao diện
        }
    }

    @Async
    public void sendInvoiceEmail(OrderResponse order) {
        String customerEmail = order.getCustomerEmail();
        if (customerEmail == null || customerEmail.isEmpty()) {
            log.warn("Order {} has no customer email, skipping invoice email", order.getOrderCode());
            return;
        }

        String subject = "Hóa đơn điện tử - " + order.getOrderCode() + " - GreenLogix";
        
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px;'>");
        html.append("<h2 style='color: #10b981; text-align: center;'>HÓA ĐƠN BÁN HÀNG</h2>");
        html.append("<p>Xin chào <strong>").append(order.getCustomerName()).append("</strong>,</p>");
        html.append("<p>Cảm ơn bạn đã tin tưởng sử dụng sản phẩm của <strong>GreenLogix</strong>. Dưới đây là thông báo thanh toán cho đơn hàng của bạn:</p>");
        
        html.append("<div style='background: #f9fafb; padding: 15px; border-radius: 8px; margin-bottom: 20px;'>");
        html.append("<p style='margin: 0;'><strong>Mã đơn hàng:</strong> ").append(order.getOrderCode()).append("</p>");
        html.append("<p style='margin: 0;'><strong>Ngày đặt:</strong> ").append(order.getOrderDate()).append("</p>");
        html.append("<p style='margin: 0;'><strong>Trạng thái:</strong> ").append(order.getStatus()).append("</p>");
        html.append("</div>");
 
        html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        html.append("<thead><tr style='background: #10b981; color: white;'>");
        html.append("<th style='padding: 10px; text-align: left;'>Sản phẩm</th>");
        html.append("<th style='padding: 10px; text-align: right;'>SL</th>");
        html.append("<th style='padding: 10px; text-align: right;'>Đơn giá</th>");
        html.append("<th style='padding: 10px; text-align: right;'>Thành tiền</th>");
        html.append("</tr></thead><tbody>");
 
        for (org.example.greenloginbe.dto.OrderItemResponse item : order.getItems()) {
            java.math.BigDecimal uPrice = item.getUnitPrice() != null ? item.getUnitPrice() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal tPrice = item.getTotalPrice() != null ? item.getTotalPrice() : java.math.BigDecimal.ZERO;

            html.append("<tr>");
            html.append("<td style='padding: 10px; border-bottom: 1px solid #eee;'>").append(item.getProductName()).append("</td>");
            html.append("<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'>").append(item.getQuantity()).append(" ").append(item.getUnit()).append("</td>");
            html.append("<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'>").append(String.format("%,.0f", uPrice)).append("đ</td>");
            html.append("<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'><strong>").append(String.format("%,.0f", tPrice)).append("đ</strong></td>");
            html.append("</tr>");
        }
 
        html.append("</tbody></table>");
 
        java.math.BigDecimal finalTotal = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;
        html.append("<div style='text-align: right; font-size: 18px;'>");
        html.append("<p><strong>TỔNG CỘNG: <span style='color: #10b981;'>").append(String.format("%,.0f", finalTotal)).append("đ</span></strong></p>");
        html.append("</div>");
 
        html.append("<hr style='border: 0; border-top: 1px solid #eee; margin: 30px 0;'>");
        html.append("<p style='font-size: 12px; color: #666; text-align: center;'>Đây là email tự động từ hệ thống quản lý GreenLogix. Vui lòng không trả lời email này.</p>");
        html.append("</div></body></html>");
 
        sendEmail(customerEmail, subject, html.toString(), true);
    }
}
