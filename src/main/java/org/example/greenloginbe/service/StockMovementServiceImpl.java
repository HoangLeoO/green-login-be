package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Product;
import org.example.greenloginbe.entity.StockMovement;
import org.example.greenloginbe.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class StockMovementServiceImpl implements StockMovementService {

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public void addMovement(Product product, String type, BigDecimal quantity, String notes) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setNotes(notes);
        movement.setCreatedAt(Instant.now());
        stockMovementRepository.save(movement);
    }

    @Override
    public Page<StockMovement> getMovementsByProduct(Integer productId, Pageable pageable) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    }
}
