package org.example.greenloginbe.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.greenloginbe.dto.PaymentRequest;
import org.example.greenloginbe.service.CustomerPaymentService;
import org.example.greenloginbe.service.VNPayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnpayService;
    private final CustomerPaymentService customerPaymentService;

    @Value("${vnpay.frontend-result-url}")
    private String frontendResultUrl;

    /**
     * Tạo URL thanh toán VNPay.
     * txnRef được embed username vào để callback có thể lấy lại, tránh mất session khi restart.
     * Format: {customerId}_{username}_{random8}
     */
    @PostMapping("/create-payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<String> createPayment(
            @RequestParam long amount,
            @RequestParam String orderInfo,
            @RequestParam long customerId,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "system";

        String paymentUrl = vnpayService.createPaymentUrl(amount, orderInfo, customerId, request);
        log.info("[VNPay] Tạo URL thanh toán: CustomerId={}, User={}", customerId, currentUsername);
        return ResponseEntity.ok(paymentUrl);
    }

    /**
     * VNPay gọi về đây sau khi giao dịch hoàn tất.
     * Sau khi xử lý DB, redirect về frontend với kết quả.
     */
    @GetMapping("/payment-callback")
    public ResponseEntity<Void> paymentCallback(@RequestParam Map<String, String> queryParams) {
        int result     = vnpayService.verifyPayment(queryParams);
        String txnRef  = queryParams.get("vnp_TxnRef");
        String amountStr    = queryParams.get("vnp_Amount");
        String transactionNo= queryParams.get("vnp_TransactionNo");
        String responseCode = queryParams.getOrDefault("vnp_ResponseCode", "unknown");

        log.info("[VNPay CALLBACK] TxnRef={}, ResponseCode={}", txnRef, responseCode);

        if (result == 1) {
            try {
                // txnRef format: {customerId}_{random}  (sinh bởi VNPayServiceImpl)
                Integer customerId = Integer.parseInt(txnRef.split("_")[0]);
                BigDecimal amount  = new BigDecimal(amountStr).divide(BigDecimal.valueOf(100));

                // Luôn dùng "system" cho các callback không xác định được user
                // (callback đến từ VNPay server, không có JWT)
                String username = "system";

                log.info("[VNPay CALLBACK] Xử lý thanh toán: CustomerId={}, Amount={}", customerId, amount);

                PaymentRequest paymentRequest = new PaymentRequest();
                paymentRequest.setCustomerId(customerId);
                paymentRequest.setAmount(amount);
                paymentRequest.setPaymentMethod("VNPAY");
                paymentRequest.setTransactionId(transactionNo);
                paymentRequest.setNotes("Thanh toán online qua VNPay. Mã GD: " + transactionNo);

                customerPaymentService.createPayment(paymentRequest, username);
                log.info("[VNPay CALLBACK] ✅ Thành công! CustomerId={}, Amount={}", customerId, amount);

                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendResultUrl
                                + "?vnp_ResponseCode=00"
                                + "&vnp_Amount=" + amountStr
                                + "&vnp_TxnRef=" + txnRef
                                + "&vnp_TransactionNo=" + transactionNo))
                        .build();

            } catch (Exception e) {
                log.error("[VNPay CALLBACK] ❌ Lỗi xử lý callback: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendResultUrl + "?vnp_ResponseCode=99&error=server_error"))
                        .build();
            }
        }

        log.info("[VNPay CALLBACK] Giao dịch thất bại. ResponseCode={}", responseCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendResultUrl + "?vnp_ResponseCode=" + responseCode))
                .build();
    }
}
