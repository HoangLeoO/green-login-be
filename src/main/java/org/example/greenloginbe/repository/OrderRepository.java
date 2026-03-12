package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:customerId IS NULL OR o.customer.id = :customerId) AND " +
           "(:startDate IS NULL OR o.orderDate >= :startDate) AND " +
           "(:endDate IS NULL OR o.orderDate <= :endDate) AND " +
           "(:search IS NULL OR LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> filterOrders(
            @Param("status") String status,
            @Param("customerId") Integer customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable);
}
