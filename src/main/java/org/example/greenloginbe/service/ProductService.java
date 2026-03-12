package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductService {
    Page<Product> getAllProducts(Pageable pageable);
    Page<Product> searchProducts(String searchTerm, Integer categoryId, Pageable pageable);
    Optional<Product> getProductById(Integer id);
    Product createProduct(Product product);
    Product updateProduct(Integer id, Product productDetails);
    void deleteProduct(Integer id);
}
