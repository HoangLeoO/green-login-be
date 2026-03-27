package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.Customer;
import org.example.greenloginbe.entity.CustomerFavorite;
import org.example.greenloginbe.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerFavoriteRepository extends JpaRepository<CustomerFavorite,Integer> {
    Optional<CustomerFavorite> findByCustomerAndProduct(Customer customer, Product product);
    List<CustomerFavorite> findByCustomerIdOrderByFrequencyScoreDesc(Integer customerId);
}
