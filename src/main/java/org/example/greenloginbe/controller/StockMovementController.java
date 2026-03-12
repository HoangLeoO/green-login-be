package org.example.greenloginbe.controller;

import org.example.greenloginbe.entity.StockMovement;
import org.example.greenloginbe.service.StockMovementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    @Autowired
    private StockMovementService stockMovementService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<StockMovement>> getMovementsByProduct(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(stockMovementService.getMovementsByProduct(productId, pageable));
    }
}
