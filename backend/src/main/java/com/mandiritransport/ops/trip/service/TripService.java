package com.mandiritransport.ops.trip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.auth.entity.AppUser;
import com.mandiritransport.ops.auth.mapper.AppUserMapper;
import com.mandiritransport.ops.booking.entity.Booking;
import com.mandiritransport.ops.booking.mapper.BookingMapper;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.GeoAndLinkUtils;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.customer.entity.Customer;
import com.mandiritransport.ops.customer.entity.CustomerNote;
import com.mandiritransport.ops.customer.mapper.CustomerMapper;
import com.mandiritransport.ops.customer.mapper.CustomerNoteMapper;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.finance.entity.TripExpense;
import com.mandiritransport.ops.finance.entity.TripPayment;
import com.mandiritransport.ops.finance.mapper.TripExpenseMapper;
import com.mandiritransport.ops.finance.mapper.TripPaymentMapper;
import com.mandiritransport.ops.finance.service.CostEngineService;
import com.mandiritransport.ops.notification.service.NotificationService;
import com.mandiritransport.ops.trip.dto.TripDtos.EmergencyAlertRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.EmergencyAlertResponse;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsBatchRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsBatchResponse;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsHistoryResponse;
import com.mandiritransport.ops.trip.dto.TripDtos.GpsPointInput;
import com.mandiritransport.ops.trip.dto.TripDtos.InspectionRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.LostAndFoundRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.TodayFlightScheduleDto;
import com.mandiritransport.ops.trip.dto.TripDtos.FlightScheduleInfoDto;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import com.mandiritransport.ops.flighttracking.entity.FlightTrackingCredential;
import com.mandiritransport.ops.flighttracking.entity.FlightTrackingPollLog;
import com.mandiritransport.ops.flighttracking.mapper.FlightTrackingCredentialMapper;
import com.mandiritransport.ops.flighttracking.mapper.FlightTrackingPollLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import com.mandiritransport.ops.trip.dto.TripDtos.UpdateLostAndFoundStatusRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.UpdateTripScheduleRequest;
import com.mandiritransport.ops.trip.dto.TripDtos.UpdateTripStatusRequest;
import com.mandiritransport.ops.trip.entity.EmergencyAlert;
import com.mandiritransport.ops.trip.entity.LostAndFoundItem;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.entity.TripGpsPoint;
import com.mandiritransport.ops.trip.entity.TripStatusHistory;
import com.mandiritransport.ops.trip.entity.TripStop;
import com.mandiritransport.ops.trip.entity.VehicleInspection;
import com.mandiritransport.ops.trip.mapper.EmergencyAlertMapper;
import com.mandiritransport.ops.trip.mapper.LostAndFoundItemMapper;
import com.mandiritransport.ops.trip.mapper.TripGpsPointMapper;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.trip.mapper.TripStatusHistoryMapper;
import com.mandiritransport.ops.trip.mapper.TripStopMapper;
import com.mandiritransport.ops.trip.mapper.VehicleInspectionMapper;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.entity.Vehicle;
import com.mandiritransport.ops.vehicle.mapper.RouteMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TripService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "UNASSIGNED", "ASSIGNED", "ACCEPTED", "ON_THE_WAY", "ARRIVED",
            "PASSENGER_ONBOARD", "COMPLETED", "CANCELLED", "NO_SHOW", "REASSIGNED"
    );

    private static final Set<String> ACTIVE_TRACKING_STATUSES = Set.of(
            "ACCEPTED", "ON_THE_WAY", "ARRIVED", "PASSENGER_ONBOARD"
    );

    private static final Set<String> TERMINAL_STATUSES = Set.of(
            "COMPLETED", "CANCELLED", "NO_SHOW"
    );

    private final TripMapper tripMapper;
    private final TripStopMapper stopMapper;
    private final TripStatusHistoryMapper historyMapper;
    private final TripGpsPointMapper gpsPointMapper;
    private final VehicleInspectionMapper inspectionMapper;
    private final LostAndFoundItemMapper lostAndFoundMapper;
    private final EmergencyAlertMapper emergencyAlertMapper;
    private final BookingMapper bookingMapper;
    private final CustomerMapper customerMapper;
    private final CustomerNoteMapper customerNoteMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final RouteMapper routeMapper;
    private final TripPaymentMapper paymentMapper;
    private final TripExpenseMapper expenseMapper;
    private final AppUserMapper appUserMapper;
    private final CostEngineService costEngineService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final FlightTrackingCredentialMapper flightCredentialMapper;
    private final FlightTrackingPollLogMapper flightPollLogMapper;
    private final ObjectMapper objectMapper;

    public TripService(TripMapper tripMapper,
                       TripStopMapper stopMapper,
                       TripStatusHistoryMapper historyMapper,
                       TripGpsPointMapper gpsPointMapper,
                       VehicleInspectionMapper inspectionMapper,
                       LostAndFoundItemMapper lostAndFoundMapper,
                       EmergencyAlertMapper emergencyAlertMapper,
                       BookingMapper bookingMapper,
                       CustomerMapper customerMapper,
                       CustomerNoteMapper customerNoteMapper,
                       DriverMapper driverMapper,
                       VehicleMapper vehicleMapper,
                       RouteMapper routeMapper,
                       TripPaymentMapper paymentMapper,
                       TripExpenseMapper expenseMapper,
                       AppUserMapper appUserMapper,
                       CostEngineService costEngineService,
                       NotificationService notificationService,
                       AuditService auditService,
                       FlightTrackingCredentialMapper flightCredentialMapper,
                       FlightTrackingPollLogMapper flightPollLogMapper,
                       ObjectMapper objectMapper) {
        this.tripMapper = tripMapper;
        this.stopMapper = stopMapper;
        this.historyMapper = historyMapper;
        this.gpsPointMapper = gpsPointMapper;
        this.inspectionMapper = inspectionMapper;
        this.lostAndFoundMapper = lostAndFoundMapper;
        this.emergencyAlertMapper = emergencyAlertMapper;
        this.bookingMapper = bookingMapper;
        this.customerMapper = customerMapper;
        this.customerNoteMapper = customerNoteMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.routeMapper = routeMapper;
        this.paymentMapper = paymentMapper;
        this.expenseMapper = expenseMapper;
        this.appUserMapper = appUserMapper;
        this.costEngineService = costEngineService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.flightCredentialMapper = flightCredentialMapper;
        this.flightPollLogMapper = flightPollLogMapper;
        this.objectMapper = objectMapper;
    }

    public List<TripDetailDto> listTrips(String status,
                                         String driverId,
                                         String vehicleId,
                                         LocalDate dateFrom,
                                         LocalDate dateTo) {
        SecurityUser current = SecurityUtils.currentUser();
        LambdaQueryWrapper<Trip> qw = new LambdaQueryWrapper<Trip>()
                .orderByDesc(Trip::getScheduledPickupAt);

        if (current.isDriver()) {
            qw.eq(Trip::getDriverId, SecurityUtils.requireDriverId());
        } else if (driverId != null && !driverId.isBlank()) {
            qw.eq(Trip::getDriverId, driverId);
        }

        if (status != null && !status.isBlank()) {
            qw.eq(Trip::getStatus, status.trim().toUpperCase());
        }
        if (vehicleId != null && !vehicleId.isBlank()) {
            qw.eq(Trip::getVehicleId, vehicleId);
        }
        if (dateFrom != null) {
            OffsetDateTime start = dateFrom.atStartOfDay(IdGenerator.WIB).toOffsetDateTime();
            qw.ge(Trip::getScheduledPickupAt, start);
        }
        if (dateTo != null) {
            OffsetDateTime end = dateTo.plusDays(1).atStartOfDay(IdGenerator.WIB).toOffsetDateTime();
            qw.lt(Trip::getScheduledPickupAt, end);
        }

        return tripMapper.selectList(qw).stream()
                .map(t -> toDetailDto(t, current.isDriver()))
                .toList();
    }

    public TripDetailDto getTrip(String id) {
        SecurityUser current = SecurityUtils.currentUser();
        Trip trip = requireTripWithAccess(id, current);
        return toDetailDto(trip, current.isDriver());
    }

    @Transactional
    public TripDetailDto updateTripSchedule(String id, UpdateTripScheduleRequest req) {
        SecurityUtils.requireOwner();
        Trip trip = tripMapper.selectById(id);
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan.");
        }
        int dur = req.estimatedDurationMin() != null ? req.estimatedDurationMin() : trip.getEstimatedDurationMin();
        int prep = req.prepBufferMin() != null ? req.prepBufferMin() : trip.getPrepBufferMin();
        int arr = req.arrivalBufferMin() != null ? req.arrivalBufferMin() : trip.getArrivalBufferMin();
        int repo = req.repositionBufferMin() != null ? req.repositionBufferMin() : trip.getRepositionBufferMin();

        trip.setScheduledPickupAt(req.scheduledPickupAt());
        trip.setEstimatedDurationMin(dur);
        trip.setPrepBufferMin(prep);
        trip.setArrivalBufferMin(arr);
        trip.setRepositionBufferMin(repo);
        trip.setReservedStartAt(req.scheduledPickupAt().minusMinutes(prep));
        trip.setReservedEndAt(req.scheduledPickupAt().plusMinutes((long) dur + arr + repo));
        trip.setUpdatedAt(OffsetDateTime.now());
        tripMapper.updateById(trip);

        notificationService.reschedulePickupNotifications(trip);
        auditService.log("TRIP", trip.getId(), "RESCHEDULE",
                "Memperbarui jadwal jemput " + trip.getTripCode(), null, trip);
        return toDetailDto(trip, false);
    }

    @Transactional
    public TripDetailDto updateStatus(String id, UpdateTripStatusRequest req, String headerMutationId) {
        SecurityUser current = SecurityUtils.currentUser();
        Trip trip = requireTripWithAccess(id, current);

        String mutationId = (req.clientMutationId() != null && !req.clientMutationId().isBlank())
                ? req.clientMutationId().trim()
                : (headerMutationId != null && !headerMutationId.isBlank() ? headerMutationId.trim() : null);

        if (mutationId != null) {
            TripStatusHistory dup = historyMapper.selectOne(new LambdaQueryWrapper<TripStatusHistory>()
                    .eq(TripStatusHistory::getTripId, id)
                    .eq(TripStatusHistory::getClientMutationId, mutationId)
                    .last("LIMIT 1"));
            if (dup != null) {
                return toDetailDto(trip, current.isDriver());
            }
        }

        if (req.expectedVersion() != null && !req.expectedVersion().equals(trip.getVersion())) {
            throw BusinessException.conflict("VERSION_CONFLICT",
                    "Status perjalanan sudah berubah di server (versi diharapkan " + req.expectedVersion()
                            + ", versi aktual " + trip.getVersion() + ").", null);
        }

        String targetStatus = req.newStatus().trim().toUpperCase();
        if (!VALID_STATUSES.contains(targetStatus)) {
            throw BusinessException.badRequest("INVALID_TRIP_STATUS", "Status perjalanan tidak valid: " + targetStatus);
        }
        if (current.isDriver() && ("CANCELLED".equals(targetStatus) || "REASSIGNED".equals(targetStatus))) {
            throw BusinessException.forbidden("Hanya OWNER yang dapat membatalkan atau mengalihkan penugasan perjalanan.");
        }
        if (TERMINAL_STATUSES.contains(trip.getStatus()) && !trip.getStatus().equals(targetStatus)) {
            throw BusinessException.badRequest("TERMINAL_TRIP_STATUS",
                    "Perjalanan sudah berstatus akhir (" + trip.getStatus() + ") dan tidak dapat diubah ke " + targetStatus);
        }

        OffsetDateTime now = OffsetDateTime.now();
        String prevStatus = trip.getStatus();
        trip.setStatus(targetStatus);
        trip.setUpdatedBy(current.userId());
        trip.setUpdatedAt(now);

        if (req.odometerStartKm() != null) {
            trip.setOdometerStartKm(req.odometerStartKm());
        }
        if (req.odometerEndKm() != null) {
            trip.setOdometerEndKm(req.odometerEndKm());
        }

        if (("ON_THE_WAY".equals(targetStatus) || "PASSENGER_ONBOARD".equals(targetStatus))
                && trip.getActualStartedAt() == null) {
            trip.setActualStartedAt(now);
        }

        if (TERMINAL_STATUSES.contains(targetStatus)) {
            // Hard-stop active GPS tracking when trip finishes/cancels/no-shows!
            if (Boolean.TRUE.equals(trip.getTrackingActive())) {
                trip.setTrackingActive(false);
                if (trip.getTrackingEndedAt() == null) {
                    trip.setTrackingEndedAt(now);
                }
            }
            if (req.reasonNotes() != null) {
                trip.setCancelOrOutcomeReason(req.reasonNotes());
            }
            notificationService.cancelPendingTripNotifications(trip.getId());
        }

        if ("COMPLETED".equals(targetStatus)) {
            trip.setActualCompletedAt(now);
            if (req.completionNotes() != null) {
                trip.setCompletionNotes(req.completionNotes());
            }
            if (trip.getActualStartedAt() != null) {
                long mins = Math.max(1L, Duration.between(trip.getActualStartedAt(), now).toMinutes());
                trip.setActualDurationMin((int) mins);
            } else if (trip.getActualDurationMin() == null) {
                trip.setActualDurationMin(trip.getEstimatedDurationMin());
            }

            if (trip.getOdometerEndKm() != null && trip.getVehicleId() != null) {
                Vehicle v = vehicleMapper.selectById(trip.getVehicleId());
                if (v != null && (v.getCurrentOdometerKm() == null
                        || trip.getOdometerEndKm().compareTo(v.getCurrentOdometerKm()) > 0)) {
                    v.setCurrentOdometerKm(trip.getOdometerEndKm());
                    v.setUpdatedAt(now);
                    vehicleMapper.updateById(v);
                }
            }
        }

        tripMapper.updateById(trip);
        recordStatusHistory(trip.getId(), prevStatus, targetStatus, current.userId(), mutationId, req.reasonNotes());
        syncParentBookingStatus(trip.getBookingId());

        if ("COMPLETED".equals(targetStatus)) {
            costEngineService.calculateAndSaveSnapshot(trip);
        }

        auditService.log("TRIP", trip.getId(), "STATUS_CHANGE",
                "Status " + trip.getTripCode() + ": " + prevStatus + " -> " + targetStatus, prevStatus, targetStatus);
        return toDetailDto(trip, current.isDriver());
    }

    @Transactional
    public TripDetailDto startTracking(String id) {
        SecurityUser current = SecurityUtils.currentUser();
        Trip trip = requireTripWithAccess(id, current);

        if (!ACTIVE_TRACKING_STATUSES.contains(trip.getStatus())) {
            throw BusinessException.badRequest("TRACKING_NOT_ALLOWED_IN_STATUS",
                    "Pelacakan GPS hanya diizinkan pada perjalanan aktif (ACCEPTED, ON_THE_WAY, ARRIVED, PASSENGER_ONBOARD). Status saat ini: "
                            + trip.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        trip.setTrackingActive(true);
        if (trip.getTrackingStartedAt() == null) {
            trip.setTrackingStartedAt(now);
        }
        trip.setTrackingEndedAt(null);
        trip.setUpdatedAt(now);
        tripMapper.updateById(trip);

        return toDetailDto(trip, current.isDriver());
    }

    @Transactional
    public TripDetailDto endTracking(String id) {
        SecurityUser current = SecurityUtils.currentUser();
        Trip trip = requireTripWithAccess(id, current);

        OffsetDateTime now = OffsetDateTime.now();
        trip.setTrackingActive(false);
        if (trip.getTrackingStartedAt() != null && trip.getTrackingEndedAt() == null) {
            trip.setTrackingEndedAt(now);
        }
        recomputeGpsSummaryOnTrip(trip);
        trip.setUpdatedAt(now);
        tripMapper.updateById(trip);

        return toDetailDto(trip, current.isDriver());
    }

    @Transactional
    public GpsBatchResponse uploadGpsBatch(String id, GpsBatchRequest req, boolean allowOfflineReassignedDriver) {
        SecurityUser current = SecurityUtils.currentUser();
        Trip trip = tripMapper.selectById(id);
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan.");
        }
        if (current.isDriver() && !allowOfflineReassignedDriver) {
            String myDriverId = SecurityUtils.requireDriverId();
            if (!myDriverId.equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Supir hanya dapat mengirim titik GPS untuk perjalanan miliknya.");
            }
        }

        if (trip.getTrackingStartedAt() == null) {
            throw BusinessException.badRequest("TRACKING_NEVER_STARTED",
                    "Sesi pelacakan GPS belum pernah dimulai untuk perjalanan ini.");
        }

        OffsetDateTime windowStart = trip.getTrackingStartedAt().minusSeconds(30);
        OffsetDateTime windowEnd = trip.getTrackingEndedAt() != null
                ? trip.getTrackingEndedAt().plusSeconds(30)
                : OffsetDateTime.now().plusMinutes(5);

        int accepted = 0;
        int duplicates = 0;
        int rejected = 0;

        for (GpsPointInput pt : req.points()) {
            TripGpsPoint existing = gpsPointMapper.selectOne(new LambdaQueryWrapper<TripGpsPoint>()
                    .eq(TripGpsPoint::getClientPointId, pt.clientPointId().trim())
                    .last("LIMIT 1"));
            if (existing != null) {
                duplicates++;
                continue;
            }

            if (pt.recordedAt().isBefore(windowStart) || pt.recordedAt().isAfter(windowEnd)) {
                rejected++;
                continue;
            }

            TripGpsPoint entity = new TripGpsPoint();
            entity.setId(IdGenerator.newId());
            entity.setTripId(trip.getId());
            entity.setClientPointId(pt.clientPointId().trim());
            entity.setLatitude(pt.latitude());
            entity.setLongitude(pt.longitude());
            entity.setSpeedKmh(pt.speedKmh());
            entity.setAccuracyM(pt.accuracyM());
            entity.setRecordedAt(pt.recordedAt());
            entity.setSyncedAt(OffsetDateTime.now());
            gpsPointMapper.insert(entity);
            accepted++;
        }

        if (accepted == 0 && duplicates == 0 && rejected > 0) {
            throw BusinessException.badRequest("GPS_OUTSIDE_TRACKING_WINDOW",
                    "Titik GPS ditolak karena direkam di luar sesi pelacakan perjalanan aktif.");
        }

        if (accepted > 0) {
            recomputeGpsSummaryOnTrip(trip);
            trip.setUpdatedAt(OffsetDateTime.now());
            tripMapper.updateById(trip);
        }

        return new GpsBatchResponse(
                trip.getId(),
                accepted,
                duplicates,
                rejected,
                trip.getLastGpsRecordedAt(),
                trip.getGpsDistanceKm()
        );
    }

    public GpsHistoryResponse getGpsPoints(String id) {
        SecurityUser current = SecurityUtils.currentUser();
        Trip trip = requireTripWithAccess(id, current);
        List<TripGpsPoint> points = gpsPointMapper.selectList(new LambdaQueryWrapper<TripGpsPoint>()
                .eq(TripGpsPoint::getTripId, id)
                .orderByAsc(TripGpsPoint::getRecordedAt));
        return new GpsHistoryResponse(
                trip.getId(),
                trip.getTrackingActive(),
                trip.getTrackingStartedAt(),
                trip.getTrackingEndedAt(),
                trip.getLastGpsRecordedAt(),
                trip.getGpsDistanceKm(),
                points
        );
    }

    @Transactional
    public VehicleInspection recordInspection(String tripId, InspectionRequest req, boolean allowOfflineReassignedDriver) {
        SecurityUser current = SecurityUtils.currentUser();
        if (req.clientMutationId() != null && !req.clientMutationId().isBlank()) {
            VehicleInspection existing = inspectionMapper.selectOne(new LambdaQueryWrapper<VehicleInspection>()
                    .eq(VehicleInspection::getClientMutationId, req.clientMutationId().trim())
                    .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
        }

        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan.");
        }
        if (current.isDriver() && !allowOfflineReassignedDriver) {
            if (!SecurityUtils.requireDriverId().equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Supir hanya dapat mengisi inspeksi untuk perjalanan miliknya.");
            }
        }

        String driverId = current.isDriver() ? SecurityUtils.requireDriverId() : trip.getDriverId();
        if (driverId == null || trip.getVehicleId() == null) {
            throw BusinessException.badRequest("MISSING_ASSIGNMENT",
                    "Supir dan kendaraan harus sudah ditugaskan sebelum mencatat inspeksi.");
        }

        VehicleInspection ins = new VehicleInspection();
        ins.setId(IdGenerator.newId());
        ins.setTripId(tripId);
        ins.setVehicleId(trip.getVehicleId());
        ins.setDriverId(driverId);
        ins.setClientMutationId(req.clientMutationId() != null && !req.clientMutationId().isBlank()
                ? req.clientMutationId().trim() : null);
        ins.setStage(req.stage() != null && !req.stage().isBlank() ? req.stage().trim().toUpperCase() : "PRE_TRIP");
        ins.setAllPassed(req.allPassed() == null || req.allPassed());
        ins.setFlaggedItemsJson(auditService.toJsonSafe(req.flaggedItems() != null ? req.flaggedItems() : List.of()));
        ins.setPhotoPathsJson(auditService.toJsonSafe(req.photoPaths() != null ? req.photoPaths() : List.of()));
        ins.setNotes(req.notes());
        ins.setInspectedAt(OffsetDateTime.now());
        ins.setCreatedAt(OffsetDateTime.now());
        inspectionMapper.insert(ins);
        return ins;
    }

    @Transactional
    public LostAndFoundItem createLostAndFound(String tripId, LostAndFoundRequest req, boolean allowOfflineReassignedDriver) {
        SecurityUser current = SecurityUtils.currentUser();
        if (req.clientMutationId() != null && !req.clientMutationId().isBlank()) {
            LostAndFoundItem existing = lostAndFoundMapper.selectOne(new LambdaQueryWrapper<LostAndFoundItem>()
                    .eq(LostAndFoundItem::getClientMutationId, req.clientMutationId().trim())
                    .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
        }

        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan.");
        }
        if (current.isDriver() && !allowOfflineReassignedDriver) {
            if (!SecurityUtils.requireDriverId().equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Supir hanya dapat melaporkan barang tertinggal pada perjalanan miliknya.");
            }
        }

        LostAndFoundItem item = new LostAndFoundItem();
        item.setId(IdGenerator.newId());
        item.setTripId(tripId);
        item.setVehicleId(trip.getVehicleId());
        item.setReportedBy(current.userId());
        item.setClientMutationId(req.clientMutationId() != null && !req.clientMutationId().isBlank()
                ? req.clientMutationId().trim() : null);
        item.setItemDescription(req.itemDescription().trim());
        item.setFoundDate(req.foundDate() != null ? req.foundDate() : LocalDate.now(IdGenerator.WIB));
        item.setPhotoPath(req.photoPath());
        item.setStatus(req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : "FOUND");
        item.setNotes(req.notes());
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        lostAndFoundMapper.insert(item);
        return item;
    }

    @Transactional
    public LostAndFoundItem updateLostAndFoundStatus(String itemId, UpdateLostAndFoundStatusRequest req) {
        LostAndFoundItem item = lostAndFoundMapper.selectById(itemId);
        if (item == null) {
            throw BusinessException.notFound("Data barang tertinggal tidak ditemukan.");
        }
        SecurityUser current = SecurityUtils.currentUser();
        if (current.isDriver()) {
            Trip trip = tripMapper.selectById(item.getTripId());
            if (trip == null || !SecurityUtils.requireDriverId().equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Akses ditolak.");
            }
        }
        item.setStatus(req.status().trim().toUpperCase());
        if (req.notes() != null) {
            item.setNotes(req.notes());
        }
        item.setUpdatedAt(OffsetDateTime.now());
        lostAndFoundMapper.updateById(item);
        return item;
    }

    public List<LostAndFoundItem> listLostAndFound() {
        SecurityUser current = SecurityUtils.currentUser();
        if (current.isDriver()) {
            String driverId = SecurityUtils.requireDriverId();
            List<Trip> myTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>().eq(Trip::getDriverId, driverId));
            if (myTrips.isEmpty()) {
                return Collections.emptyList();
            }
            Set<String> tripIds = myTrips.stream().map(Trip::getId).collect(Collectors.toSet());
            return lostAndFoundMapper.selectList(new LambdaQueryWrapper<LostAndFoundItem>()
                    .in(LostAndFoundItem::getTripId, tripIds)
                    .orderByDesc(LostAndFoundItem::getCreatedAt));
        }
        return lostAndFoundMapper.selectList(new LambdaQueryWrapper<LostAndFoundItem>()
                .orderByDesc(LostAndFoundItem::getCreatedAt));
    }

    @Transactional
    public EmergencyAlertResponse createEmergencyAlert(EmergencyAlertRequest req) {
        SecurityUser current = SecurityUtils.currentUser();
        Trip trip = (req.tripId() != null && !req.tripId().isBlank()) ? tripMapper.selectById(req.tripId()) : null;

        String driverId = current.isDriver() ? SecurityUtils.requireDriverId()
                : (trip != null ? trip.getDriverId() : null);
        if (driverId == null) {
            Driver anyDrv = driverMapper.selectList(null).stream().findFirst().orElse(null);
            driverId = anyDrv != null ? anyDrv.getId() : null;
        }
        if (driverId == null) {
            throw BusinessException.badRequest("DRIVER_NOT_FOUND", "Profil supir tidak ditemukan untuk peringatan darurat.");
        }

        Driver driver = driverMapper.selectById(driverId);
        Vehicle vehicle = (trip != null && trip.getVehicleId() != null) ? vehicleMapper.selectById(trip.getVehicleId()) : null;

        EmergencyAlert alert = new EmergencyAlert();
        alert.setId(IdGenerator.newId());
        alert.setDriverId(driverId);
        alert.setTripId(trip != null ? trip.getId() : null);
        alert.setVehicleId(vehicle != null ? vehicle.getId() : null);
        alert.setLatitude(req.latitude());
        alert.setLongitude(req.longitude());
        alert.setMessage(req.message() != null ? req.message() : "Peringatan darurat dari lapangan");
        alert.setStatus("OPEN");
        alert.setCreatedAt(OffsetDateTime.now());
        emergencyAlertMapper.insert(alert);

        String mapsUrl = GeoAndLinkUtils.buildGoogleMapsSearchUrl(req.latitude(), req.longitude(),
                trip != null ? trip.getPickupAddress() : null);
        String msgText = "[DARURAT MANDIRI TRANSPORT] Supir: " + (driver != null ? driver.getFullName() : driverId)
                + (trip != null ? " | Trip: " + trip.getTripCode() : "")
                + (vehicle != null ? " | Unit: " + vehicle.getRegistrationNumber() : "")
                + " | Pesan: " + alert.getMessage()
                + (mapsUrl != null ? " | Lokasi: " + mapsUrl : "");

        notificationService.notifyOwnerImmediate("EMERGENCY_ALERT", "EMERGENCY_ALERT", alert.getId(),
                "DARURAT: " + (driver != null ? driver.getFullName() : "Supir"), msgText);

        AppUser owner = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getRole, "OWNER")
                .eq(AppUser::getIsActive, true)
                .last("LIMIT 1"));
        String ownerPhone = owner != null ? owner.getPhone() : "+628116001001";
        String ownerWaUrl = GeoAndLinkUtils.buildWhatsappUrl(ownerPhone, msgText);

        return new EmergencyAlertResponse(alert.getId(), alert.getStatus(), ownerWaUrl);
    }

    public void recordStatusHistory(String tripId,
                                    String fromStatus,
                                    String toStatus,
                                    String changedBy,
                                    String clientMutationId,
                                    String reasonNotes) {
        TripStatusHistory hist = new TripStatusHistory();
        hist.setId(IdGenerator.newId());
        hist.setTripId(tripId);
        hist.setFromStatus(fromStatus);
        hist.setToStatus(toStatus);
        hist.setChangedBy(changedBy);
        hist.setClientMutationId(clientMutationId);
        hist.setReasonNotes(reasonNotes);
        hist.setRecordedAt(OffsetDateTime.now());
        hist.setCreatedAt(OffsetDateTime.now());
        historyMapper.insert(hist);
    }

    public TripDetailDto toDetailDto(Trip t, boolean driverScoped) {
        Booking b = bookingMapper.selectById(t.getBookingId());
        Customer c = b != null ? customerMapper.selectById(b.getCustomerId()) : null;
        Route r = t.getRouteId() != null ? routeMapper.selectById(t.getRouteId()) : null;
        Driver d = t.getDriverId() != null ? driverMapper.selectById(t.getDriverId()) : null;
        Vehicle v = t.getVehicleId() != null ? vehicleMapper.selectById(t.getVehicleId()) : null;

        List<CustomerNote> notes = Collections.emptyList();
        if (c != null) {
            LambdaQueryWrapper<CustomerNote> noteQw = new LambdaQueryWrapper<CustomerNote>()
                    .eq(CustomerNote::getCustomerId, c.getId())
                    .orderByDesc(CustomerNote::getCreatedAt);
            if (driverScoped) {
                noteQw.eq(CustomerNote::getVisibleToDrivers, true);
            }
            notes = customerNoteMapper.selectList(noteQw);
        }

        List<TripStop> stops = stopMapper.selectList(new LambdaQueryWrapper<TripStop>()
                .eq(TripStop::getTripId, t.getId())
                .orderByAsc(TripStop::getStopOrder));
        List<TripPayment> payments = paymentMapper.selectList(new LambdaQueryWrapper<TripPayment>()
                .eq(TripPayment::getTripId, t.getId())
                .orderByAsc(TripPayment::getPaidAt));
        List<TripExpense> expenses = expenseMapper.selectList(new LambdaQueryWrapper<TripExpense>()
                .eq(TripExpense::getTripId, t.getId())
                .eq(TripExpense::getIsVoided, false)
                .orderByAsc(TripExpense::getIncurredAt));
        List<VehicleInspection> inspections = inspectionMapper.selectList(new LambdaQueryWrapper<VehicleInspection>()
                .eq(VehicleInspection::getTripId, t.getId())
                .orderByDesc(VehicleInspection::getInspectedAt));
        List<LostAndFoundItem> lostItems = lostAndFoundMapper.selectList(new LambdaQueryWrapper<LostAndFoundItem>()
                .eq(LostAndFoundItem::getTripId, t.getId())
                .orderByDesc(LostAndFoundItem::getCreatedAt));

        String custPhone = c != null && c.getWhatsappPhone() != null && !c.getWhatsappPhone().isBlank()
                ? c.getWhatsappPhone() : (b != null ? b.getContactPhone() : (c != null ? c.getPhone() : null));

        return new TripDetailDto(
                t.getId(),
                t.getTripCode(),
                t.getBookingId(),
                b != null ? b.getBookingCode() : null,
                c != null ? c.getId() : (b != null ? b.getCustomerId() : null),
                c != null ? c.getName() : (b != null ? b.getContactName() : null),
                b != null ? b.getContactPhone() : (c != null ? c.getPhone() : null),
                notes,
                t.getLegNumber(),
                t.getLegLabel(),
                t.getRouteId(),
                r != null ? r.getName() : null,
                t.getTripType(),
                t.getDriverId(),
                d != null ? d.getFullName() : null,
                d != null ? d.getPhone() : null,
                t.getVehicleId(),
                v != null ? v.getRegistrationNumber() : null,
                v != null ? v.getNameModel() : null,
                t.getScheduledPickupAt(),
                t.getEstimatedDurationMin(),
                t.getPrepBufferMin(),
                t.getArrivalBufferMin(),
                t.getRepositionBufferMin(),
                t.getReservedStartAt(),
                t.getReservedEndAt(),
                t.getPickupAddress(),
                t.getDestinationAddress(),
                GeoAndLinkUtils.buildGoogleMapsDirectionsUrl(t.getPickupLat(), t.getPickupLng(), t.getPickupAddress()),
                GeoAndLinkUtils.buildGoogleMapsDirectionsUrl(t.getDestinationLat(), t.getDestinationLng(), t.getDestinationAddress()),
                GeoAndLinkUtils.buildWhatsappUrl(custPhone, null),
                t.getPassengerCount(),
                t.getLuggageCount(),
                t.getFlightNumber(),
                t.getAgreedFareSnapshot(),
                t.getStatus(),
                t.getTrackingActive(),
                t.getTrackingStartedAt(),
                t.getTrackingEndedAt(),
                t.getLastGpsRecordedAt(),
                t.getLastGpsLat(),
                t.getLastGpsLng(),
                t.getGpsDistanceKm(),
                t.getOdometerStartKm(),
                t.getOdometerEndKm(),
                t.getActualDurationMin(),
                t.getCompletionNotes(),
                t.getPaymentStatus(),
                t.getTotalPaidAmount(),
                t.getCashCustodyStatus(),
                t.getDriverCashHeldAmount(),
                t.getVersion(),
                stops,
                payments,
                expenses,
                inspections,
                lostItems
        );
    }

    private Trip requireTripWithAccess(String tripId, SecurityUser current) {
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan.");
        }
        if (current.isDriver()) {
            String myDriverId = SecurityUtils.requireDriverId();
            if (!myDriverId.equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Supir hanya dapat mengakses perjalanan yang ditugaskan kepadanya.");
            }
        }
        return trip;
    }

    private void recomputeGpsSummaryOnTrip(Trip trip) {
        List<TripGpsPoint> ordered = gpsPointMapper.selectList(new LambdaQueryWrapper<TripGpsPoint>()
                .eq(TripGpsPoint::getTripId, trip.getId())
                .orderByAsc(TripGpsPoint::getRecordedAt));
        if (ordered.isEmpty()) {
            return;
        }
        double totalKm = 0.0;
        for (int i = 1; i < ordered.size(); i++) {
            TripGpsPoint prev = ordered.get(i - 1);
            TripGpsPoint curr = ordered.get(i);
            totalKm += GeoAndLinkUtils.calculateDistanceKm(
                    prev.getLatitude().doubleValue(), prev.getLongitude().doubleValue(),
                    curr.getLatitude().doubleValue(), curr.getLongitude().doubleValue());
        }
        TripGpsPoint last = ordered.get(ordered.size() - 1);
        trip.setLastGpsRecordedAt(last.getRecordedAt());
        trip.setLastGpsLat(last.getLatitude());
        trip.setLastGpsLng(last.getLongitude());
        trip.setGpsDistanceKm(BigDecimal.valueOf(totalKm).setScale(2, RoundingMode.HALF_UP));
    }

    private void syncParentBookingStatus(String bookingId) {
        Booking b = bookingMapper.selectById(bookingId);
        if (b == null) {
            return;
        }
        List<Trip> legs = tripMapper.selectList(new LambdaQueryWrapper<Trip>().eq(Trip::getBookingId, bookingId));
        if (legs.isEmpty()) {
            return;
        }
        boolean allCancelled = legs.stream().allMatch(l -> "CANCELLED".equals(l.getStatus()));
        boolean allCompletedOrCancelled = legs.stream().allMatch(l ->
                "COMPLETED".equals(l.getStatus()) || "CANCELLED".equals(l.getStatus()) || "NO_SHOW".equals(l.getStatus()));
        boolean anyActive = legs.stream().anyMatch(l -> ACTIVE_TRACKING_STATUSES.contains(l.getStatus()));

        String nextBookingStatus = b.getStatus();
        if (allCancelled) {
            nextBookingStatus = "CANCELLED";
        } else if (allCompletedOrCancelled) {
            nextBookingStatus = "COMPLETED";
        } else if (anyActive) {
            nextBookingStatus = "IN_PROGRESS";
        }
        if (!nextBookingStatus.equals(b.getStatus())) {
            b.setStatus(nextBookingStatus);
            b.setUpdatedAt(OffsetDateTime.now());
            bookingMapper.updateById(b);
        }
    }

    public List<TodayFlightScheduleDto> listTodayFlightSchedules(LocalDate dateParam) {
        SecurityUser current = SecurityUtils.currentUser();
        LocalDate targetDate = dateParam != null ? dateParam : LocalDate.now(ZoneId.of("Asia/Jakarta"));
        OffsetDateTime startOfDay = targetDate.atStartOfDay(ZoneId.of("Asia/Jakarta")).toOffsetDateTime();
        OffsetDateTime endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Jakarta")).toOffsetDateTime();

        LambdaQueryWrapper<Trip> qw = new LambdaQueryWrapper<Trip>()
                .isNotNull(Trip::getFlightNumber)
                .ne(Trip::getFlightNumber, "")
                .ge(Trip::getScheduledPickupAt, startOfDay)
                .lt(Trip::getScheduledPickupAt, endOfDay)
                .orderByAsc(Trip::getScheduledPickupAt);

        if (current.isDriver()) {
            qw.eq(Trip::getDriverId, SecurityUtils.requireDriverId());
        }

        List<Trip> trips = tripMapper.selectList(qw);
        return trips.stream()
                .map(this::toTodayFlightScheduleDto)
                .toList();
    }

    public TodayFlightScheduleDto toTodayFlightScheduleDto(Trip t) {
        Booking b = bookingMapper.selectById(t.getBookingId());
        Customer c = b != null ? customerMapper.selectById(b.getCustomerId()) : null;
        Driver d = t.getDriverId() != null ? driverMapper.selectById(t.getDriverId()) : null;
        Vehicle v = t.getVehicleId() != null ? vehicleMapper.selectById(t.getVehicleId()) : null;
        Route r = t.getRouteId() != null ? routeMapper.selectById(t.getRouteId()) : null;

        String custPhone = c != null && c.getWhatsappPhone() != null && !c.getWhatsappPhone().isBlank()
                ? c.getWhatsappPhone() : (b != null ? b.getContactPhone() : (c != null ? c.getPhone() : null));

        FlightScheduleInfoDto flightInfo = resolveFlightScheduleInfo(t);

        return new TodayFlightScheduleDto(
                t.getId(),
                t.getTripCode(),
                t.getBookingId(),
                b != null ? b.getBookingCode() : null,
                c != null ? c.getId() : (b != null ? b.getCustomerId() : null),
                c != null ? c.getName() : (b != null ? b.getContactName() : "Pelanggan"),
                custPhone,
                t.getDriverId(),
                d != null ? d.getFullName() : null,
                t.getVehicleId(),
                v != null ? v.getRegistrationNumber() : null,
                v != null ? v.getNameModel() : null,
                t.getRouteId(),
                r != null ? r.getName() : null,
                t.getTripType(),
                t.getPickupAddress(),
                t.getDestinationAddress(),
                t.getScheduledPickupAt(),
                t.getStatus(),
                flightInfo
        );
    }

    public FlightScheduleInfoDto resolveFlightScheduleInfo(Trip t) {
        String flightNo = t.getFlightNumber();
        String airline = resolveAirlineName(flightNo);
        boolean isPickup = "AIRPORT_PICKUP".equalsIgnoreCase(t.getTripType());
        String defaultOrigin = isPickup ? "CGK" : "KNO";
        String defaultDest = isPickup ? "KNO" : "CGK";
        int bufferMin = t.getArrivalBufferMin() != null && t.getArrivalBufferMin() > 0 ? t.getArrivalBufferMin() : 30;
        OffsetDateTime scheduledArrival = isPickup
                ? t.getScheduledPickupAt().minusMinutes(bufferMin)
                : t.getScheduledPickupAt().plusMinutes(t.getEstimatedDurationMin() != null ? t.getEstimatedDurationMin() : 75);

        FlightTrackingPollLog latestLog = findLatestPollLogForFlight(t.getId(), flightNo);

        if (latestLog != null && Boolean.TRUE.equals(latestLog.getResponseValid())) {
            return mapPollLogToFlightSchedule(latestLog, flightNo, airline, defaultOrigin, defaultDest, scheduledArrival, t.getStatus());
        }

        String defaultStatus = "COMPLETED".equals(t.getStatus()) ? "LANDED" : "SCHEDULED";
        OffsetDateTime actualArrival = "COMPLETED".equals(t.getStatus()) ? t.getScheduledPickupAt() : null;

        return new FlightScheduleInfoDto(
                flightNo,
                airline,
                defaultOrigin,
                defaultDest,
                null,
                scheduledArrival,
                scheduledArrival,
                actualArrival,
                defaultStatus,
                0,
                null,
                "Terminal 1",
                null,
                "UNAVAILABLE",
                null,
                "Jadwal booking asli. Menunggu pembaruan pelacakan live."
        );
    }

    private FlightScheduleInfoDto mapPollLogToFlightSchedule(
            FlightTrackingPollLog log,
            String flightNo,
            String airline,
            String defaultOrigin,
            String defaultDest,
            OffsetDateTime scheduledArrivalFallback,
            String tripStatus) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime reqTs = log.getRequestTimestamp();
        Duration age = Duration.between(reqTs, now);
        String trackingStatus = age.toMinutes() <= 15 ? "LIVE" : (age.toMinutes() <= 60 ? "RECENT" : "STALE");

        String flightStatus = "SCHEDULED";
        Integer delayMin = 0;
        String origin = defaultOrigin;
        String dest = defaultDest;
        OffsetDateTime schedArr = scheduledArrivalFallback;
        OffsetDateTime estArr = scheduledArrivalFallback;
        OffsetDateTime actArr = null;
        String gate = null;
        String terminal = "Terminal 1";

        String rawJson = log.getResponsePayload() != null && !log.getResponsePayload().isBlank()
                ? log.getResponsePayload()
                : log.getNormalizedPayload();

        if (rawJson != null && !rawJson.isBlank()) {
            try {
                var map = objectMapper.readValue(rawJson, Map.class);
                var flights = map.get("flights");
                if (flights instanceof Map<?, ?> flightMap && !flightMap.isEmpty()) {
                    var firstFlight = flightMap.values().iterator().next();
                    if (firstFlight instanceof Map<?, ?> fData) {
                        if (fData.get("flightStatus") instanceof String fs && !fs.isBlank()) {
                            flightStatus = fs.toUpperCase(Locale.ROOT);
                        }
                        if (Boolean.TRUE.equals(fData.get("cancelled"))) {
                            flightStatus = "CANCELLED";
                        } else if (Boolean.TRUE.equals(fData.get("diverted"))) {
                            flightStatus = "DIVERTED";
                        }
                        if (fData.get("origin") instanceof Map<?, ?> oMap && oMap.get("iata") instanceof String oi) {
                            origin = oi;
                        }
                        if (fData.get("destination") instanceof Map<?, ?> dMap) {
                            if (dMap.get("iata") instanceof String di) dest = di;
                            if (dMap.get("gate") instanceof String dg) gate = dg;
                            if (dMap.get("terminal") instanceof String dt) terminal = dt;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if ("COMPLETED".equals(tripStatus) && !"CANCELLED".equals(flightStatus)) {
            flightStatus = "LANDED";
            actArr = schedArr;
        }

        String notes = "LIVE".equals(trackingStatus)
                ? "Pelacakan FlightAware aktif."
                : ("Data tersimpan (" + age.toMinutes() + " mnt lalu).");

        return new FlightScheduleInfoDto(
                flightNo,
                airline,
                origin,
                dest,
                null,
                schedArr,
                estArr,
                actArr,
                flightStatus,
                delayMin,
                gate,
                terminal,
                null,
                trackingStatus,
                reqTs,
                notes
        );
    }

    private FlightTrackingPollLog findLatestPollLogForFlight(String tripId, String flightNumber) {
        if (flightCredentialMapper == null || flightPollLogMapper == null) {
            return null;
        }
        try {
            List<FlightTrackingCredential> creds = flightCredentialMapper.selectList(
                    new LambdaQueryWrapper<FlightTrackingCredential>()
                            .eq(FlightTrackingCredential::getFlightId, tripId)
                            .or()
                            .eq(FlightTrackingCredential::getFlightNumber, normalizeFlightNumber(flightNumber))
                            .orderByDesc(FlightTrackingCredential::getExtractedAt)
            );
            if (creds.isEmpty()) return null;

            for (FlightTrackingCredential c : creds) {
                FlightTrackingPollLog log = flightPollLogMapper.selectOne(
                        new LambdaQueryWrapper<FlightTrackingPollLog>()
                                .eq(FlightTrackingPollLog::getCredentialId, c.getId())
                                .eq(FlightTrackingPollLog::getResponseValid, true)
                                .orderByDesc(FlightTrackingPollLog::getRequestTimestamp)
                                .last("LIMIT 1")
                );
                if (log != null) return log;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String resolveAirlineName(String flightNumber) {
        if (flightNumber == null || flightNumber.isBlank()) return "Penerbangan";
        String normalized = flightNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalized.startsWith("GA") || normalized.startsWith("GIA")) return "Garuda Indonesia";
        if (normalized.startsWith("JT") || normalized.startsWith("LNI")) return "Lion Air";
        if (normalized.startsWith("ID") || normalized.startsWith("BTK")) return "Batik Air";
        if (normalized.startsWith("QG") || normalized.startsWith("CTV")) return "Citilink";
        if (normalized.startsWith("IU") || normalized.startsWith("SJV")) return "Super Air Jet";
        if (normalized.startsWith("QZ") || normalized.startsWith("AWQ")) return "AirAsia Indonesia";
        if (normalized.startsWith("IW") || normalized.startsWith("WON")) return "Wings Air";
        if (normalized.startsWith("IP") || normalized.startsWith("BJV")) return "Pelita Air";
        if (normalized.startsWith("MH") || normalized.startsWith("MAS")) return "Malaysia Airlines";
        if (normalized.startsWith("SQ") || normalized.startsWith("SIA")) return "Singapore Airlines";
        if (normalized.startsWith("AK")) return "AirAsia";
        if (normalized.startsWith("TR") || normalized.startsWith("TGW")) return "Scoot";
        if (normalized.startsWith("SJ") || normalized.startsWith("SJY")) return "Sriwijaya Air";
        if (normalized.startsWith("IN") || normalized.startsWith("NAM")) return "NAM Air";
        return "Penerbangan";
    }

    private String normalizeFlightNumber(String flightNo) {
        if (flightNo == null) return "";
        return flightNo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
