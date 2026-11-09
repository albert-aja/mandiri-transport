package com.mandiritransport.ops.finance.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CashCustodySummaryDto;
import com.mandiritransport.ops.finance.dto.FinanceDtos.ConfirmCashHandoverRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CostFormulaVersionDto;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CreateCashHandoverRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CreateCostFormulaRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.RecordPaymentRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.RecordTripExpenseRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.TripProfitabilityDto;
import com.mandiritransport.ops.finance.dto.FinanceDtos.VoidPaymentRequest;
import com.mandiritransport.ops.finance.entity.CashHandover;
import com.mandiritransport.ops.finance.entity.TripExpense;
import com.mandiritransport.ops.finance.entity.TripPayment;
import com.mandiritransport.ops.finance.service.CostEngineService;
import com.mandiritransport.ops.finance.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FinanceController {

    private final FinanceService financeService;
    private final CostEngineService costEngineService;

    public FinanceController(FinanceService financeService, CostEngineService costEngineService) {
        this.financeService = financeService;
        this.costEngineService = costEngineService;
    }

    @PostMapping("/trips/{id}/payments")
    public ApiResponse<TripPayment> recordPayment(
            @PathVariable String id,
            @Valid @RequestBody RecordPaymentRequest req,
            @RequestHeader(value = "X-Client-Mutation-Id", required = false) String headerMutationId) {
        return ApiResponse.ok(financeService.recordPayment(id, req, headerMutationId, false));
    }

    @PostMapping("/payments/{paymentId}/void")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<TripPayment> voidPayment(@PathVariable String paymentId,
                                                @Valid @RequestBody VoidPaymentRequest req) {
        return ApiResponse.ok(financeService.voidPayment(paymentId, req));
    }

    @GetMapping("/cash-custody/summary")
    public ApiResponse<CashCustodySummaryDto> getCashCustodySummary() {
        return ApiResponse.ok(financeService.getCashCustodySummary());
    }

    @PostMapping("/cash-handovers")
    public ApiResponse<CashHandover> createCashHandover(@Valid @RequestBody CreateCashHandoverRequest req) {
        return ApiResponse.ok(financeService.createCashHandover(req));
    }

    @PostMapping("/cash-handovers/{handoverId}/confirm")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CashHandover> confirmCashHandover(@PathVariable String handoverId,
                                                         @Valid @RequestBody ConfirmCashHandoverRequest req) {
        return ApiResponse.ok(financeService.confirmCashHandover(handoverId, req));
    }

    @PostMapping("/trips/{id}/expenses")
    public ApiResponse<TripExpense> recordTripExpense(
            @PathVariable String id,
            @Valid @RequestBody RecordTripExpenseRequest req,
            @RequestHeader(value = "X-Client-Mutation-Id", required = false) String headerMutationId) {
        return ApiResponse.ok(financeService.recordTripExpense(id, req, headerMutationId, false));
    }

    @GetMapping("/cost-formulas")
    public ApiResponse<List<CostFormulaVersionDto>> listCostFormulas() {
        return ApiResponse.ok(costEngineService.listFormulaVersions());
    }

    @PostMapping("/cost-formulas")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CostFormulaVersionDto> createCostFormula(@Valid @RequestBody CreateCostFormulaRequest req) {
        return ApiResponse.ok(costEngineService.createFormulaVersion(req));
    }

    @GetMapping("/trips/{id}/profitability")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<TripProfitabilityDto> getTripProfitability(@PathVariable String id) {
        return ApiResponse.ok(costEngineService.getTripProfitability(id));
    }
}
