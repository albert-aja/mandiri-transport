package com.mandiritransport.ops.vehicle.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.RouteRequest;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final VehicleService vehicleService;

    public RouteController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ApiResponse<List<Route>> listRoutes() {
        return ApiResponse.ok(vehicleService.listRoutes());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<Route> createRoute(@Valid @RequestBody RouteRequest req) {
        return ApiResponse.ok(vehicleService.createRoute(req));
    }
}
