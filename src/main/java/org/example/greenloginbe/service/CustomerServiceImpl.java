package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.Customer;
import org.example.greenloginbe.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        
        customer.setStatus("approved"); // Khách tạo trong hệ thống admin thì trạng thái mặc định là approved
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        if (customer.getTotalDebt() == null) {
            customer.setTotalDebt(BigDecimal.ZERO);
        }

        
        if (customer.getBranches() != null) {
            customer.getBranches().forEach(branch -> {
                branch.setCustomer(customer);
                branch.setCreatedAt(Instant.now());
            });
        }
        
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer createPublicCustomer(Customer customer) {
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            String code = "KH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            customer.setCustomerCode(code);
        }
        
        customer.setStatus("pending"); // Đăng ký public thì trạng thái là pending
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        if (customer.getTotalDebt() == null) {
            customer.setTotalDebt(java.math.BigDecimal.ZERO);
        }

        
        if (customer.getBranches() != null) {
            customer.getBranches().forEach(branch -> {
                branch.setCustomer(customer);
                branch.setCreatedAt(Instant.now());
            });
        }
        
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer updateCustomerStatus(Integer id, String status) {
        return customerRepository.findById(id).map(customer -> {
            customer.setStatus(status);
            customer.setUpdatedAt(Instant.now());
            return customerRepository.save(customer);
        }).orElseThrow(() -> new RuntimeException("Customer not found"));
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
                    customer.setTotalDebt(customerDetails.getTotalDebt());
                    customer.setUpdatedAt(Instant.now());

                    if (customerDetails.getBranches() != null) {

                        customer.getBranches().clear();
                        customerDetails.getBranches().forEach(branch -> {
                            branch.setCustomer(customer);
                            branch.setCreatedAt(Instant.now());
                            customer.getBranches().add(branch);
                        });
                    }

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
