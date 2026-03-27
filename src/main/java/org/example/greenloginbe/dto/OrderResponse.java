package org.example.greenloginbe.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class OrderResponse {
    private Integer id;
    private String orderCode;
    private Integer customerId;
    private String customerName;
    private Integer branchId;
    private String branchName;
    private Integer userId;
    private String createdBy;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private LocalDate orderDate;
    private Instant createdAt;
    
    private String customerEmail;
    private List<OrderItemResponse> items;
}
