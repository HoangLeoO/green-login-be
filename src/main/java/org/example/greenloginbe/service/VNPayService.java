package org.example.greenloginbe.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface VNPayService {
    String createPaymentUrl(long amount, String orderInfo, long customerId, HttpServletRequest request);
    int verifyPayment(Map<String, String> queryParams);
}
