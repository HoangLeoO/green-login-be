package org.example.greenloginbe.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {
    private Integer customerId;
    private String notes;
    private String orderDate; // format: "yyyy-MM-dd"
    
    // Khi tạo đơn hàng cần danh sách món
    private List<OrderItemRequest> items;
}
