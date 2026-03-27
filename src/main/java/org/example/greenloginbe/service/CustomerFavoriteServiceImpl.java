package org.example.greenloginbe.service;

import org.example.greenloginbe.dto.CustomerFavoriteResponse;
import org.example.greenloginbe.entity.Customer;
import org.example.greenloginbe.entity.CustomerFavorite;
import org.example.greenloginbe.entity.Product;
import org.example.greenloginbe.repository.CustomerFavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerFavoriteServiceImpl implements CustomerFavoriteService {

    @Autowired
    private CustomerFavoriteRepository customerFavoriteRepository;

    @Override
    public void updateFavorite(Customer customer, Product product, BigDecimal quantity) {
        Optional<CustomerFavorite> customerFavoriteOptional = customerFavoriteRepository.findByCustomerAndProduct(customer, product);

        if (customerFavoriteOptional.isEmpty()) {
            CustomerFavorite customerFavorite = new CustomerFavorite();
            customerFavorite.setCustomer(customer);
            customerFavorite.setProduct(product);
            customerFavorite.setFrequencyScore(1);
            customerFavorite.setDefaultQuantity(quantity);
            customerFavorite.setLastOrderedAt(Instant.now());
            customerFavoriteRepository.save(customerFavorite);
        } else {
            CustomerFavorite favorite = customerFavoriteOptional.get();
            
            // Cập nhật điểm tần suất (Xử lý null để an toàn)
            int currentScore = (favorite.getFrequencyScore() != null) ? favorite.getFrequencyScore() : 0;
            favorite.setFrequencyScore(currentScore + 1);

            // Cập nhật số lượng trung bình mua hàng (Làm tròn 2 chữ số thập phân)
            BigDecimal currentAvg = favorite.getDefaultQuantity() != null ? favorite.getDefaultQuantity() : BigDecimal.ZERO;
            BigDecimal newAvg = currentAvg.add(quantity).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            
            favorite.setDefaultQuantity(newAvg);
            favorite.setLastOrderedAt(Instant.now());
            customerFavoriteRepository.save(favorite);
        }
    }

    @Override
    public List<CustomerFavoriteResponse> getCustomerFavorites(Integer customerId) {
        List<CustomerFavorite> favorites = customerFavoriteRepository.findByCustomerIdOrderByFrequencyScoreDesc(customerId);
        return favorites.stream().map(fav -> {
            org.example.greenloginbe.dto.CustomerFavoriteResponse res = new org.example.greenloginbe.dto.CustomerFavoriteResponse();
            res.setId(fav.getId());
            res.setProductId(fav.getProduct().getId());
            res.setProductName(fav.getProduct().getName());
            res.setProductUnit(fav.getProduct().getUnit());
            res.setDefaultQuantity(fav.getDefaultQuantity());
            res.setFrequencyScore(fav.getFrequencyScore());
            res.setLastOrderedAt(fav.getLastOrderedAt());
            
            // Lấy thông tin giá và tồn kho hiện tại của sản phẩm
            res.setCurrentPrice(fav.getProduct().getDefaultPrice());
            res.setStockQuantity(fav.getProduct().getStockQuantity());
            res.setStatus(Boolean.TRUE.equals(fav.getProduct().getIsActive()) ? "active" : "inactive");
            
            return res;
        }).collect(java.util.stream.Collectors.toList());
    }
}
