package com.mandiritransport.ops.booking.dto;

import com.mandiritransport.ops.booking.entity.Booking;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record BookingTemplateRequest(
            @NotBlank(message = "Nama template wajib diisi") String name,
            String customerId,
            String routeId,
            @NotBlank(message = "tripType wajib diisi") String tripType,
            String bookingSource,
            @NotBlank(message = "pickupAddress wajib diisi") String pickupAddress,
            @NotBlank(message = "destinationAddress wajib diisi") String destinationAddress,
            Integer defaultPassengerCount,
            Integer defaultLuggageCount,
            String luggageNotes,
            String preferredVehicleType,
            String notes
    ) {
    }

    public record TripStopInput(
            @NotBlank(message = "stopAddress wajib diisi") String stopAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            OffsetDateTime plannedArrivalAt,
            String notes
    ) {
    }

    public record QuickBookingRequest(
            String customerId,
            String newCustomerName,
            String newCustomerPhone,
            String bookingSource,
            @NotBlank(message = "tripType wajib diisi") String tripType,
            String routeId,
            @NotNull(message = "requestedPickupAt wajib diisi") OffsetDateTime requestedPickupAt,
            OffsetDateTime returnPickupAt,
            BigDecimal returnAgreedFare,
            Integer charterHours,
            @NotBlank(message = "pickupAddress wajib diisi") String pickupAddress,
            BigDecimal pickupLat,
            BigDecimal pickupLng,
            @NotBlank(message = "destinationAddress wajib diisi") String destinationAddress,
            BigDecimal destinationLat,
            BigDecimal destinationLng,
            Integer passengerCount,
            Integer luggageCount,
            String luggageNotes,
            String flightNumber,
            OffsetDateTime flightScheduledAt,
            String vehicleTypePreference,
            BigDecimal agreedFare,
            String overrideReason,
            String specialInstructions,
            @Valid List<TripStopInput> stops
    ) {
    }

    public record BookingDetailDto(
            Booking booking,
            List<TripDetailDto> trips
    ) {
    }
}
