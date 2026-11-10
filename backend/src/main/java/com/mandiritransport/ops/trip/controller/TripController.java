package com.mandiritransport.ops.trip.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.trip.dto.TripDtos.EmergencyAlertRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.EmergencyAlertResponse;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsBatchRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsBatchResponse;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsHistoryResponse;
import com.mandiritransport.ops.trip.dto.TripDtos.InspectionRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.LostAndFoundRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.TodayFlightScheduleDto;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import com.mandiritransport.ops.trip.dto.TripDtos.UpdateLostAndFoundStatusRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.UpdateTripScheduleRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.UpdateTripStatusRequest;
import com.mandiritransport.ops.trip.entity.LostAndFoundItem;
import com.mandiritransport.ops.trip.entity.VehicleInspection;
import com.mandiritransport.ops.trip.service.TripService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/trips")
    public ApiResponse<List<TripDetailDto>> listTrips(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "driverId", required = false) String driverId,
            @RequestParam(value = "vehicleId", required = false) String vehicleId,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.ok(tripService.listTrips(status, driverId, vehicleId, dateFrom, dateTo));
    }

    @GetMapping("/trips/today-flights")
    public ApiResponse<List<TodayFlightScheduleDto>> listTodayFlightSchedules(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(tripService.listTodayFlightSchedules(date));
    }

    @GetMapping("/trips/{id}")
    public ApiResponse<TripDetailDto> getTrip(@PathVariable String id) {
        return ApiResponse.ok(tripService.getTrip(id));
    }

    @PutMapping("/trips/{id}/schedule")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<TripDetailDto> updateSchedule(@PathVariable String id,
                                                     @Valid @RequestBody UpdateTripScheduleRequest req) {
        return ApiResponse.ok(tripService.updateTripSchedule(id, req));
    }

    @PostMapping("/trips/{id}/status")
    public ApiResponse<TripDetailDto> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateTripStatusRequest req,
            @RequestHeader(value = "X-Client-Mutation-Id", required = false) String headerMutationId) {
        return ApiResponse.ok(tripService.updateStatus(id, req, headerMutationId));
    }

    @PostMapping("/trips/{id}/tracking/start")
    public ApiResponse<TripDetailDto> startTracking(@PathVariable String id) {
        return ApiResponse.ok(tripService.startTracking(id));
    }

    @PostMapping("/trips/{id}/tracking/end")
    public ApiResponse<TripDetailDto> endTracking(@PathVariable String id) {
        return ApiResponse.ok(tripService.endTracking(id));
    }

    @PostMapping("/trips/{id}/gps-points/batch")
    public ApiResponse<GpsBatchResponse> uploadGpsBatch(@PathVariable String id,
                                                        @Valid @RequestBody GpsBatchRequest req) {
        return ApiResponse.ok(tripService.uploadGpsBatch(id, req, false));
    }

    @GetMapping("/trips/{id}/gps-points")
    public ApiResponse<GpsHistoryResponse> getGpsPoints(@PathVariable String id) {
        return ApiResponse.ok(tripService.getGpsPoints(id));
    }

    @PostMapping("/trips/{id}/inspections")
    public ApiResponse<VehicleInspection> recordInspection(@PathVariable String id,
                                                           @Valid @RequestBody InspectionRequest req) {
        return ApiResponse.ok(tripService.recordInspection(id, req, false));
    }

    @PostMapping("/trips/{id}/lost-and-found")
    public ApiResponse<LostAndFoundItem> createLostAndFound(@PathVariable String id,
                                                            @Valid @RequestBody LostAndFoundRequest req) {
        return ApiResponse.ok(tripService.createLostAndFound(id, req, false));
    }

    @PutMapping("/lost-and-found/{itemId}/status")
    public ApiResponse<LostAndFoundItem> updateLostAndFoundStatus(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateLostAndFoundStatusRequest req) {
        return ApiResponse.ok(tripService.updateLostAndFoundStatus(itemId, req));
    }

    @GetMapping("/lost-and-found")
    public ApiResponse<List<LostAndFoundItem>> listLostAndFound() {
        return ApiResponse.ok(tripService.listLostAndFound());
    }

    @PostMapping("/emergency-alerts")
    public ApiResponse<EmergencyAlertResponse> createEmergencyAlert(@RequestBody EmergencyAlertRequest req) {
        return ApiResponse.ok(tripService.createEmergencyAlert(req));
    }
}
