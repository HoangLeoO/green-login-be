package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerService {
    List<Customer> getAllCustomers();
    Optional<Customer> getCustomerById(Integer id);
    Customer createCustomer(Customer customer);
    Customer updateCustomer(Integer id, Customer customerDetails);
    void deleteCustomer(Integer id);
}
