package com.mandiritransport.ops.trip.dto;

import com.mandiritransport.ops.customer.entity.CustomerNote;
import com.mandiritransport.ops.finance.entity.TripExpense;
import com.mandiritransport.ops.finance.entity.TripPayment;
import com.mandiritransport.ops.trip.entity.LostAndFoundItem;
import com.mandiritransport.ops.trip.entity.TripGpsPoint;
import com.mandiritransport.ops.trip.entity.TripStop;
import com.mandiritransport.ops.trip.entity.VehicleInspection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class TripDtos {

    private TripDtos() {
    }

    public record UpdateTripStatusRequest(
            @NotBlank(message = "newStatus wajib diisi") String newStatus,
            String reasonNotes,
            String completionNotes,
            BigDecimal odometerStartKm,
            BigDecimal odometerEndKm,
            String clientMutationId,
            Long expectedVersion
    ) {
    }

    public record UpdateTripScheduleRequest(
            @NotNull(message = "scheduledPickupAt wajib diisi") OffsetDateTime scheduledPickupAt,
            Integer estimatedDurationMin,
            Integer prepBufferMin,
            Integer arrivalBufferMin,
            Integer repositionBufferMin
    ) {
    }

    public record GpsPointInput(
            @NotBlank(message = "clientPointId wajib diisi") String clientPointId,
            @NotNull(message = "latitude wajib diisi") BigDecimal latitude,
            @NotNull(message = "longitude wajib diisi") BigDecimal longitude,
            BigDecimal speedKmh,
            BigDecimal accuracyM,
            @NotNull(message = "recordedAt wajib diisi") OffsetDateTime recordedAt
    ) {
    }

    public record GpsBatchRequest(
            @NotEmpty(message = "Daftar titik GPS tidak boleh kosong") @Valid List<GpsPointInput> points
    ) {
    }

    public record GpsBatchResponse(
            String tripId,
            int acceptedCount,
            int duplicateCount,
            int rejectedCount,
            OffsetDateTime lastGpsRecordedAt,
            BigDecimal gpsDistanceKm
    ) {
    }

    public record GpsHistoryResponse(
            String tripId,
            Boolean trackingActive,
            OffsetDateTime trackingStartedAt,
            OffsetDateTime trackingEndedAt,
            OffsetDateTime lastGpsRecordedAt,
            BigDecimal gpsDistanceKm,
            List<TripGpsPoint> points
    ) {
    }

    public record InspectionRequest(
            String stage,
            Boolean allPassed,
            List<String> flaggedItems,
            List<String> photoPaths,
            String notes,
            String clientMutationId
    ) {
    }

    public record LostAndFoundRequest(
            @NotBlank(message = "itemDescription wajib diisi") String itemDescription,
            LocalDate foundDate,
            String photoPath,
            String status,
            String notes,
            String clientMutationId
    ) {
    }

    public record UpdateLostAndFoundStatusRequest(
            @NotBlank(message = "status wajib diisi") String status,
            String notes
    ) {
    }

    public record EmergencyAlertRequest(
            String tripId,
            BigDecimal latitude,
            BigDecimal longitude,
            String message
    ) {
    }

    public record EmergencyAlertResponse(
            String alertId,
            String status,
            String ownerWhatsappEmergencyUrl
    ) {
    }

    public record TripDetailDto(
            String id,
            String tripCode,
            String bookingId,
            String bookingCode,
            String customerId,
            String customerName,
            String customerPhone,
            List<CustomerNote> customerNotes,
            Integer legNumber,
            String legLabel,
            String routeId,
            String routeName,
            String tripType,
            String driverId,
            String driverName,
            String driverPhone,
            String vehicleId,
            String vehiclePlate,
            String vehicleModel,
            OffsetDateTime scheduledPickupAt,
            Integer estimatedDurationMin,
            Integer prepBufferMin,
            Integer arrivalBufferMin,
            Integer repositionBufferMin,
            OffsetDateTime reservedStartAt,
            OffsetDateTime reservedEndAt,
            String pickupAddress,
            String destinationAddress,
            String pickupMapsUrl,
            String destinationMapsUrl,
            String customerWhatsappUrl,
            Integer passengerCount,
            Integer luggageCount,
            String flightNumber,
            BigDecimal agreedFareSnapshot,
            String status,
            Boolean trackingActive,
            OffsetDateTime trackingStartedAt,
            OffsetDateTime trackingEndedAt,
            OffsetDateTime lastGpsRecordedAt,
            BigDecimal lastGpsLat,
            BigDecimal lastGpsLng,
            BigDecimal gpsDistanceKm,
            BigDecimal odometerStartKm,
            BigDecimal odometerEndKm,
            Integer actualDurationMin,
            String completionNotes,
            String paymentStatus,
            BigDecimal totalPaidAmount,
            String cashCustodyStatus,
            BigDecimal driverCashHeldAmount,
            Long version,
            List<TripStop> stops,
            List<TripPayment> payments,
            List<TripExpense> expenses,
            List<VehicleInspection> inspections,
            List<LostAndFoundItem> lostAndFoundItems
    ) {
    }
}
