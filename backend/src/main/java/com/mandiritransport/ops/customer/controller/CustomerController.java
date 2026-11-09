package com.mandiritransport.ops.customer.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerAddressRequest;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerDetailDto;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerNoteRequest;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerRequest;
import com.mandiritransport.ops.customer.entity.CustomerAddress;
import com.mandiritransport.ops.customer.entity.CustomerNote;
import com.mandiritransport.ops.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<List<CustomerDetailDto>> listCustomers(@RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(customerService.listCustomers(q));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerDetailDto> getCustomer(@PathVariable String id) {
        return ApiResponse.ok(customerService.getCustomer(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CustomerDetailDto> createCustomer(@Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok(customerService.createCustomer(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CustomerDetailDto> updateCustomer(@PathVariable String id,
                                                         @Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok(customerService.updateCustomer(id, req));
    }

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CustomerAddress> addAddress(@PathVariable String id,
                                                   @Valid @RequestBody CustomerAddressRequest req) {
        return ApiResponse.ok(customerService.addAddress(id, req));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CustomerNote> addNote(@PathVariable String id,
                                             @Valid @RequestBody CustomerNoteRequest req) {
        return ApiResponse.ok(customerService.addNote(id, req));
    }
}
