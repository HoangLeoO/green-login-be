package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Integer productId, Pageable pageable);
}
