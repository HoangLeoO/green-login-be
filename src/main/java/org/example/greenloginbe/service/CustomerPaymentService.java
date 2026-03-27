package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.PaymentRequest;
import org.example.greenloginbe.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerPaymentService {
    PaymentResponse createPayment(PaymentRequest request, String username);
    List<PaymentResponse> getPaymentsByCustomerId(Integer customerId);
    Page<PaymentResponse> getAllPayments(Pageable pageable);
}
