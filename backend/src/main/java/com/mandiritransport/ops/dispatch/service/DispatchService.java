package com.mandiritransport.ops.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.booking.entity.Booking;
import com.mandiritransport.ops.booking.mapper.BookingMapper;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.customer.entity.Customer;
import com.mandiritransport.ops.customer.mapper.CustomerMapper;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.AssignTripRequest;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.ConflictItemDto;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.ConflictSimulateRequest;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.ConflictSimulationResult;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.DriverRecommendationDto;
import com.mandiritransport.ops.dispatch.dto.DispatchDtos.VehicleRecommendationDto;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.entity.DriverUnavailabilityWindow;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.driver.mapper.DriverUnavailabilityWindowMapper;
import com.mandiritransport.ops.notification.service.NotificationService;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.trip.service.TripService;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.entity.Vehicle;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceLog;
import com.mandiritransport.ops.vehicle.mapper.RouteMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMaintenanceLogMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DispatchService {

    private static final Set<String> NON_BLOCKING_STATUSES = Set.of("CANCELLED", "NO_SHOW", "REASSIGNED");
    private static final DateTimeFormatter WINDOW_FMT =
            DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.forLanguageTag("id-ID"));

    private final TripMapper tripMapper;
    private final BookingMapper bookingMapper;
    private final CustomerMapper customerMapper;
    private final DriverMapper driverMapper;
    private final DriverUnavailabilityWindowMapper unavailabilityMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleMaintenanceLogMapper maintenanceLogMapper;
    private final RouteMapper routeMapper;
    private final TripService tripService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public DispatchService(TripMapper tripMapper,
                           BookingMapper bookingMapper,
                           CustomerMapper customerMapper,
                           DriverMapper driverMapper,
                           DriverUnavailabilityWindowMapper unavailabilityMapper,
                           VehicleMapper vehicleMapper,
                           VehicleMaintenanceLogMapper maintenanceLogMapper,
                           RouteMapper routeMapper,
                           TripService tripService,
                           NotificationService notificationService,
                           AuditService auditService) {
        this.tripMapper = tripMapper;
        this.bookingMapper = bookingMapper;
        this.customerMapper = customerMapper;
        this.driverMapper = driverMapper;
        this.unavailabilityMapper = unavailabilityMapper;
        this.vehicleMapper = vehicleMapper;
        this.maintenanceLogMapper = maintenanceLogMapper;
        this.routeMapper = routeMapper;
        this.tripService = tripService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public ConflictSimulationResult simulate(ConflictSimulateRequest req) {
        SecurityUtils.requireOwner();

        Trip existingTrip = (req.tripId() != null && !req.tripId().isBlank())
                ? tripMapper.selectById(req.tripId()) : null;
        Booking booking = (existingTrip != null && existingTrip.getBookingId() != null)
                ? bookingMapper.selectById(existingTrip.getBookingId()) : null;

        String routeId = (req.routeId() != null && !req.routeId().isBlank())
                ? req.routeId() : (existingTrip != null ? existingTrip.getRouteId() : null);
        Route route = routeId != null ? routeMapper.selectById(routeId) : null;

        OffsetDateTime pickupAt = req.scheduledPickupAt() != null
                ? req.scheduledPickupAt()
                : (existingTrip != null ? existingTrip.getScheduledPickupAt() : OffsetDateTime.now().plusHours(2));

        int durationMin = req.estimatedDurationMin() != null ? req.estimatedDurationMin()
                : (existingTrip != null ? existingTrip.getEstimatedDurationMin()
                : (route != null ? route.getDefaultDurationMin() : 60));
        int prepMin = req.prepBufferMin() != null ? req.prepBufferMin()
                : (existingTrip != null ? existingTrip.getPrepBufferMin()
                : (route != null ? route.getDefaultPrepBufferMin() : 15));
        int arrivalMin = req.arrivalBufferMin() != null ? req.arrivalBufferMin()
                : (existingTrip != null ? existingTrip.getArrivalBufferMin()
                : (route != null ? route.getDefaultArrivalBufferMin() : 0));
        int repoMin = req.repositionBufferMin() != null ? req.repositionBufferMin()
                : (existingTrip != null ? existingTrip.getRepositionBufferMin()
                : (route != null ? route.getDefaultRepositionBufferMin() : 15));

        int candidateRestMin = (route != null && route.getRequiredRestAfterMin() != null)
                ? route.getRequiredRestAfterMin() : 0;

        OffsetDateTime reservedStart = pickupAt.minusMinutes(prepMin);
        OffsetDateTime reservedEnd = pickupAt.plusMinutes((long) durationMin + arrivalMin + repoMin);

        int pax = req.passengerCount() != null ? req.passengerCount()
                : (existingTrip != null && existingTrip.getPassengerCount() != null ? existingTrip.getPassengerCount() : 1);
        int luggage = req.luggageCount() != null ? req.luggageCount()
                : (existingTrip != null && existingTrip.getLuggageCount() != null ? existingTrip.getLuggageCount() : 0);
        String tripType = req.tripType() != null && !req.tripType().isBlank() ? req.tripType()
                : (existingTrip != null ? existingTrip.getTripType() : "LOCAL");
        String customerId = req.customerId() != null && !req.customerId().isBlank() ? req.customerId()
                : (booking != null ? booking.getCustomerId() : null);
        Customer customer = customerId != null ? customerMapper.selectById(customerId) : null;
        String preferredVehicleType = customer != null ? customer.getPreferredVehicleType() : null;

        Map<String, Route> routeMap = routeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Route::getId, r -> r, (a, b) -> a));

        List<Trip> allOtherTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                        .notIn(Trip::getStatus, NON_BLOCKING_STATUSES))
                .stream()
                .filter(t -> existingTrip == null || !t.getId().equals(existingTrip.getId()))
                .toList();

        // Candidate Driver Conflicts (independent of vehicle!)
        List<ConflictItemDto> driverConflicts = new ArrayList<>();
        if (req.candidateDriverId() != null && !req.candidateDriverId().isBlank()) {
            Driver candDrv = driverMapper.selectById(req.candidateDriverId());
            if (candDrv == null) {
                driverConflicts.add(new ConflictItemDto("DRIVER_NOT_FOUND", null, reservedStart, reservedEnd,
                        "Supir tidak ditemukan."));
            } else {
                driverConflicts.addAll(checkDriverConflicts(candDrv, reservedStart, reservedEnd,
                        candidateRestMin, allOtherTrips, routeMap));
            }
        }

        // Candidate Vehicle Conflicts (independent of driver!)
        List<ConflictItemDto> vehicleConflicts = new ArrayList<>();
        if (req.candidateVehicleId() != null && !req.candidateVehicleId().isBlank()) {
            Vehicle candVeh = vehicleMapper.selectById(req.candidateVehicleId());
            if (candVeh == null) {
                vehicleConflicts.add(new ConflictItemDto("VEHICLE_NOT_FOUND", null, reservedStart, reservedEnd,
                        "Kendaraan tidak ditemukan."));
            } else {
                vehicleConflicts.addAll(checkVehicleConflicts(candVeh, reservedStart, reservedEnd,
                        pax, luggage, allOtherTrips));
            }
        }

        // 13.1 Driver Workload Balancing Recommendations
        LocalDate targetDay = pickupAt.atZoneSameInstant(IdGenerator.WIB).toLocalDate();
        List<DriverRecommendationDto> driverRecs = buildDriverRecommendations(
                reservedStart, reservedEnd, candidateRestMin, targetDay, allOtherTrips, routeMap);

        // 13.3 Vehicle Suitability Recommendations
        List<VehicleRecommendationDto> vehicleRecs = buildVehicleRecommendations(
                reservedStart, reservedEnd, pax, luggage, tripType, preferredVehicleType, allOtherTrips);

        return new ConflictSimulationResult(
                reservedStart,
                reservedEnd,
                !driverConflicts.isEmpty(),
                driverConflicts,
                !vehicleConflicts.isEmpty(),
                vehicleConflicts,
                driverRecs,
                vehicleRecs
        );
    }

    @Transactional
    public TripDetailDto assignTrip(String tripId, AssignTripRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan.");
        }
        if ((req.driverId() == null || req.driverId().isBlank())
                && (req.vehicleId() == null || req.vehicleId().isBlank())) {
            throw BusinessException.badRequest("MISSING_ASSIGNMENT", "Pilih supir atau unit kendaraan yang akan ditugaskan.");
        }

        OffsetDateTime pickupAt = req.scheduledPickupAt() != null ? req.scheduledPickupAt() : trip.getScheduledPickupAt();
        int prepMin = req.prepBufferMin() != null ? req.prepBufferMin() : trip.getPrepBufferMin();
        int arrMin = req.arrivalBufferMin() != null ? req.arrivalBufferMin() : trip.getArrivalBufferMin();
        int repoMin = req.repositionBufferMin() != null ? req.repositionBufferMin() : trip.getRepositionBufferMin();

        String targetDriverId = req.driverId() != null && !req.driverId().isBlank() ? req.driverId() : trip.getDriverId();
        String targetVehicleId = req.vehicleId() != null && !req.vehicleId().isBlank() ? req.vehicleId() : trip.getVehicleId();

        ConflictSimulationResult sim = simulate(new ConflictSimulateRequest(
                trip.getId(),
                trip.getRouteId(),
                pickupAt,
                trip.getEstimatedDurationMin(),
                prepMin,
                arrMin,
                repoMin,
                targetDriverId,
                targetVehicleId,
                trip.getPassengerCount(),
                trip.getLuggageCount(),
                trip.getTripType(),
                null
        ));

        boolean hasConflict = sim.hasDriverConflict() || sim.hasVehicleConflict();
        if (hasConflict && !Boolean.TRUE.equals(req.forceOverrideConfirmed())) {
            String msg = buildConflictSummaryMessage(sim);
            throw BusinessException.conflict("SCHEDULE_CONFLICT", msg, sim);
        }

        String previousDriverId = trip.getDriverId();
        String previousStatus = trip.getStatus();

        trip.setScheduledPickupAt(pickupAt);
        trip.setPrepBufferMin(prepMin);
        trip.setArrivalBufferMin(arrMin);
        trip.setRepositionBufferMin(repoMin);
        trip.setReservedStartAt(sim.reservedStartAt());
        trip.setReservedEndAt(sim.reservedEndAt());
        trip.setDriverId(targetDriverId);
        trip.setVehicleId(targetVehicleId);

        if (hasConflict) {
            String note = (req.overrideReason() != null && !req.overrideReason().isBlank())
                    ? req.overrideReason().trim()
                    : "Override konflik dikonfirmasi eksplisit oleh Owner";
            trip.setConflictOverrideNote(note);
        }

        boolean wasAssignedToAnotherDriver = previousDriverId != null
                && targetDriverId != null
                && !previousDriverId.equals(targetDriverId);

        if ("UNASSIGNED".equals(previousStatus) || "REASSIGNED".equals(previousStatus) || wasAssignedToAnotherDriver) {
            if (wasAssignedToAnotherDriver) {
                tripService.recordStatusHistory(trip.getId(), previousStatus, "REASSIGNED", owner.userId(), null,
                        "Dialihkan dari supir sebelumnya");
            }
            trip.setStatus("ASSIGNED");
            tripService.recordStatusHistory(trip.getId(), wasAssignedToAnotherDriver ? "REASSIGNED" : previousStatus,
                    "ASSIGNED", owner.userId(), null,
                    hasConflict ? ("Penugasan dengan override: " + trip.getConflictOverrideNote()) : "Penugasan supir & armada");
        }

        trip.setUpdatedBy(owner.userId());
        trip.setUpdatedAt(OffsetDateTime.now());
        tripMapper.updateById(trip);

        notificationService.scheduleTripAssignmentAndPickupReminders(trip);

        auditService.log("TRIP", trip.getId(), hasConflict ? "ASSIGN_WITH_OVERRIDE" : "ASSIGN",
                "Menugaskan " + trip.getTripCode() + " ke supir=" + targetDriverId + ", armada=" + targetVehicleId,
                null, trip);

        return tripService.toDetailDto(trip, false);
    }

    private List<ConflictItemDto> checkDriverConflicts(Driver driver,
                                                       OffsetDateTime candStart,
                                                       OffsetDateTime candEnd,
                                                       int candidateRestMin,
                                                       List<Trip> allOtherTrips,
                                                       Map<String, Route> routeMap) {
        List<ConflictItemDto> conflicts = new ArrayList<>();
        OffsetDateTime candEffectiveDriverEnd = candEnd.plusMinutes(candidateRestMin);

        if (!Boolean.TRUE.equals(driver.getIsActive()) || !"AVAILABLE".equalsIgnoreCase(driver.getAvailabilityStatus())) {
            conflicts.add(new ConflictItemDto(
                    "DRIVER_STATUS_UNAVAILABLE",
                    null,
                    candStart,
                    candEnd,
                    "Supir " + driver.getFullName() + " sedang berstatus " + driver.getAvailabilityStatus()
                            + (driver.getNotes() != null ? " (" + driver.getNotes() + ")" : "")
            ));
        }

        List<DriverUnavailabilityWindow> windows = unavailabilityMapper.selectList(
                new LambdaQueryWrapper<DriverUnavailabilityWindow>()
                        .eq(DriverUnavailabilityWindow::getDriverId, driver.getId()));
        for (DriverUnavailabilityWindow w : windows) {
            if (overlaps(candStart, candEffectiveDriverEnd, w.getStartAt(), w.getEndAt())) {
                conflicts.add(new ConflictItemDto(
                        "DRIVER_UNAVAILABILITY_WINDOW",
                        null,
                        w.getStartAt(),
                        w.getEndAt(),
                        "Supir " + driver.getFullName() + " memiliki jadwal izin (" + w.getStatusType() + ") pada "
                                + fmtWindow(w.getStartAt(), w.getEndAt())
                                + (w.getReason() != null ? ": " + w.getReason() : "")
                ));
            }
        }

        for (Trip other : allOtherTrips) {
            if (!driver.getId().equals(other.getDriverId())) {
                continue;
            }
            Route otherRoute = other.getRouteId() != null ? routeMap.get(other.getRouteId()) : null;
            int otherRestMin = (otherRoute != null && otherRoute.getRequiredRestAfterMin() != null)
                    ? otherRoute.getRequiredRestAfterMin() : 0;
            OffsetDateTime otherEffectiveEnd = other.getReservedEndAt().plusMinutes(otherRestMin);

            if (overlaps(candStart, candEffectiveDriverEnd, other.getReservedStartAt(), otherEffectiveEnd)) {
                boolean directTripOverlap = overlaps(candStart, candEnd, other.getReservedStartAt(), other.getReservedEndAt());
                String type = directTripOverlap ? "DRIVER_TRIP_OVERLAP" : "DRIVER_REST_BUFFER_OVERLAP";
                String explanation = directTripOverlap
                        ? "Supir " + driver.getFullName() + " bentrok dengan perjalanan " + other.getTripCode()
                        + " (" + fmtWindow(other.getReservedStartAt(), other.getReservedEndAt()) + " WIB termasuk buffer)."
                        : "Supir " + driver.getFullName() + " masih dalam jeda istirahat wajib pasca perjalanan jarak jauh "
                        + other.getTripCode() + " hingga "
                        + otherEffectiveEnd.atZoneSameInstant(IdGenerator.WIB).format(WINDOW_FMT) + " WIB.";
                conflicts.add(new ConflictItemDto(type, other.getTripCode(), other.getReservedStartAt(), otherEffectiveEnd, explanation));
            }
        }

        return conflicts;
    }

    private List<ConflictItemDto> checkVehicleConflicts(Vehicle vehicle,
                                                        OffsetDateTime candStart,
                                                        OffsetDateTime candEnd,
                                                        int pax,
                                                        int luggage,
                                                        List<Trip> allOtherTrips) {
        List<ConflictItemDto> conflicts = new ArrayList<>();

        if (!"ACTIVE".equalsIgnoreCase(vehicle.getStatus())) {
            conflicts.add(new ConflictItemDto(
                    "VEHICLE_STATUS_UNAVAILABLE",
                    null,
                    candStart,
                    candEnd,
                    "Armada " + vehicle.getRegistrationNumber() + " (" + vehicle.getNameModel() + ") berstatus "
                            + vehicle.getStatus()
            ));
        }

        if (vehicle.getPassengerCapacity() != null && vehicle.getPassengerCapacity() < pax) {
            conflicts.add(new ConflictItemDto(
                    "VEHICLE_INSUFFICIENT_CAPACITY",
                    null,
                    candStart,
                    candEnd,
                    "Kapasitas penumpang " + vehicle.getNameModel() + " (" + vehicle.getPassengerCapacity()
                            + " kursi) tidak mencukupi untuk " + pax + " penumpang."
            ));
        } else if (vehicle.getLuggageCapacityBags() != null && vehicle.getLuggageCapacityBags() < luggage) {
            conflicts.add(new ConflictItemDto(
                    "VEHICLE_INSUFFICIENT_CAPACITY",
                    null,
                    candStart,
                    candEnd,
                    "Kapasitas bagasi " + vehicle.getNameModel() + " (" + vehicle.getLuggageCapacityBags()
                            + " koper) kurang untuk " + luggage + " bagasi."
            ));
        }

        LocalDate candDate = candStart.atZoneSameInstant(IdGenerator.WIB).toLocalDate();
        List<VehicleMaintenanceLog> maintLogs = maintenanceLogMapper.selectList(
                new LambdaQueryWrapper<VehicleMaintenanceLog>().eq(VehicleMaintenanceLog::getVehicleId, vehicle.getId()));
        for (VehicleMaintenanceLog m : maintLogs) {
            if (m.getStartAt() != null && m.getEndAt() != null) {
                if (overlaps(candStart, candEnd, m.getStartAt(), m.getEndAt())) {
                    conflicts.add(new ConflictItemDto(
                            "VEHICLE_MAINTENANCE_WINDOW",
                            null,
                            m.getStartAt(),
                            m.getEndAt(),
                            "Armada " + vehicle.getRegistrationNumber() + " dijadwalkan servis (" + m.getMaintenanceType()
                                    + ") pada " + fmtWindow(m.getStartAt(), m.getEndAt())
                    ));
                }
            } else if (m.getServiceDate() != null && m.getServiceDate().equals(candDate)
                    && m.getDowntimeHours() != null && m.getDowntimeHours().signum() > 0) {
                conflicts.add(new ConflictItemDto(
                        "VEHICLE_MAINTENANCE_WINDOW",
                        null,
                        candStart,
                        candEnd,
                        "Armada " + vehicle.getRegistrationNumber() + " memiliki servis " + m.getMaintenanceType()
                                + " (" + m.getDowntimeHours() + " jam downtime) pada tanggal tersebut."
                ));
            }
        }

        for (Trip other : allOtherTrips) {
            if (!vehicle.getId().equals(other.getVehicleId())) {
                continue;
            }
            if (overlaps(candStart, candEnd, other.getReservedStartAt(), other.getReservedEndAt())) {
                conflicts.add(new ConflictItemDto(
                        "VEHICLE_TRIP_OVERLAP",
                        other.getTripCode(),
                        other.getReservedStartAt(),
                        other.getReservedEndAt(),
                        "Armada " + vehicle.getRegistrationNumber() + " sedang digunakan pada perjalanan "
                                + other.getTripCode() + " (" + fmtWindow(other.getReservedStartAt(), other.getReservedEndAt()) + " WIB)."
                ));
            }
        }

        return conflicts;
    }

    private List<DriverRecommendationDto> buildDriverRecommendations(OffsetDateTime candStart,
                                                                     OffsetDateTime candEnd,
                                                                     int candidateRestMin,
                                                                     LocalDate targetDay,
                                                                     List<Trip> allOtherTrips,
                                                                     Map<String, Route> routeMap) {
        OffsetDateTime dayStart = targetDay.atStartOfDay(IdGenerator.WIB).toOffsetDateTime();
        OffsetDateTime dayEnd = targetDay.plusDays(1).atStartOfDay(IdGenerator.WIB).toOffsetDateTime();
        double candReservedHours = Math.max(0.0, Duration.between(candStart, candEnd).toMinutes() / 60.0);

        List<Driver> drivers = driverMapper.selectList(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getIsActive, true)
                .orderByAsc(Driver::getDriverCode));

        List<DriverRecommendationDto> list = new ArrayList<>();
        for (Driver d : drivers) {
            List<ConflictItemDto> conflicts = checkDriverConflicts(
                    d, candStart, candEnd, candidateRestMin, allOtherTrips, routeMap);
            boolean conflictFree = conflicts.isEmpty();

            long reservedMins = 0;
            long activeMins = 0;
            for (Trip t : allOtherTrips) {
                if (!d.getId().equals(t.getDriverId())) {
                    continue;
                }
                if (overlaps(t.getReservedStartAt(), t.getReservedEndAt(), dayStart, dayEnd)) {
                    OffsetDateTime s = t.getReservedStartAt().isBefore(dayStart) ? dayStart : t.getReservedStartAt();
                    OffsetDateTime e = t.getReservedEndAt().isAfter(dayEnd) ? dayEnd : t.getReservedEndAt();
                    reservedMins += Math.max(0L, Duration.between(s, e).toMinutes());
                    int act = t.getActualDurationMin() != null ? t.getActualDurationMin()
                            : (t.getEstimatedDurationMin() != null ? t.getEstimatedDurationMin() : 60);
                    activeMins += act;
                }
            }

            BigDecimal reservedHrs = BigDecimal.valueOf(reservedMins / 60.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal activeHrs = BigDecimal.valueOf(activeMins / 60.0).setScale(2, RoundingMode.HALF_UP);
            double maxDaily = d.getMaxDailyHours() != null ? d.getMaxDailyHours().doubleValue() : 10.0;
            boolean withinDailyLimit = (reservedHrs.doubleValue() + candReservedHours) <= maxDaily;
            boolean recommended = conflictFree && withinDailyLimit;

            String explanation;
            if (!conflictFree) {
                explanation = conflicts.get(0).explanation();
            } else if (!withinDailyLimit) {
                explanation = "Bebas bentrok, namun total alokasi harian akan melebihi batas "
                        + d.getMaxDailyHours() + " jam (saat ini " + reservedHrs + " jam).";
            } else {
                explanation = "Direkomendasikan: tersedia & bebas bentrok. Beban kerja hari tersebut "
                        + reservedHrs + " jam teralokasi (" + activeHrs + " jam aktif) dari batas " + d.getMaxDailyHours() + " jam.";
            }

            list.add(new DriverRecommendationDto(
                    d.getId(),
                    d.getFullName(),
                    d.getAvailabilityStatus(),
                    conflictFree,
                    reservedHrs,
                    activeHrs,
                    recommended,
                    explanation
            ));
        }

        list.sort(Comparator
                .comparing(DriverRecommendationDto::recommended, Comparator.reverseOrder())
                .thenComparing(DriverRecommendationDto::conflictFree, Comparator.reverseOrder())
                .thenComparing(DriverRecommendationDto::reservedHoursTargetDay)
                .thenComparing(DriverRecommendationDto::driverName));
        return list;
    }

    private List<VehicleRecommendationDto> buildVehicleRecommendations(OffsetDateTime candStart,
                                                                       OffsetDateTime candEnd,
                                                                       int pax,
                                                                       int luggage,
                                                                       String tripType,
                                                                       String preferredVehicleType,
                                                                       List<Trip> allOtherTrips) {
        List<Vehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>()
                .orderByAsc(Vehicle::getRegistrationNumber));

        List<VehicleRecommendationDto> list = new ArrayList<>();
        for (Vehicle v : vehicles) {
            List<ConflictItemDto> conflicts = checkVehicleConflicts(v, candStart, candEnd, pax, luggage, allOtherTrips);
            boolean conflictFree = conflicts.isEmpty();
            boolean activeStatus = "ACTIVE".equalsIgnoreCase(v.getStatus());
            boolean capacityOk = v.getPassengerCapacity() >= pax && v.getLuggageCapacityBags() >= luggage;
            boolean suitable = conflictFree && activeStatus && capacityOk;

            int score = 0;
            List<String> reasons = new ArrayList<>();
            if (suitable) {
                score = 50;
                if (pax <= 6 && v.getPassengerCapacity() <= 7) {
                    score += 20;
                    reasons.add("kapasitas efisien (" + v.getPassengerCapacity() + " kursi)");
                } else if (pax > 6 && v.getPassengerCapacity() >= pax) {
                    score += 25;
                    reasons.add("kapasitas rombongan memadai (" + v.getPassengerCapacity() + " kursi)");
                } else {
                    score += 5;
                }

                if (v.getLuggageCapacityBags() >= luggage + 1) {
                    score += 10;
                    reasons.add("ruang bagasi lega (" + v.getLuggageCapacityBags() + " koper)");
                }

                String tags = v.getSuitabilityTags() != null ? v.getSuitabilityTags().toUpperCase() : "";
                if ((tripType.contains("AIRPORT") && tags.contains("AIRPORT"))
                        || (tripType.contains("INTERCITY") && tags.contains("INTERCITY"))
                        || (tripType.contains("CHARTER") && tags.contains("CHARTER"))
                        || (tripType.contains("LOCAL") && tags.contains("LOCAL"))
                        || (pax >= 7 && tags.contains("GROUP"))) {
                    score += 10;
                    reasons.add("cocok untuk tipe perjalanan " + tripType);
                }

                if (preferredVehicleType != null && preferredVehicleType.equalsIgnoreCase(v.getVehicleType())) {
                    score += 15;
                    reasons.add("sesuai preferensi pelanggan (" + preferredVehicleType + ")");
                }
                score = Math.min(100, score);
            }

            String explanation = suitable
                    ? "Unit layak (skor " + score + "): " + String.join(", ", reasons) + "."
                    : (!conflicts.isEmpty() ? conflicts.get(0).explanation() : "Unit tidak memenuhi syarat ketersediaan/kapasitas.");

            list.add(new VehicleRecommendationDto(
                    v.getId(),
                    v.getRegistrationNumber(),
                    v.getNameModel(),
                    v.getVehicleType(),
                    v.getPassengerCapacity(),
                    v.getLuggageCapacityBags(),
                    conflictFree,
                    suitable,
                    score,
                    explanation
            ));
        }

        list.sort(Comparator
                .comparing(VehicleRecommendationDto::suitable, Comparator.reverseOrder())
                .thenComparing(Comparator.comparingInt(VehicleRecommendationDto::suitabilityScore).reversed())
                .thenComparing(VehicleRecommendationDto::registrationNumber));
        return list;
    }

    private boolean overlaps(OffsetDateTime s1, OffsetDateTime e1, OffsetDateTime s2, OffsetDateTime e2) {
        if (s1 == null || e1 == null || s2 == null || e2 == null) {
            return false;
        }
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private String fmtWindow(OffsetDateTime start, OffsetDateTime end) {
        return start.atZoneSameInstant(IdGenerator.WIB).format(WINDOW_FMT)
                + " - " + end.atZoneSameInstant(IdGenerator.WIB).format(WINDOW_FMT);
    }

    private String buildConflictSummaryMessage(ConflictSimulationResult sim) {
        List<String> msgs = new ArrayList<>();
        sim.driverConflicts().forEach(c -> msgs.add(c.explanation()));
        sim.vehicleConflicts().forEach(c -> msgs.add(c.explanation()));
        return msgs.isEmpty()
                ? "Terdapat bentrok penugasan supir/armada."
                : String.join(" | ", msgs);
    }
}
