package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Product;
import org.example.greenloginbe.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface StockMovementService {
    void addMovement(Product product, String type, BigDecimal quantity, String notes);
    Page<StockMovement> getMovementsByProduct(Integer productId, Pageable pageable);
}
