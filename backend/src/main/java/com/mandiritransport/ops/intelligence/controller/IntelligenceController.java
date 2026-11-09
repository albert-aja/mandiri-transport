package com.mandiritransport.ops.intelligence.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.AnomalyReportDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.BiExplorerResponseDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.CustomerProfitabilityDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.RoutePerformanceDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.VehicleReplacementItemDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.WorkloadReportDto;
import com.mandiritransport.ops.intelligence.service.IntelligenceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bi")
@PreAuthorize("hasRole('OWNER')")
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    public IntelligenceController(IntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/workload")
    public ApiResponse<WorkloadReportDto> getWorkload(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(intelligenceService.getWorkloadReport(date));
    }

    @GetMapping("/routes")
    public ApiResponse<List<RoutePerformanceDto>> getRoutes(
            @RequestParam(value = "routeId", required = false) String routeId,
            @RequestParam(value = "vehicleType", required = false) String vehicleType) {
        return ApiResponse.ok(intelligenceService.getRoutePerformance(routeId, vehicleType));
    }

    @GetMapping("/customers")
    public ApiResponse<List<CustomerProfitabilityDto>> getCustomers(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.ok(intelligenceService.getCustomerProfitability(dateFrom, dateTo));
    }

    @GetMapping("/anomalies")
    public ApiResponse<AnomalyReportDto> getAnomalies() {
        return ApiResponse.ok(intelligenceService.getAnomalies());
    }

    @GetMapping("/vehicle-replacement")
    public ApiResponse<List<VehicleReplacementItemDto>> getVehicleReplacement() {
        return ApiResponse.ok(intelligenceService.getVehicleReplacementAnalysis());
    }

    @GetMapping("/explorer")
    public ApiResponse<BiExplorerResponseDto> getExplorer(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "driverId", required = false) String driverId,
            @RequestParam(value = "vehicleId", required = false) String vehicleId,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "routeId", required = false) String routeId) {
        return ApiResponse.ok(intelligenceService.getExplorer(dateFrom, dateTo, driverId, vehicleId, customerId, routeId));
    }
}
