package com.mandiritransport.ops.sync.dto;

import com.mandiritransport.ops.customer.entity.WhatsappTemplate;
import com.mandiritransport.ops.driver.dto.DriverDtos.DriverDto;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class SyncDtos {

    private SyncDtos() {
    }

    public record DriverBootstrapResponse(
            OffsetDateTime syncedAt,
            DriverDto driver,
            List<TripDetailDto> trips,
            List<WhatsappTemplate> whatsappTemplates
    ) {
    }

    public record SyncMutationItem(
            @NotBlank(message = "clientMutationId wajib diisi") String clientMutationId,
            @NotBlank(message = "mutationType wajib diisi") String mutationType,
            @NotBlank(message = "entityId wajib diisi") String entityId,
            Long expectedVersion,
            OffsetDateTime recordedAt,
            Map<String, Object> payload
    ) {
    }

    public record SyncMutationsRequest(
            @NotEmpty(message = "Daftar mutasi tidak boleh kosong") @Valid List<SyncMutationItem> mutations
    ) {
    }

    public record SyncMutationResultItem(
            String clientMutationId,
            String outcomeStatus,
            String message,
            Long serverVersion
    ) {
    }

    public record SyncMutationsResponse(
            List<SyncMutationResultItem> results,
            long openConflictsCount
    ) {
    }

    public record ResolveSyncConflictRequest(
            @NotBlank(message = "resolutionStatus wajib diisi") String resolutionStatus,
            String notes
    ) {
    }
}
