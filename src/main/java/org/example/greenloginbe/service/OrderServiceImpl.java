package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.OrderItemRequest;
import org.example.greenloginbe.dto.OrderItemResponse;
import org.example.greenloginbe.dto.OrderRequest;
import org.example.greenloginbe.dto.OrderResponse;
import org.example.greenloginbe.entity.*;
import org.example.greenloginbe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrderResponse> getOrderById(Integer id) {
        return orderRepository.findById(id).map(this::mapToOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setUser(user);
        order.setStatus("pending");
        order.setNotes(request.getNotes());
        order.setOrderDate(request.getOrderDate() != null ? LocalDate.parse(request.getOrderDate()) : LocalDate.now());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        Order savedOrder = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            // Tạo OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(itemReq.getUnitPrice());
            
            BigDecimal itemTotal = itemReq.getQuantity().multiply(itemReq.getUnitPrice());
            orderItem.setTotalPrice(itemTotal);
            orderItemRepository.save(orderItem);

            totalAmount = totalAmount.add(itemTotal);

            // Trừ tồn kho
            product.setStockQuantity(product.getStockQuantity().subtract(itemReq.getQuantity()));
            productRepository.save(product);

            // Lưu lịch sử biến động kho
            StockMovement movement = new StockMovement();
            movement.setProduct(product);
            movement.setMovementType("OUT");
            movement.setQuantity(itemReq.getQuantity());
            movement.setNotes("Bán hàng cho đơn #" + savedOrder.getId());
            movement.setCreatedAt(Instant.now());
            stockMovementRepository.save(movement);
        }

        savedOrder.setTotalAmount(totalAmount);
        savedOrder = orderRepository.save(savedOrder);

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer id, String status, String username) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        
        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        
        // Cần lưu thêm OrderLog nếu trạng thái là CANCELLED thì hoàn tồn kho, v.v..
        // ... (Tuỳ luồng nghiệp vụ mở rộng sau)

        return mapToOrderResponse(orderRepository.save(order));
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomer().getId());
        response.setCustomerName(order.getCustomer().getName());
        response.setUserId(order.getUser().getId());
        response.setCreatedBy(order.getUser().getDisplayName());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setNotes(order.getNotes());
        response.setOrderDate(order.getOrderDate());
        response.setCreatedAt(order.getCreatedAt());

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream().map(item -> {
            OrderItemResponse itemRes = new OrderItemResponse();
            itemRes.setId(item.getId());
            itemRes.setProductId(item.getProduct().getId());
            itemRes.setProductName(item.getProduct().getName());
            itemRes.setUnit(item.getProduct().getUnit());
            itemRes.setQuantity(item.getQuantity());
            itemRes.setUnitPrice(item.getUnitPrice());
            itemRes.setTotalPrice(item.getTotalPrice());
            return itemRes;
        }).collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;
    }
}
