package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.OrderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderLogRepository extends JpaRepository<OrderLog, Integer> {
    List<OrderLog> findByOrderIdOrderByCreatedAtDesc(Integer orderId);
}
