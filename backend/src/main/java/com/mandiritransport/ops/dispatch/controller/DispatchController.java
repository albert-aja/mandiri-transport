package com.mandiritransport.ops.dispatch.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.AssignTripRequest;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.ConflictSimulateRequest;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.ConflictSimulationResult;
import com.mandiritransport.ops.dispatch.service.DispatchService;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('OWNER')")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/dispatch/simulate")
    public ApiResponse<ConflictSimulationResult> simulate(@RequestBody ConflictSimulateRequest req) {
        return ApiResponse.ok(dispatchService.simulate(req));
    }

    @PostMapping("/trips/{tripId}/assign")
    public ApiResponse<TripDetailDto> assignTrip(@PathVariable String tripId,
                                                 @RequestBody AssignTripRequest req) {
        return ApiResponse.ok(dispatchService.assignTrip(tripId, req));
    }
}
