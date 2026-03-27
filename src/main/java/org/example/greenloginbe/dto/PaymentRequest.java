package org.example.greenloginbe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Integer customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
    private String notes;
}
