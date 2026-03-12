package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Product;
import org.example.greenloginbe.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementService stockMovementService;

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Page<Product> searchProducts(String searchTerm, Integer categoryId, Pageable pageable) {
        return productRepository.filterProducts(searchTerm, categoryId, pageable);
    }

    @Override
    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        // Tự sinh SKU nếu chưa có: SP-{6 ký tự random}
        if (product.getSku() == null || product.getSku().isBlank()) {
            String sku = "SP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            product.setSku(sku);
        }
        Product savedProduct = productRepository.save(product);
        
        // Ghi lại lịch sử nhập kho ban đầu nếu có số lượng > 0
        if (product.getStockQuantity() != null && product.getStockQuantity().compareTo(BigDecimal.ZERO) > 0) {
            stockMovementService.addMovement(savedProduct, "IN", product.getStockQuantity(), "Khởi tạo tồn kho ban đầu");
        }
        
        return savedProduct;
    }

    @Override
    @Transactional
    public Product updateProduct(Integer id, Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    BigDecimal oldStock = product.getStockQuantity();
                    BigDecimal newStock = productDetails.getStockQuantity();
                    
                    product.setName(productDetails.getName());
                    product.setUnit(productDetails.getUnit());
                    product.setDefaultPrice(productDetails.getDefaultPrice());
                    product.setIsActive(productDetails.getIsActive());
                    product.setCategory(productDetails.getCategory());
                    product.setStockQuantity(newStock);
                    product.setMinStockLevel(productDetails.getMinStockLevel());
                    
                    Product updated = productRepository.save(product);
                    
                    // Kiểm tra xem có sự thay đổi về tồn kho không để ghi log
                    if (newStock != null && (oldStock == null || oldStock.compareTo(newStock) != 0)) {
                        BigDecimal diff = newStock.subtract(oldStock == null ? BigDecimal.ZERO : oldStock);
                        String type = diff.compareTo(BigDecimal.ZERO) > 0 ? "IN" : "OUT";
                        stockMovementService.addMovement(updated, type, diff.abs(), "Điều chỉnh tồn kho thủ công");
                    }
                    
                    return updated;
                })
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        productRepository.delete(product);
    }
}
