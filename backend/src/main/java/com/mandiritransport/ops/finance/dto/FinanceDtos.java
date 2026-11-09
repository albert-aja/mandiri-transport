package com.mandiritransport.ops.finance.dto;

import com.mandiritransport.ops.finance.entity.CashHandover;
import com.mandiritransport.ops.finance.entity.CostFormulaComponent;
import com.mandiritransport.ops.finance.entity.TripCostSnapshotItem;
import com.mandiritransport.ops.finance.entity.TripPayment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class FinanceDtos {

    private FinanceDtos() {
    }

    public record RecordPaymentRequest(
            @NotNull(message = "Jumlah pembayaran wajib diisi") BigDecimal amount,
            @NotBlank(message = "Metode pembayaran wajib diisi") String paymentMethod,
            Boolean isRefund,
            String collectedByRole,
            OffsetDateTime paidAt,
            String referenceNumber,
            String receiptPath,
            String notes,
            String clientMutationId
    ) {
    }

    public record VoidPaymentRequest(
            @NotBlank(message = "Alasan pembatalan (void) wajib diisi") String reason
    ) {
    }

    public record CreateCashHandoverRequest(
            String driverId,
            @NotNull(message = "declaredAmount wajib diisi") BigDecimal declaredAmount,
            List<String> paymentIds,
            String driverNotes
    ) {
    }

    public record ConfirmCashHandoverRequest(
            @NotNull(message = "confirmedAmount wajib diisi") BigDecimal confirmedAmount,
            String ownerNotes,
            Boolean markDisputed
    ) {
    }

    public record DriverCashCustodyDto(
            String driverId,
            String driverName,
            BigDecimal unsettledCashAmount,
            List<TripPayment> unsettledPayments
    ) {
    }

    public record CashCustodySummaryDto(
            BigDecimal totalDriverHeldCash,
            long pendingHandoversCount,
            long disputedCount,
            List<DriverCashCustodyDto> byDriver,
            List<CashHandover> recentHandovers
    ) {
    }

    public record RecordTripExpenseRequest(
            @NotBlank(message = "Kategori pengeluaran wajib diisi") String category,
            @NotNull(message = "Jumlah pengeluaran wajib diisi") BigDecimal amount,
            BigDecimal fuelLiters,
            String receiptPath,
            String notes,
            OffsetDateTime incurredAt,
            String clientMutationId
    ) {
    }

    public record CostFormulaComponentRequest(
            @NotBlank(message = "componentCode wajib diisi") String componentCode,
            @NotBlank(message = "name wajib diisi") String name,
            @NotBlank(message = "calcType wajib diisi") String calcType,
            @NotBlank(message = "reconciliationPolicy wajib diisi") String reconciliationPolicy,
            String expenseCategoryLink,
            BigDecimal fixedAmount,
            BigDecimal rateValue,
            BigDecimal monthlyAmount,
            Integer assumedMonthlyTrips,
            String expressionFormula,
            String parametersJson,
            String vehicleTypeFilter,
            Integer sortOrder
    ) {
    }

    public record CreateCostFormulaRequest(
            Integer versionNumber,
            @NotBlank(message = "Nama formula wajib diisi") String name,
            @NotNull(message = "effectiveFrom wajib diisi") LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes,
            Boolean isActive,
            @NotEmpty(message = "Minimal 1 komponen biaya diperlukan") @Valid List<CostFormulaComponentRequest> components
    ) {
    }

    public record CostFormulaVersionDto(
            String id,
            Integer versionNumber,
            String name,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Boolean isActive,
            String notes,
            List<CostFormulaComponent> components
    ) {
    }

    public record TripProfitabilityDto(
            String tripId,
            String tripCode,
            boolean snapshotLocked,
            Integer formulaVersionNumber,
            String formulaVersionName,
            BigDecimal distanceKmUsed,
            String distanceSource,
            BigDecimal durationHoursUsed,
            BigDecimal agreedFareSnapshot,
            BigDecimal estimatedOperatingCost,
            BigDecimal actualExpensesTotal,
            BigDecimal allocatedOperatingCost,
            BigDecimal totalEffectiveCost,
            BigDecimal estimatedProfit,
            BigDecimal actualProfit,
            List<TripCostSnapshotItem> items
    ) {
    }
}
