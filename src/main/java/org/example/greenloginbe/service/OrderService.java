package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.OrderRequest;
import org.example.greenloginbe.dto.OrderResponse;
import org.example.greenloginbe.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderService {
    Page<OrderResponse> getAllOrders(String status, Integer customerId, String startDate, String endDate, String search, Pageable pageable);
    Optional<OrderResponse> getOrderById(Integer id);
    OrderResponse createOrder(OrderRequest request, String username);
    OrderResponse updateOrderStatus(Integer id, OrderStatus status, String username);
    void sendInvoice(Integer orderId);
    OrderResponse copyOrder(Integer orderId, String username);
}
