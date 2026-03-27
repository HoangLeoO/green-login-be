package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.OrderItemRequest;
import org.example.greenloginbe.dto.OrderItemResponse;
import org.example.greenloginbe.dto.OrderRequest;
import org.example.greenloginbe.dto.OrderResponse;
import org.example.greenloginbe.entity.*;
import org.example.greenloginbe.enums.OrderStatus;
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
import java.util.*;
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
    private CustomerBranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderLogRepository orderLogRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private CustomerFavoriteService customerFavoriteService;

    @Autowired
    private EmailService emailService;

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

        if (!"approved".equals(customer.getStatus())) {
            throw new RuntimeException("Khách hàng này chưa được phê duyệt hoặc đang bị khóa. Không thể tạo đơn hàng.");
        }

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

        if(request.getBranchId() != null) {
            CustomerBranch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found: " + request.getBranchId()));
            order.setBranch(branch);
        }

        order.setUser(user);
        order.setStatus(OrderStatus.PENDING.getValue());
        order.setNotes(request.getNotes());
        order.setOrderDate(request.getOrderDate() != null ? LocalDate.parse(request.getOrderDate()) : LocalDate.now());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
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

        // Cập nhật công nợ cho khách hàng
        Customer currentCustomer = savedOrder.getCustomer();
        if (currentCustomer.getTotalDebt() == null) currentCustomer.setTotalDebt(BigDecimal.ZERO);
        currentCustomer.setTotalDebt(currentCustomer.getTotalDebt().add(totalAmount));
        customerRepository.save(currentCustomer);

        // Ghi log tạo đơn
        saveOrderLog(savedOrder, user, "CREATE", null, "Tạo đơn hàng mới");


        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer id, OrderStatus status, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        
        String oldStatus = order.getStatus();
        String newStatusValue = status.getValue();
        if (oldStatus.equalsIgnoreCase(newStatusValue)) return mapToOrderResponse(order);

        // Nếu chuyển sang CANCELLED, hoàn lại tồn kho
        if (OrderStatus.CANCELLED == status && !OrderStatus.CANCELLED.getValue().equalsIgnoreCase(oldStatus)) {
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

        // Nếu chuyển sang PAID, cập nhật sản phẩm yêu thích của khách hàng thông qua CustomerFavoriteService
        if (OrderStatus.PAID == status && !OrderStatus.PAID.getValue().equalsIgnoreCase(oldStatus)) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                customerFavoriteService.updateFavorite(order.getCustomer(), item.getProduct(), item.getQuantity());
            }
        }

        order.setStatus(newStatusValue);
        order.setUpdatedAt(Instant.now());
        Order updatedOrder = orderRepository.save(order);

        // Cập nhật công nợ khách hàng dựa trên trạng thái mới
        Customer customer = updatedOrder.getCustomer();
        if (customer.getTotalDebt() == null) customer.setTotalDebt(BigDecimal.ZERO);

        // 1. Chuyển sang PAID: Trừ nợ (nếu trước đó chưa paid)
        if (OrderStatus.PAID == status && !oldStatus.equalsIgnoreCase(OrderStatus.PAID.getValue())) {
            customer.setTotalDebt(customer.getTotalDebt().subtract(updatedOrder.getTotalAmount()));
            customerRepository.save(customer);
        }
        // 2. Chuyển sang CANCELLED: Trừ nợ (nếu trước đó chưa paid)
        else if (OrderStatus.CANCELLED == status && !oldStatus.equalsIgnoreCase(OrderStatus.CANCELLED.getValue())) {
            if (!oldStatus.equalsIgnoreCase(OrderStatus.PAID.getValue())) {
                customer.setTotalDebt(customer.getTotalDebt().subtract(updatedOrder.getTotalAmount()));
                customerRepository.save(customer);
            }
        }
        // 3. Chuyển từ PAID về PENDING/DEBT (nếu có): Cộng nợ lại
        else if (oldStatus.equalsIgnoreCase(OrderStatus.PAID.getValue()) && 
                 (status == OrderStatus.PENDING || status == OrderStatus.DEBT || status == OrderStatus.DELIVERING)) {
            customer.setTotalDebt(customer.getTotalDebt().add(updatedOrder.getTotalAmount()));
            customerRepository.save(customer);
        }

        // Ghi log cập nhật trạng thái

        saveOrderLog(updatedOrder, user, "UPDATE_STATUS", oldStatus, "Cập nhật trạng thái từ " + oldStatus + " sang " + newStatusValue);

        return mapToOrderResponse(updatedOrder);
    }

    private void saveOrderLog(Order order, User user, String action, String oldStatus, String notes) {
        OrderLog log = new OrderLog();
        log.setOrder(order);
        log.setUser(user);
        log.setActionType(action);

        // Ghi thông tin vào newData (JSON)
        Map<String, Object> newData = new HashMap<>();
        newData.put("notes", notes);
        newData.put("status", order.getStatus());
        log.setNewData(newData);

        if (oldStatus != null) {
            Map<String, Object> oldData = new HashMap<>();
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
        response.setCustomerEmail(order.getCustomer().getEmail());
        
        if (order.getBranch() != null) {
            response.setBranchId(order.getBranch().getId());
            response.setBranchName(order.getBranch().getBranchName());
        }
        
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

    @Override
    @Transactional
    public void sendInvoice(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Chuyển thành DTO ngay trong Transaction để load hết dữ liệu
        OrderResponse response = mapToOrderResponse(order);
        emailService.sendInvoiceEmail(response);
    }

    @Override
    @Transactional
    public OrderResponse copyOrder(Integer orderId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Order sourceOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng gốc ID: " + orderId));

        List<OrderItem> sourceItems = orderItemRepository.findByOrderId(orderId);
        if (sourceItems.isEmpty()) {
            throw new RuntimeException("Đơn hàng gốc không có sản phẩm nào để sao chép.");
        }

        // 1. Kiểm tra tồn kho cho các sản phẩm trong đơn cũ
        for (OrderItem item : sourceItems) {
            Product product = item.getProduct();
            BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
            if (currentStock.compareTo(item.getQuantity()) < 0) {
                throw new RuntimeException("Sản phẩm \"" + product.getName() + "\" không đủ tồn kho để sao chép đơn này.");
            }
        }

        // 2. Tạo mã đơn hàng mới
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String orderCode = "HD" + datePart + "-" + randPart + "-COPY";

        // 3. Tạo đơn hàng mới
        Order newOrder = new Order();
        newOrder.setOrderCode(orderCode);
        newOrder.setCustomer(sourceOrder.getCustomer());
        newOrder.setBranch(sourceOrder.getBranch());
        newOrder.setUser(user);
        newOrder.setStatus(OrderStatus.PENDING.getValue());
        newOrder.setNotes("[Sao chép từ " + sourceOrder.getOrderCode() + "] " + (sourceOrder.getNotes() != null ? sourceOrder.getNotes() : ""));
        newOrder.setOrderDate(LocalDate.now());
        newOrder.setTotalAmount(sourceOrder.getTotalAmount());
        newOrder.setPaidAmount(BigDecimal.ZERO);
        newOrder.setCreatedAt(Instant.now());
        newOrder.setUpdatedAt(Instant.now());

        Order savedOrder = orderRepository.save(newOrder);

        // 4. Copy các mặt hàng và trừ kho
        for (OrderItem sourceItem : sourceItems) {
            OrderItem newItem = new OrderItem();
            newItem.setOrder(savedOrder);
            newItem.setProduct(sourceItem.getProduct());
            newItem.setQuantity(sourceItem.getQuantity());
            newItem.setUnitPrice(sourceItem.getUnitPrice());
            newItem.setTotalPrice(sourceItem.getTotalPrice());
            orderItemRepository.save(newItem);

            // Trừ tồn kho
            Product p = sourceItem.getProduct();
            p.setStockQuantity(p.getStockQuantity().subtract(sourceItem.getQuantity()));
            productRepository.save(p);

            // Ghi biến động kho
            StockMovement movement = new StockMovement();
            movement.setProduct(p);
            movement.setMovementType("OUT");
            movement.setQuantity(sourceItem.getQuantity());
            movement.setNotes("Sao chép đơn - Đơn mới " + savedOrder.getOrderCode());
            movement.setCreatedAt(Instant.now());
            stockMovementRepository.save(movement);
        }

        // 5. Cộng nợ khách hàng
        Customer customer = savedOrder.getCustomer();
        if (customer.getTotalDebt() == null) customer.setTotalDebt(BigDecimal.ZERO);
        customer.setTotalDebt(customer.getTotalDebt().add(savedOrder.getTotalAmount()));
        customerRepository.save(customer);

        // 6. Ghi log
        saveOrderLog(savedOrder, user, "COPY", null, "Sao chép từ đơn hàng: " + sourceOrder.getOrderCode());

        return mapToOrderResponse(savedOrder);
    }
}
