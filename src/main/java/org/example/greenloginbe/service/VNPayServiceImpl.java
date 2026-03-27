package org.example.greenloginbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    private static final String VNP_VERSION    = "2.1.0";
    private static final String VNP_COMMAND    = "pay";
    private static final String VNP_ORDER_TYPE = "other";
    private static final String VNP_CURRENCY   = "VND";
    private static final String VNP_LOCALE     = "vn";

    // SecureRandom là thread-safe và cryptographically strong
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String createPaymentUrl(long amount, String orderInfo, long customerId, HttpServletRequest request) {
        String txnRef    = customerId + "_" + getRandomNumber(8);
        String ipAddr    = getClientIp(request);
        String createDate;
        String expireDate;

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        createDate = formatter.format(cld.getTime());
        cld.add(Calendar.MINUTE, 15);
        expireDate = formatter.format(cld.getTime());

        Map<String, String> vnpParams = new TreeMap<>(); // TreeMap tự sắp xếp ABC, không cần sort thêm
        vnpParams.put("vnp_Version",    VNP_VERSION);
        vnpParams.put("vnp_Command",    VNP_COMMAND);
        vnpParams.put("vnp_TmnCode",    tmnCode);
        vnpParams.put("vnp_Amount",     String.valueOf(amount * 100));
        vnpParams.put("vnp_CurrCode",   VNP_CURRENCY);
        vnpParams.put("vnp_TxnRef",     txnRef);
        vnpParams.put("vnp_OrderInfo",  orderInfo);
        vnpParams.put("vnp_OrderType",  VNP_ORDER_TYPE);
        vnpParams.put("vnp_Locale",     VNP_LOCALE);
        vnpParams.put("vnp_ReturnUrl",  returnUrl);
        vnpParams.put("vnp_IpAddr",     ipAddr);
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query    = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String key   = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                    query.append('&');
                }
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
                hashData.append(key).append('=').append(encodedValue);
                query.append(URLEncoder.encode(key, StandardCharsets.UTF_8)).append('=').append(encodedValue);
                first = false;
            }
        }

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        return payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public int verifyPayment(Map<String, String> queryParams) {
        String receivedHash = queryParams.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            log.warn("[VNPay VERIFY] Thiếu vnp_SecureHash trong callback.");
            return 0;
        }

        // Clone và loại bỏ các trường không tham gia ký
        Map<String, String> fields = new TreeMap<>(queryParams);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                if (!first) hashData.append('&');
                hashData.append(entry.getKey()).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                first = false;
            }
        }

        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        if (calculatedHash == null) return 0;

        if (!calculatedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("[VNPay VERIFY] ❌ Chữ ký không khớp! TxnRef={}", queryParams.get("vnp_TxnRef"));
            return 0;
        }

        String responseCode = queryParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            log.info("[VNPay VERIFY] ✅ Xác thực thành công. TxnRef={}", queryParams.get("vnp_TxnRef"));
            return 1;
        }

        log.info("[VNPay VERIFY] Giao dịch không thành công. ResponseCode={}", responseCode);
        return 0;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            hmac512.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", Byte.toUnsignedInt(b)));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error("[VNPay] HMAC-SHA512 error: {}", ex.getMessage());
            return null;
        }
    }

    /** Lấy IP thực của client, xử lý cả trường hợp đứng sau proxy/load balancer */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For có thể chứa danh sách "client, proxy1, proxy2"
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String getRandomNumber(int len) {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
