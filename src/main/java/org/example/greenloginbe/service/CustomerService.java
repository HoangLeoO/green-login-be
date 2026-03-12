package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomerService {
    Page<Customer> getAllCustomers(Pageable pageable);
    Page<Customer> searchCustomers(String searchTerm, Pageable pageable);
    Optional<Customer> getCustomerById(Integer id);
    Customer createCustomer(Customer customer);
    Customer updateCustomer(Integer id, Customer customerDetails);
    void deleteCustomer(Integer id);
}
