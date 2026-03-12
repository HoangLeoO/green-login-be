package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Product;
import org.example.greenloginbe.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        // Có thể thêm logic nghiệp vụ ở đây (ví dụ: validate giá, check tên trùng, etc.)
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Integer id, Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(productDetails.getName());
                    product.setUnit(productDetails.getUnit());
                    product.setDefaultPrice(productDetails.getDefaultPrice());
                    product.setIsActive(productDetails.getIsActive());
                    product.setCategory(productDetails.getCategory());
                    // Có thể bổ sung cập nhật các trường khác như stock_quantity, min_stock_level
                    product.setStockQuantity(productDetails.getStockQuantity());
                    product.setMinStockLevel(productDetails.getMinStockLevel());
                    return productRepository.save(product);
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
