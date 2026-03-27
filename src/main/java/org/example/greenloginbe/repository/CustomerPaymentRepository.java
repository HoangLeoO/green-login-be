package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.CustomerPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, Integer> {
    List<CustomerPayment> findByCustomerIdOrderByPaymentDateDesc(Integer customerId);
    Page<CustomerPayment> findByCustomerId(Integer customerId, Pageable pageable);
}
