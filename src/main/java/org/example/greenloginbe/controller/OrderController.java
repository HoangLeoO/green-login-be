package org.example.greenloginbe.controller;

import org.example.greenloginbe.dto.OrderRequest;
import org.example.greenloginbe.dto.OrderResponse;
import org.example.greenloginbe.dto.OrderLogResponse;
import org.example.greenloginbe.entity.OrderLog;
import org.example.greenloginbe.repository.OrderLogRepository;
import org.example.greenloginbe.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderLogRepository orderLogRepository;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                sort[0].equals("id") ? "id" : sort[0]
        ).descending());

        if (sort.length > 1 && sort[1].equalsIgnoreCase("asc")) {
            pageable = PageRequest.of(page, size, Sort.by(sort[0]).ascending());
        }

        return ResponseEntity.ok(orderService.getAllOrders(status, customerId, startDate, endDate, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Integer id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest orderRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        OrderResponse response = orderService.createOrder(orderRequest, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Integer id, @RequestParam String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        try {
            OrderResponse response = orderService.updateOrderStatus(id, status, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/{id}/logs")
    @Transactional
    public ResponseEntity<java.util.List<OrderLogResponse>> getOrderLogs(@PathVariable Integer id) {
        java.util.List<OrderLog> logs = orderLogRepository.findByOrderIdOrderByCreatedAtDesc(id);
        java.util.List<OrderLogResponse> response = logs.stream().map(log -> {
            OrderLogResponse dto = new OrderLogResponse();
            dto.setId(log.getId());
            dto.setOrderId(log.getOrder().getId());
            dto.setActionType(log.getActionType());
            dto.setOldData(log.getOldData());
            dto.setNewData(log.getNewData());
            dto.setCreatedAt(log.getCreatedAt());
            
            if (log.getUser() != null) {
                dto.setUserId(log.getUser().getId());
                dto.setUsername(log.getUser().getUsername());
                dto.setUserFullName(log.getUser().getDisplayName());
            }
            return dto;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
