package org.example.greenloginbe.controller;

import org.example.greenloginbe.entity.Customer;
import org.example.greenloginbe.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public")
public class PublicAPIController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/register-customer")
    public ResponseEntity<Customer> registerCustomer(@RequestBody Customer customer) {
        Customer createdCustomer = customerService.createPublicCustomer(customer);
        return ResponseEntity.ok(createdCustomer);
    }
}
