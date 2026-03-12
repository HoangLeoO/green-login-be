package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.OrderItemRequest;
import org.example.greenloginbe.dto.OrderItemResponse;
import org.example.greenloginbe.dto.OrderRequest;
import org.example.greenloginbe.dto.OrderResponse;
import org.example.greenloginbe.entity.*;
import org.example.greenloginbe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private OrderLogRepository orderLogRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Override
    public Page<OrderResponse> getAllOrders(String status, Integer customerId, String startDate, String endDate, String search, Pageable pageable) {
        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : null;
        String searchStr = (search != null && !search.isEmpty()) ? search : null;

        return orderRepository.filterOrders(status, customerId, start, end, searchStr, pageable)
                .map(this::mapToOrderResponse);
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

        // ── Bước 1: Validate toàn bộ tồn kho TRƯỚC khi lưu bất cứ thứ gì ──
        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + itemReq.getProductId()));

            BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
            if (currentStock.compareTo(itemReq.getQuantity()) < 0) {
                throw new RuntimeException(
                    "Sản phẩm \"" + product.getName() + "\" chỉ còn " + currentStock
                    + " " + product.getUnit() + " trong kho, không đủ để bán "
                    + itemReq.getQuantity() + " " + product.getUnit() + "!"
                );
            }
        }

        // ── Bước 2: Tạo mã đơn hàng tự động: HD{yyMMdd}-{randShort} ──
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String orderCode = "HD" + datePart + "-" + randPart;

        Order order = new Order();
        order.setOrderCode(orderCode);
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

            // Trừ tồn kho (đã validated ở trên nên chắc chắn không âm)
            if (product.getStockQuantity() == null) product.setStockQuantity(BigDecimal.ZERO);
            product.setStockQuantity(product.getStockQuantity().subtract(itemReq.getQuantity()));
            productRepository.save(product);

            // Lưu lịch sử biến động kho
            StockMovement movement = new StockMovement();
            movement.setProduct(product);
            movement.setMovementType("OUT");
            movement.setQuantity(itemReq.getQuantity());
            movement.setNotes("Bán hàng - Đơn " + savedOrder.getOrderCode());
            movement.setCreatedAt(Instant.now());
            stockMovementRepository.save(movement);
        }

        savedOrder.setTotalAmount(totalAmount);
        savedOrder = orderRepository.save(savedOrder);

        // Ghi log tạo đơn
        saveOrderLog(savedOrder, user, "CREATE", null, "Tạo đơn hàng mới");

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer id, String status, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        
        String oldStatus = order.getStatus();
        if (oldStatus.equals(status)) return mapToOrderResponse(order);

        // Nếu chuyển sang CANCELLED, hoàn lại tồn kho
        if ("cancelled".equalsIgnoreCase(status) && !"cancelled".equalsIgnoreCase(oldStatus)) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                Product product = item.getProduct();
                if (product.getStockQuantity() == null) product.setStockQuantity(BigDecimal.ZERO);
                product.setStockQuantity(product.getStockQuantity().add(item.getQuantity()));
                productRepository.save(product);

                // Log biến động kho (IN - Hoàn hàng)
                StockMovement movement = new StockMovement();
                movement.setProduct(product);
                movement.setMovementType("IN");
                movement.setQuantity(item.getQuantity());
                movement.setNotes("Hoàn hàng từ đơn hủy " + order.getOrderCode());
                movement.setCreatedAt(Instant.now());
                stockMovementRepository.save(movement);
            }
        }

        order.setStatus(status.toLowerCase());
        order.setUpdatedAt(Instant.now());
        Order updatedOrder = orderRepository.save(order);

        // Ghi log cập nhật trạng thái
        saveOrderLog(updatedOrder, user, "UPDATE_STATUS", oldStatus, "Cập nhật trạng thái từ " + oldStatus + " sang " + status);

        return mapToOrderResponse(updatedOrder);
    }

    private void saveOrderLog(Order order, User user, String action, String oldStatus, String notes) {
        OrderLog log = new OrderLog();
        log.setOrder(order);
        log.setUser(user);
        log.setActionType(action);

        // Ghi thông tin vào newData (JSON)
        java.util.Map<String, Object> newData = new java.util.HashMap<>();
        newData.put("notes", notes);
        newData.put("status", order.getStatus());
        log.setNewData(newData);

        if (oldStatus != null) {
            java.util.Map<String, Object> oldData = new java.util.HashMap<>();
            oldData.put("status", oldStatus);
            log.setOldData(oldData);
        }

        log.setCreatedAt(Instant.now());
        orderLogRepository.save(log);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderCode(order.getOrderCode());
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
