package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.OrderRequest;
import org.example.greenloginbe.dto.OrderResponse;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<OrderResponse> getAllOrders();
    Optional<OrderResponse> getOrderById(Integer id);
    OrderResponse createOrder(OrderRequest request, String username);
    OrderResponse updateOrderStatus(Integer id, String status, String username);
}
