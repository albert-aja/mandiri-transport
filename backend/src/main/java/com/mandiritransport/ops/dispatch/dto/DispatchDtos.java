package com.mandiritransport.ops.dispatch.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class DispatchDtos {

    private DispatchDtos() {
    }

    public record ConflictSimulateRequest(
            String tripId,
            String routeId,
            OffsetDateTime scheduledPickupAt,
            Integer estimatedDurationMin,
            Integer prepBufferMin,
            Integer arrivalBufferMin,
            Integer repositionBufferMin,
            String candidateDriverId,
            String candidateVehicleId,
            Integer passengerCount,
            Integer luggageCount,
            String tripType,
            String customerId
    ) {
    }

    public record AssignTripRequest(
            String driverId,
            String vehicleId,
            OffsetDateTime scheduledPickupAt,
            Integer prepBufferMin,
            Integer arrivalBufferMin,
            Integer repositionBufferMin,
            Boolean forceOverrideConfirmed,
            String overrideReason
    ) {
    }

    public record ConflictItemDto(
            String type,
            String conflictingTripCode,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            String explanation
    ) {
    }

    public record DriverRecommendationDto(
            String driverId,
            String driverName,
            String availabilityStatus,
            boolean conflictFree,
            BigDecimal reservedHoursTargetDay,
            BigDecimal activeHoursTargetDay,
            boolean recommended,
            String explanation
    ) {
    }

    public record VehicleRecommendationDto(
            String vehicleId,
            String registrationNumber,
            String nameModel,
            String vehicleType,
            Integer passengerCapacity,
            Integer luggageCapacityBags,
            boolean conflictFree,
            boolean suitable,
            int suitabilityScore,
            String explanation
    ) {
    }

    public record ConflictSimulationResult(
            OffsetDateTime reservedStartAt,
            OffsetDateTime reservedEndAt,
            boolean hasDriverConflict,
            List<ConflictItemDto> driverConflicts,
            boolean hasVehicleConflict,
            List<ConflictItemDto> vehicleConflicts,
            List<DriverRecommendationDto> driverRecommendations,
            List<VehicleRecommendationDto> vehicleRecommendations
    ) {
    }
}
