package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Customer;
import org.example.greenloginbe.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Override
    public Page<Customer> searchCustomers(String searchTerm, Pageable pageable) {
        return customerRepository.searchCustomers(searchTerm, pageable);
    }

    @Override
    public Optional<Customer> getCustomerById(Integer id) {
        return customerRepository.findById(id);
    }

    @Override
    @Transactional
    public Customer createCustomer(Customer customer) {
        // Tự sinh mã khách hàng nếu chưa có: KH-{6 ký tự random}
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            String code = "KH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            customer.setCustomerCode(code);
        }
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer updateCustomer(Integer id, Customer customerDetails) {
        return customerRepository.findById(id)
                .map(customer -> {
                    customer.setName(customerDetails.getName());
                    customer.setPhone(customerDetails.getPhone());
                    customer.setEmail(customerDetails.getEmail());
                    customer.setAddress(customerDetails.getAddress());
                    customer.setNotes(customerDetails.getNotes());
                    customer.setUpdatedAt(Instant.now());
                    return customerRepository.save(customer);
                })
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customerRepository.delete(customer);
    }
}
