package com.mandiritransport.ops.customer.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.customer.dto.CustomerDtos.RenderWhatsappRequest;
import com.mandiritransport.ops.customer.dto.CustomerDtos.RenderWhatsappResponse;
import com.mandiritransport.ops.customer.entity.WhatsappTemplate;
import com.mandiritransport.ops.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/whatsapp-templates")
public class WhatsappTemplateController {

    private final CustomerService customerService;

    public WhatsappTemplateController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<List<WhatsappTemplate>> listTemplates() {
        return ApiResponse.ok(customerService.listWhatsappTemplates());
    }

    @PostMapping("/render")
    public ApiResponse<RenderWhatsappResponse> render(@Valid @RequestBody RenderWhatsappRequest req) {
        return ApiResponse.ok(customerService.renderWhatsappTemplate(req));
    }
}
