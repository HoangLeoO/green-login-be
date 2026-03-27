package org.example.greenloginbe.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CustomerFavoriteResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productUnit;
    private BigDecimal defaultQuantity;
    private Integer frequencyScore;
    private Instant lastOrderedAt;
    
    // Bổ sung thêm thông tin sản phẩm để hiển thị trên UI
    private BigDecimal currentPrice;
    private BigDecimal stockQuantity;
    private String status;
}
