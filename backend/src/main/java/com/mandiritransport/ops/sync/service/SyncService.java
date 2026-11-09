package com.mandiritransport.ops.sync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.customer.service.CustomerService;
import com.mandiritransport.ops.driver.dto.DriverDtos.DriverDto;
import com.mandiritransport.ops.driver.service.DriverService;
import com.mandiritransport.ops.finance.dto.FinanceDtos.RecordPaymentRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.RecordTripExpenseRequest;
import com.mandiritransport.ops.finance.service.FinanceService;
import com.mandiritransport.ops.sync.dto.SyncDtos.DriverBootstrapResponse;
import com.mandiritransport.ops.sync.dto.SyncDtos.ResolveSyncConflictRequest;
import com.mandiritransport.ops.sync.dto.SyncDtos.SyncMutationItem;
import com.mandiritransport.ops.sync.dto.SyncDtos.SyncMutationResultItem;
import com.mandiritransport.ops.sync.dto.SyncDtos.SyncMutationsRequest;
import com.mandiritransport.ops.sync.dto.SyncDtos.SyncMutationsResponse;
import com.mandiritransport.ops.sync.entity.SyncConflictRecord;
import com.mandiritransport.ops.sync.entity.SyncMutationLog;
import com.mandiritransport.ops.sync.mapper.SyncConflictRecordMapper;
import com.mandiritransport.ops.sync.mapper.SyncMutationLogMapper;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsBatchRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.InspectionRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.LostAndFoundRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import com.mandiritransport.ops.trip.dto.TripDtos.UpdateTripStatusRequest;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.trip.service.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SyncService {

    private final SyncMutationLogMapper mutationLogMapper;
    private final SyncConflictRecordMapper conflictMapper;
    private final TripMapper tripMapper;
    private final TripService tripService;
    private final FinanceService financeService;
    private final DriverService driverService;
    private final CustomerService customerService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public SyncService(SyncMutationLogMapper mutationLogMapper,
                       SyncConflictRecordMapper conflictMapper,
                       TripMapper tripMapper,
                       TripService tripService,
                       FinanceService financeService,
                       DriverService driverService,
                       CustomerService customerService,
                       ObjectMapper objectMapper,
                       AuditService auditService) {
        this.mutationLogMapper = mutationLogMapper;
        this.conflictMapper = conflictMapper;
        this.tripMapper = tripMapper;
        this.tripService = tripService;
        this.financeService = financeService;
        this.driverService = driverService;
        this.customerService = customerService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    public DriverBootstrapResponse getDriverBootstrap() {
        SecurityUser current = SecurityUtils.currentUser();
        DriverDto driverDto = current.driverId() != null ? driverService.getDriverDto(current.driverId()) : null;
        List<TripDetailDto> trips = tripService.listTrips(null, current.driverId(), null, null, null);
        return new DriverBootstrapResponse(
                OffsetDateTime.now(),
                driverDto,
                trips,
                customerService.listWhatsappTemplates()
        );
    }

    @Transactional
    public SyncMutationsResponse processMutations(SyncMutationsRequest req) {
        SecurityUser current = SecurityUtils.currentUser();
        List<SyncMutationResultItem> results = new ArrayList<>();

        for (SyncMutationItem item : req.mutations()) {
            String mutId = item.clientMutationId().trim();
            SyncMutationLog existingLog = mutationLogMapper.selectById(mutId);
            if (existingLog != null) {
                Trip t = tripMapper.selectById(item.entityId());
                results.add(new SyncMutationResultItem(
                        mutId,
                        "DUPLICATE_IGNORED",
                        "Mutasi sudah pernah diproses sebelumnya (idempoten).",
                        t != null ? t.getVersion() : null
                ));
                continue;
            }

            SyncMutationResultItem result = applySingleMutation(current, item);
            results.add(result);
        }

        long openConflicts = conflictMapper.selectCount(new LambdaQueryWrapper<SyncConflictRecord>()
                .eq(SyncConflictRecord::getResolutionStatus, "NEEDS_REVIEW"));
        return new SyncMutationsResponse(results, openConflicts);
    }

    public List<SyncConflictRecord> listConflicts(String status) {
        SecurityUtils.requireOwner();
        LambdaQueryWrapper<SyncConflictRecord> qw = new LambdaQueryWrapper<SyncConflictRecord>()
                .orderByDesc(SyncConflictRecord::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(SyncConflictRecord::getResolutionStatus, status.trim().toUpperCase());
        }
        return conflictMapper.selectList(qw);
    }

    @Transactional
    public SyncConflictRecord resolveConflict(String id, ResolveSyncConflictRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        SyncConflictRecord conflict = conflictMapper.selectById(id);
        if (conflict == null) {
            throw BusinessException.notFound("Data konflik sinkronisasi tidak ditemukan.");
        }
        String resStatus = req.resolutionStatus().trim().toUpperCase();
        if (!"ACCEPTED_SERVER".equals(resStatus) && !"MERGED_MANUALLY".equals(resStatus)) {
            throw BusinessException.badRequest("INVALID_RESOLUTION_STATUS",
                    "Status resolusi harus ACCEPTED_SERVER atau MERGED_MANUALLY.");
        }
        conflict.setResolutionStatus(resStatus);
        conflict.setResolvedBy(owner.userId());
        conflict.setResolvedAt(OffsetDateTime.now());
        if (req.notes() != null && !req.notes().isBlank()) {
            conflict.setConflictReason(conflict.getConflictReason() + " | Catatan resolusi: " + req.notes());
        }
        conflictMapper.updateById(conflict);
        auditService.log("SYNC_CONFLICT", conflict.getId(), "RESOLVE",
                "Menyelesaikan konflik sinkronisasi: " + resStatus, null, conflict);
        return conflict;
    }

    private SyncMutationResultItem applySingleMutation(SecurityUser actor, SyncMutationItem item) {
        String mutId = item.clientMutationId().trim();
        String type = item.mutationType().trim().toUpperCase();
        String entityId = item.entityId().trim();
        Map<String, Object> payload = item.payload() != null ? new HashMap<>(item.payload()) : new HashMap<>();
        payload.putIfAbsent("clientMutationId", mutId);

        return switch (type) {
            case "TRIP_STATUS" -> handleTripStatusMutation(actor, item, payload);
            case "TRIP_PAYMENT" -> {
                // Financial records are NEVER discarded even if trip was reassigned or cancelled!
                RecordPaymentRequest payReq = convertPayload(payload, RecordPaymentRequest.class);
                financeService.recordPayment(entityId, payReq, mutId, true);
                Trip updatedTrip = tripMapper.selectById(entityId);
                saveMutationLog(mutId, actor.userId(), type, entityId, "APPLIED", payReq);
                yield new SyncMutationResultItem(mutId, "APPLIED",
                        "Pembayaran offline berhasil dicatat dan diamankan.",
                        updatedTrip != null ? updatedTrip.getVersion() : null);
            }
            case "TRIP_EXPENSE" -> {
                RecordTripExpenseRequest expReq = convertPayload(payload, RecordTripExpenseRequest.class);
                financeService.recordTripExpense(entityId, expReq, mutId, true);
                Trip updatedTrip = tripMapper.selectById(entityId);
                saveMutationLog(mutId, actor.userId(), type, entityId, "APPLIED", expReq);
                yield new SyncMutationResultItem(mutId, "APPLIED",
                        "Pengeluaran perjalanan offline berhasil dicatat.",
                        updatedTrip != null ? updatedTrip.getVersion() : null);
            }
            case "GPS_BATCH" -> {
                GpsBatchRequest gpsReq = convertPayload(payload, GpsBatchRequest.class);
                tripService.uploadGpsBatch(entityId, gpsReq, true);
                Trip updatedTrip = tripMapper.selectById(entityId);
                saveMutationLog(mutId, actor.userId(), type, entityId, "APPLIED", gpsReq);
                yield new SyncMutationResultItem(mutId, "APPLIED",
                        "Titik rute GPS offline berhasil disinkronkan.",
                        updatedTrip != null ? updatedTrip.getVersion() : null);
            }
            case "VEHICLE_INSPECTION" -> {
                InspectionRequest insReq = convertPayload(payload, InspectionRequest.class);
                tripService.recordInspection(entityId, insReq, true);
                Trip updatedTrip = tripMapper.selectById(entityId);
                saveMutationLog(mutId, actor.userId(), type, entityId, "APPLIED", insReq);
                yield new SyncMutationResultItem(mutId, "APPLIED",
                        "Inspeksi kendaraan offline berhasil disimpan.",
                        updatedTrip != null ? updatedTrip.getVersion() : null);
            }
            case "LOST_AND_FOUND" -> {
                LostAndFoundRequest lafReq = convertPayload(payload, LostAndFoundRequest.class);
                tripService.createLostAndFound(entityId, lafReq, true);
                Trip updatedTrip = tripMapper.selectById(entityId);
                saveMutationLog(mutId, actor.userId(), type, entityId, "APPLIED", lafReq);
                yield new SyncMutationResultItem(mutId, "APPLIED",
                        "Laporan barang tertinggal offline berhasil disimpan.",
                        updatedTrip != null ? updatedTrip.getVersion() : null);
            }
            default -> throw BusinessException.badRequest("UNSUPPORTED_MUTATION_TYPE",
                    "Tipe mutasi sinkronisasi tidak didukung: " + type);
        };
    }

    private SyncMutationResultItem handleTripStatusMutation(SecurityUser actor,
                                                            SyncMutationItem item,
                                                            Map<String, Object> payload) {
        String mutId = item.clientMutationId().trim();
        Trip trip = tripMapper.selectById(item.entityId());
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan untuk sinkronisasi: " + item.entityId());
        }

        String conflictReason = null;
        if (actor.isDriver() && (trip.getDriverId() == null || !trip.getDriverId().equals(actor.driverId()))) {
            conflictReason = "Perjalanan " + trip.getTripCode()
                    + " telah dialihkan ke supir lain saat perangkat Anda sedang offline.";
        } else if ("CANCELLED".equalsIgnoreCase(trip.getStatus())) {
            conflictReason = "Perjalanan " + trip.getTripCode()
                    + " telah dibatalkan oleh Owner di server saat perangkat Anda offline.";
        } else if (item.expectedVersion() != null && !item.expectedVersion().equals(trip.getVersion())) {
            conflictReason = "Versi perjalanan di server berubah (diharapkan v" + item.expectedVersion()
                    + ", aktual v" + trip.getVersion() + ", status server: " + trip.getStatus() + ").";
        }

        if (conflictReason != null) {
            SyncConflictRecord conflict = new SyncConflictRecord();
            conflict.setId(IdGenerator.newId());
            conflict.setClientMutationId(mutId);
            conflict.setActorUserId(actor.userId());
            conflict.setEntityType("TRIP");
            conflict.setEntityId(trip.getId());
            conflict.setMutationType("TRIP_STATUS");
            conflict.setExpectedVersion(item.expectedVersion());
            conflict.setActualVersion(trip.getVersion());
            conflict.setClientPayloadJson(auditService.toJsonSafe(payload));
            conflict.setServerStateJson(auditService.toJsonSafe(trip));
            conflict.setConflictReason(conflictReason);
            conflict.setResolutionStatus("NEEDS_REVIEW");
            conflict.setCreatedAt(OffsetDateTime.now());
            conflictMapper.insert(conflict);

            saveMutationLog(mutId, actor.userId(), "TRIP_STATUS", trip.getId(), "CONFLICT_STAGED", conflict);
            return new SyncMutationResultItem(mutId, "CONFLICT_STAGED", conflictReason, trip.getVersion());
        }

        // Clear expectedVersion inside payload since we already checked it above
        payload.remove("expectedVersion");
        UpdateTripStatusRequest statusReq = convertPayload(payload, UpdateTripStatusRequest.class);
        TripDetailDto updated = tripService.updateStatus(trip.getId(), statusReq, mutId);
        saveMutationLog(mutId, actor.userId(), "TRIP_STATUS", trip.getId(), "APPLIED", updated.status());
        return new SyncMutationResultItem(mutId, "APPLIED",
                "Status perjalanan diperbarui ke " + updated.status(), updated.version());
    }

    private <T> T convertPayload(Map<String, Object> payload, Class<T> targetClass) {
        try {
            return objectMapper.convertValue(payload, targetClass);
        } catch (Exception e) {
            throw BusinessException.badRequest("INVALID_SYNC_PAYLOAD",
                    "Payload sinkronisasi tidak sesuai format: " + e.getMessage());
        }
    }

    private void saveMutationLog(String mutId,
                                 String actorUserId,
                                 String type,
                                 String entityId,
                                 String outcome,
                                 Object responseObj) {
        SyncMutationLog log = new SyncMutationLog();
        log.setClientMutationId(mutId);
        log.setActorUserId(actorUserId);
        log.setMutationType(type);
        log.setEntityId(entityId);
        log.setOutcomeStatus(outcome);
        log.setResponseJson(auditService.toJsonSafe(responseObj));
        log.setProcessedAt(OffsetDateTime.now());
        mutationLogMapper.insert(log);
    }
}
