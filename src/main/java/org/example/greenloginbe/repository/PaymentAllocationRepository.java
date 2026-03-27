package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Integer> {
    List<PaymentAllocation> findByPaymentId(Integer paymentId);
    List<PaymentAllocation> findByOrderId(Integer orderId);
}
