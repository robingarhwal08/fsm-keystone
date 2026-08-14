package com.fsm.keystone.service;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.dto.CustomerOptionResponse;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;
    private final CascadeDeleteService cascadeDeleteService;

    public List<CustomerOptionResponse> listSignupOptions() {
        return customerRepository.findAll().stream()
                .map(c -> new CustomerOptionResponse(c.getId(), c.getName()))
                .toList();
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        AppUser actor = currentUser();
        if (actor != null && actor.getRole() == Role.CUSTOMER && actor.getCustomer() != null) {
            return List.of(actor.getCustomer());
        }
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        AppUser actor = currentUser();
        if (actor != null && actor.getRole() == Role.CUSTOMER
                && (actor.getCustomer() == null || !actor.getCustomer().getId().equals(id))) {
            throw new RuntimeException("Customer not found with id: " + id);
        }
        return customer;
    }

    public Customer updateCustomer(Long id, Customer input) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setName(input.getName());
        customer.setEmail(input.getEmail());
        customer.setPhone(input.getPhone());
        customer.setBillingAddress(input.getBillingAddress());

        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        cascadeDeleteService.deleteCustomer(id);
    }

    private AppUser currentUser() {
        try {
            return currentUserService.requireUser();
        } catch (Exception ex) {
            return null;
        }
    }
}
