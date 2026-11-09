package com.mandiritransport.ops.intelligence.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class IntelligenceDtos {

    private IntelligenceDtos() {
    }

    // 13.1 Driver Workload Balancing
    public record DriverWorkloadItemDto(
            String driverId,
            String driverCode,
            String driverName,
            String availabilityStatus,
            BigDecimal maxDailyHours,
            int tripCount,
            BigDecimal reservedHours,
            BigDecimal activeTripHours,
            BigDecimal utilizationPercent,
            String workloadStatus,
            String recommendation
    ) {
    }

    public record WorkloadReportDto(
            LocalDate targetDate,
            int totalDrivers,
            int availableDriversCount,
            List<DriverWorkloadItemDto> drivers
    ) {
    }

    // 13.2 Route Performance History (with minSampleSize = 5 guard)
    public record RoutePerformanceDto(
            String routeId,
            String routeCode,
            String routeName,
            int sampleSize,
            int minSampleSizeRequired,
            boolean sufficientData,
            BigDecimal avgDurationMin,
            Integer minDurationMin,
            Integer maxDurationMin,
            BigDecimal avgDistanceKm,
            BigDecimal avgFuelCost,
            BigDecimal avgTotalEffectiveCost,
            BigDecimal avgAgreedFare,
            BigDecimal avgActualProfit,
            BigDecimal profitMarginPercent,
            String explanation
    ) {
    }

    // 13.5 Respectful Customer Profitability Analysis
    public record CustomerProfitabilityDto(
            String customerId,
            String customerName,
            String companyName,
            int totalTrips,
            int completedTrips,
            int cancelledOrNoShowTrips,
            BigDecimal totalRevenue,
            BigDecimal totalEffectiveCost,
            BigDecimal totalActualProfit,
            BigDecimal avgProfitPerTrip,
            BigDecimal profitMarginPercent,
            BigDecimal outstandingUnpaidAmount,
            boolean hasSpecialPricingAgreement,
            String pricingInsight
    ) {
    }

    // 13.6 Operational Anomaly Detection
    public record AnomalyAlertDto(
            String ruleCode,
            String ruleName,
            String entityType,
            String entityId,
            String entityLabel,
            BigDecimal observedValue,
            BigDecimal baselineValue,
            BigDecimal thresholdValue,
            String whyFlagged,
            String reviewSuggestion
    ) {
    }

    public record AnomalyReportDto(
            int totalFlagged,
            List<AnomalyAlertDto> anomalies
    ) {
    }

    // 13.7 Vehicle Replacement Intelligence
    public record VehicleReplacementItemDto(
            String vehicleId,
            String registrationNumber,
            String nameModel,
            Integer manufactureYear,
            String status,
            BigDecimal currentOdometerKm,
            int completedTripsCount,
            BigDecimal totalRevenue,
            BigDecimal tripOperatingCost,
            BigDecimal nonTripVehicleExpenses,
            BigDecimal maintenanceCostTotal,
            int repairCount,
            BigDecimal downtimeHoursTotal,
            BigDecimal maintenanceCostPerKm,
            BigDecimal netFleetContribution,
            String replacementSignal,
            String recommendationSummary
    ) {
    }

    // Section 14 Central Cross-Filtered BI Explorer
    public record BiExplorerKpisDto(
            int totalTrips,
            int completedTrips,
            int cancelledTrips,
            BigDecimal grossFareRevenue,
            BigDecimal collectedPayments,
            BigDecimal outstandingReceivables,
            BigDecimal driverHeldCash,
            BigDecimal tripOperatingCost,
            BigDecimal vehicleMaintenanceExpenses,
            BigDecimal netOperatingProfit,
            BigDecimal fleetProfitAfterVehicleExpenses
    ) {
    }

    public record DimensionBreakdownDto(
            String id,
            String label,
            int completedTrips,
            BigDecimal revenue,
            BigDecimal cost,
            BigDecimal profit
    ) {
    }

    public record OperationalQaInsightDto(
            String question,
            String answer,
            String supportingDetail
    ) {
    }

    public record BiExplorerResponseDto(
            Map<String, String> filtersApplied,
            BiExplorerKpisDto kpis,
            List<DimensionBreakdownDto> byRoute,
            List<DimensionBreakdownDto> byVehicle,
            List<DimensionBreakdownDto> byDriver,
            List<DimensionBreakdownDto> byCustomer,
            List<OperationalQaInsightDto> directQaInsights
    ) {
    }
}
