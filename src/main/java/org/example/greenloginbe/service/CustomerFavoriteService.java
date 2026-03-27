package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Customer;
import org.example.greenloginbe.entity.Product;

import java.math.BigDecimal;

import org.example.greenloginbe.dto.CustomerFavoriteResponse;
import java.util.List;

public interface CustomerFavoriteService {
    void updateFavorite(Customer customer, Product product, BigDecimal quantity);
    List<CustomerFavoriteResponse> getCustomerFavorites(Integer customerId);
}
