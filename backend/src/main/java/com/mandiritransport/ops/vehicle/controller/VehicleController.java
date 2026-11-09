package com.mandiritransport.ops.vehicle.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.MaintenanceLogRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.MaintenanceScheduleRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleDetailDto;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleDocumentRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleExpenseRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleRequest;
import com.mandiritransport.ops.vehicle.entity.VehicleDocument;
import com.mandiritransport.ops.vehicle.entity.VehicleExpense;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceLog;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceSchedule;
import com.mandiritransport.ops.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ApiResponse<List<VehicleDetailDto>> listVehicles() {
        return ApiResponse.ok(vehicleService.listVehicles());
    }

    @GetMapping("/{id}")
    public ApiResponse<VehicleDetailDto> getVehicle(@PathVariable String id) {
        return ApiResponse.ok(vehicleService.getVehicle(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<VehicleDetailDto> createVehicle(@Valid @RequestBody VehicleRequest req) {
        return ApiResponse.ok(vehicleService.createVehicle(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<VehicleDetailDto> updateVehicle(@PathVariable String id,
                                                       @Valid @RequestBody VehicleRequest req) {
        return ApiResponse.ok(vehicleService.updateVehicle(id, req));
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<VehicleDocument> addDocument(@PathVariable String id,
                                                    @Valid @RequestBody VehicleDocumentRequest req) {
        return ApiResponse.ok(vehicleService.addDocument(id, req));
    }

    @PostMapping("/{id}/maintenance-schedules")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<VehicleMaintenanceSchedule> addMaintenanceSchedule(@PathVariable String id,
                                                                          @Valid @RequestBody MaintenanceScheduleRequest req) {
        return ApiResponse.ok(vehicleService.addMaintenanceSchedule(id, req));
    }

    @PostMapping("/{id}/maintenance-logs")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<VehicleMaintenanceLog> addMaintenanceLog(@PathVariable String id,
                                                                @Valid @RequestBody MaintenanceLogRequest req) {
        return ApiResponse.ok(vehicleService.addMaintenanceLog(id, req));
    }

    @PostMapping("/{id}/expenses")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<VehicleExpense> addVehicleExpense(@PathVariable String id,
                                                         @Valid @RequestBody VehicleExpenseRequest req) {
        return ApiResponse.ok(vehicleService.addVehicleExpense(id, req));
    }

    @GetMapping("/{id}/expenses")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<List<VehicleExpense>> listVehicleExpenses(@PathVariable String id) {
        return ApiResponse.ok(vehicleService.listVehicleExpenses(id));
    }
}
