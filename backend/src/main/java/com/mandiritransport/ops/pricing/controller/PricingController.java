package com.mandiritransport.ops.pricing.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.pricing.dto.PricingDtos.CustomerPricingAgreementRequest;
import com.mandiritransport.ops.pricing.dto.PricingDtos.PricingResolveRequest;
import com.mandiritransport.ops.pricing.dto.PricingDtos.PricingResolveResponse;
import com.mandiritransport.ops.pricing.dto.PricingDtos.RoutePricingRuleRequest;
import com.mandiritransport.ops.pricing.entity.CustomerPricingAgreement;
import com.mandiritransport.ops.pricing.entity.RoutePricingRule;
import com.mandiritransport.ops.pricing.entity.StandardPricingConfig;
import com.mandiritransport.ops.pricing.service.PricingEngineService;
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
@RequestMapping("/api/v1/pricing")
@PreAuthorize("hasRole('OWNER')")
public class PricingController {

    private final PricingEngineService pricingEngineService;

    public PricingController(PricingEngineService pricingEngineService) {
        this.pricingEngineService = pricingEngineService;
    }

    @PostMapping("/resolve")
    public ApiResponse<PricingResolveResponse> resolve(@RequestBody PricingResolveRequest req) {
        return ApiResponse.ok(pricingEngineService.resolvePrice(req));
    }

    @GetMapping("/customer-agreements")
    public ApiResponse<List<CustomerPricingAgreement>> listCustomerAgreements(
            @RequestParam(value = "customerId", required = false) String customerId) {
        return ApiResponse.ok(pricingEngineService.listCustomerAgreements(customerId));
    }

    @PostMapping("/customer-agreements")
    public ApiResponse<CustomerPricingAgreement> createCustomerAgreement(
            @Valid @RequestBody CustomerPricingAgreementRequest req) {
        return ApiResponse.ok(pricingEngineService.createCustomerAgreement(req));
    }

    @PutMapping("/customer-agreements/{id}")
    public ApiResponse<CustomerPricingAgreement> updateCustomerAgreement(
            @PathVariable String id,
            @Valid @RequestBody CustomerPricingAgreementRequest req) {
        return ApiResponse.ok(pricingEngineService.updateCustomerAgreement(id, req));
    }

    @GetMapping("/route-rules")
    public ApiResponse<List<RoutePricingRule>> listRouteRules(
            @RequestParam(value = "routeId", required = false) String routeId) {
        return ApiResponse.ok(pricingEngineService.listRouteRules(routeId));
    }

    @PostMapping("/route-rules")
    public ApiResponse<RoutePricingRule> createRouteRule(@Valid @RequestBody RoutePricingRuleRequest req) {
        return ApiResponse.ok(pricingEngineService.createRouteRule(req));
    }

    @PutMapping("/route-rules/{id}")
    public ApiResponse<RoutePricingRule> updateRouteRule(@PathVariable String id,
                                                         @Valid @RequestBody RoutePricingRuleRequest req) {
        return ApiResponse.ok(pricingEngineService.updateRouteRule(id, req));
    }

    @GetMapping("/standard-configs")
    public ApiResponse<List<StandardPricingConfig>> listStandardConfigs() {
        return ApiResponse.ok(pricingEngineService.listStandardConfigs());
    }
}
