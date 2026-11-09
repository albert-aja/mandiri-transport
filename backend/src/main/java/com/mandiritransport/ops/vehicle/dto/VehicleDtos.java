package com.mandiritransport.ops.vehicle.dto;

import com.mandiritransport.ops.vehicle.entity.VehicleDocument;
import com.mandiritransport.ops.vehicle.entity.VehicleExpense;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceLog;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceSchedule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class VehicleDtos {

    private VehicleDtos() {
    }

    public record VehicleRequest(
            @NotBlank(message = "Nomor polisi wajib diisi") String registrationNumber,
            @NotBlank(message = "Nama/model unit wajib diisi") String nameModel,
            String brand,
            Integer manufactureYear,
            @NotBlank(message = "Tipe kendaraan wajib diisi") String vehicleType,
            @NotNull(message = "Kapasitas penumpang wajib diisi") Integer passengerCapacity,
            Integer luggageCapacityBags,
            String status,
            BigDecimal currentOdometerKm,
            String fuelType,
            String suitabilityTags,
            String notes
    ) {
    }

    public record VehicleDocumentRequest(
            @NotBlank(message = "docType wajib diisi") String docType,
            String docNumber,
            LocalDate issueDate,
            @NotNull(message = "expiryDate wajib diisi") LocalDate expiryDate,
            Integer reminderDaysBefore,
            String storagePath,
            String notes
    ) {
    }

    public record MaintenanceScheduleRequest(
            @NotBlank(message = "maintenanceType wajib diisi") String maintenanceType,
            BigDecimal intervalKm,
            Integer intervalMonths,
            BigDecimal lastDoneOdometerKm,
            LocalDate lastDoneDate,
            BigDecimal nextDueOdometerKm,
            LocalDate nextDueDate,
            BigDecimal estimatedCost
    ) {
    }

    public record MaintenanceLogRequest(
            @NotBlank(message = "maintenanceType wajib diisi") String maintenanceType,
            @NotNull(message = "serviceDate wajib diisi") LocalDate serviceDate,
            BigDecimal odometerKm,
            String vendorName,
            @NotNull(message = "costAmount wajib diisi") BigDecimal costAmount,
            BigDecimal downtimeHours,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String description,
            String receiptPath,
            Boolean recordAsVehicleExpense
    ) {
    }

    public record VehicleExpenseRequest(
            @NotBlank(message = "category wajib diisi") String category,
            @NotNull(message = "amount wajib diisi") BigDecimal amount,
            @NotNull(message = "expenseDate wajib diisi") LocalDate expenseDate,
            String description,
            String receiptPath
    ) {
    }

    public record RouteRequest(
            @NotBlank(message = "code wajib diisi") String code,
            @NotBlank(message = "name wajib diisi") String name,
            @NotBlank(message = "originName wajib diisi") String originName,
            @NotBlank(message = "destinationName wajib diisi") String destinationName,
            @NotNull(message = "typicalDistanceKm wajib diisi") BigDecimal typicalDistanceKm,
            @NotNull(message = "defaultDurationMin wajib diisi") Integer defaultDurationMin,
            Integer defaultPrepBufferMin,
            Integer defaultArrivalBufferMin,
            Integer defaultRepositionBufferMin,
            Boolean isLongDistance,
            Integer requiredRestAfterMin
    ) {
    }

    public record VehicleDetailDto(
            String id,
            String registrationNumber,
            String nameModel,
            String brand,
            Integer manufactureYear,
            String vehicleType,
            Integer passengerCapacity,
            Integer luggageCapacityBags,
            String status,
            BigDecimal currentOdometerKm,
            String fuelType,
            String suitabilityTags,
            String notes,
            List<VehicleDocument> documents,
            List<VehicleMaintenanceSchedule> maintenanceSchedules,
            List<VehicleMaintenanceLog> recentMaintenanceLogs,
            List<VehicleExpense> recentExpenses
    ) {
    }
}
