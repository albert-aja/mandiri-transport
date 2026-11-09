package com.mandiritransport.ops.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PricingDtos {

    private PricingDtos() {
    }

    public record PricingResolveRequest(
            String customerId,
            String routeId,
            String vehicleType,
            String tripType,
            BigDecimal distanceKm,
            Integer charterHours,
            LocalDate serviceDate,
            BigDecimal customFare
    ) {
    }

    public record PricingResolveResponse(
            BigDecimal suggestedFare,
            String pricingTierUsed,
            String explanation,
            String matchedAgreementName
    ) {
    }

    public record CustomerPricingAgreementRequest(
            @NotBlank(message = "customerId wajib diisi") String customerId,
            @NotBlank(message = "agreementName wajib diisi") String agreementName,
            String routeId,
            String vehicleType,
            @NotBlank(message = "pricingMode wajib diisi") String pricingMode,
            BigDecimal agreedFare,
            BigDecimal perKmRate,
            BigDecimal hourlyRate,
            BigDecimal fullDayRate,
            BigDecimal discountPercent,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String conditionsNote,
            Boolean isActive
    ) {
    }

    public record RoutePricingRuleRequest(
            @NotBlank(message = "routeId wajib diisi") String routeId,
            String vehicleType,
            String tripType,
            @NotNull(message = "suggestedFare wajib diisi") BigDecimal suggestedFare,
            String notes,
            Boolean isActive
    ) {
    }
}
