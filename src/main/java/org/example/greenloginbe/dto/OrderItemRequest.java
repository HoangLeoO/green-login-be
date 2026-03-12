package org.example.greenloginbe.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemRequest {
    private Integer productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
}
