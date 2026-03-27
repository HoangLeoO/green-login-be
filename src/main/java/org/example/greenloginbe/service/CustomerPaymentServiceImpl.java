package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.PaymentRequest;
import org.example.greenloginbe.dto.PaymentResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerPaymentServiceImpl implements CustomerPaymentService {

    @Autowired
    private CustomerPaymentRepository paymentRepository;

    @Autowired
    private PaymentAllocationRepository allocationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderLogRepository orderLogRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, String username) {
        // username có thể là "system" khi callback đến từ VNPay server (không có JWT)
        User user = (username != null && !username.equals("system"))
                ? userRepository.findByUsername(username).orElse(null)
                : null;

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + request.getCustomerId()));

        // 1. Lưu bản ghi Phiếu thu (Payment)
        CustomerPayment payment = new CustomerPayment();
        payment.setCustomer(customer);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(request.getTransactionId());
        payment.setNotes(request.getNotes());
        payment.setUser(user); // null nếu là VNPay callback tự động
        payment.setPaymentDate(Instant.now());
        payment.setCreatedAt(Instant.now());
        
        CustomerPayment savedPayment = paymentRepository.save(payment);

        // 2. Thực hiện gạch nợ tự động theo thứ tự ưu tiên (FIFO)
        List<Order> unpaidOrders = orderRepository.findUnpaidOrders(customer.getId());
        BigDecimal remainingAmount = request.getAmount();
        List<PaymentResponse.AllocationDetail> allocationDetails = new ArrayList<>();

        for (Order order : unpaidOrders) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            String oldStatus = order.getStatus();
            BigDecimal oldPaidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
            
            BigDecimal currentPaid = oldPaidAmount;
            BigDecimal amountNeeded = order.getTotalAmount().subtract(currentPaid);
            BigDecimal allocated;

            if (remainingAmount.compareTo(amountNeeded) >= 0) {
                // Thanh toán toàn bộ đơn này
                allocated = amountNeeded;
                order.setPaidAmount(order.getTotalAmount());
                order.setStatus(OrderStatus.PAID.getValue());
                remainingAmount = remainingAmount.subtract(amountNeeded);
            } else {
                // Thanh toán một phần đơn này
                allocated = remainingAmount;
                order.setPaidAmount(currentPaid.add(remainingAmount));
                order.setStatus(OrderStatus.PARTIAL_PAID.getValue());
                remainingAmount = BigDecimal.ZERO;
            }

            orderRepository.save(order);

            // Lưu lịch sử gạch nợ (Allocation)
            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setPayment(savedPayment);
            allocation.setOrder(order);
            allocation.setAllocatedAmount(allocated);
            allocation.setCreatedAt(Instant.now());
            allocationRepository.save(allocation);

            // 🎯 Ghi Log lịch sử đơn hàng (OrderLog)
            OrderLog orderLog = new OrderLog();
            orderLog.setOrder(order);
            orderLog.setUser(user);
            orderLog.setActionType("PAYMENT_RECEIVED");
            
            // Lưu dữ liệu trạng thái cũ và mới để sau này xem lại
            orderLog.setOldData(Map.of("status", oldStatus, "paid_amount", oldPaidAmount));
            orderLog.setNewData(Map.of(
                "status", order.getStatus(), 
                "paid_amount", order.getPaidAmount(),
                "allocated_now", allocated,
                "payment_method", savedPayment.getPaymentMethod(),
                "notes", "Thanh toán cho đơn hàng từ phiếu thu #" + savedPayment.getId()
            ));
            orderLog.setCreatedAt(Instant.now());
            orderLogRepository.save(orderLog);

            allocationDetails.add(new PaymentResponse.AllocationDetail(order.getOrderCode(), allocated));
        }

        // 3. Cập nhật lại tổng nợ dồn của khách hàng trong bảng customers
        BigDecimal currentDebt = customer.getTotalDebt() != null ? customer.getTotalDebt() : BigDecimal.ZERO;
        customer.setTotalDebt(currentDebt.subtract(request.getAmount()));
        customerRepository.save(customer);

        return mapToResponse(savedPayment, allocationDetails);
    }

    @Override
    public List<PaymentResponse> getPaymentsByCustomerId(Integer customerId) {
        return paymentRepository.findByCustomerIdOrderByPaymentDateDesc(customerId)
                .stream().map(p -> mapToResponse(p, null)).collect(Collectors.toList());
    }

    @Override
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(p -> mapToResponse(p, null));
    }

    private PaymentResponse mapToResponse(CustomerPayment p, List<PaymentResponse.AllocationDetail> allocations) {
        PaymentResponse res = new PaymentResponse();
        res.setId(p.getId());
        res.setCustomerId(p.getCustomer().getId());
        res.setCustomerName(p.getCustomer().getName());
        res.setAmount(p.getAmount());
        res.setPaymentMethod(p.getPaymentMethod());
        res.setTransactionId(p.getTransactionId());
        res.setPaymentDate(p.getPaymentDate());
        res.setNotes(p.getNotes());
        res.setCreatedBy(p.getUser() != null ? p.getUser().getDisplayName() : "VNPay (Tự động)");
        res.setAllocations(allocations);
        return res;
    }
}
