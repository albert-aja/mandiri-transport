package com.mandiritransport.ops.driver.dto;

import com.mandiritransport.ops.driver.entity.DriverUnavailabilityWindow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DriverDtos {

    private DriverDtos() {
    }

    public record CreateDriverRequest(
            String username,
            @NotBlank(message = "Nama lengkap supir wajib diisi") String fullName,
            @NotBlank(message = "Nomor telepon wajib diisi") String phone,
            String licenseNumber,
            LocalDate licenseExpiryDate,
            String availabilityStatus,
            BigDecimal maxDailyHours,
            String notes
    ) {
    }

    public record UpdateAvailabilityRequest(
            @NotBlank(message = "availabilityStatus wajib diisi") String availabilityStatus,
            String notes
    ) {
    }

    public record UnavailabilityWindowRequest(
            String statusType,
            String reason,
            @NotNull(message = "startAt wajib diisi") OffsetDateTime startAt,
            @NotNull(message = "endAt wajib diisi") OffsetDateTime endAt
    ) {
    }

    public record DriverDto(
            String id,
            String userId,
            String driverCode,
            String fullName,
            String phone,
            String whatsappUrl,
            String licenseNumber,
            LocalDate licenseExpiryDate,
            String availabilityStatus,
            BigDecimal maxDailyHours,
            String notes,
            BigDecimal cashHeldAmount,
            List<DriverUnavailabilityWindow> unavailabilityWindows
    ) {
    }
}
