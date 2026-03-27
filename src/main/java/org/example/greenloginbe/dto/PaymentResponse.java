package org.example.greenloginbe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Integer id;
    private Integer customerId;
    private String customerName;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
    private Instant paymentDate;
    private String notes;
    private String createdBy;
    private List<AllocationDetail> allocations;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AllocationDetail {
        private String orderCode;
        private BigDecimal allocatedAmount;
    }
}
