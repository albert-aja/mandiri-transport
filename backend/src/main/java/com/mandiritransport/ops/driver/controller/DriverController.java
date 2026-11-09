package com.mandiritransport.ops.driver.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.driver.dto.DriverDtos.CreateDriverRequest;
import com.mandiritransport.ops.driver.dto.DriverDtos.DriverDto;
import com.mandiritransport.ops.driver.dto.DriverDtos.UnavailabilityWindowRequest;
import com.mandiritransport.ops.driver.dto.DriverDtos.UpdateAvailabilityRequest;
import com.mandiritransport.ops.driver.entity.DriverUnavailabilityWindow;
import com.mandiritransport.ops.driver.service.DriverService;
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
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public ApiResponse<List<DriverDto>> listDrivers() {
        return ApiResponse.ok(driverService.listDrivers());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<DriverDto> createDriver(@Valid @RequestBody CreateDriverRequest req) {
        return ApiResponse.ok(driverService.createDriver(req));
    }

    @PutMapping("/{id}/availability")
    public ApiResponse<DriverDto> updateAvailability(@PathVariable String id,
                                                     @Valid @RequestBody UpdateAvailabilityRequest req) {
        return ApiResponse.ok(driverService.updateAvailability(id, req));
    }

    @PostMapping("/{id}/unavailability-windows")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<DriverUnavailabilityWindow> addUnavailabilityWindow(@PathVariable String id,
                                                                           @Valid @RequestBody UnavailabilityWindowRequest req) {
        return ApiResponse.ok(driverService.addUnavailabilityWindow(id, req));
    }

    @GetMapping("/{id}/unavailability-windows")
    public ApiResponse<List<DriverUnavailabilityWindow>> listUnavailabilityWindows(@PathVariable String id) {
        return ApiResponse.ok(driverService.listUnavailabilityWindows(id));
    }
}
