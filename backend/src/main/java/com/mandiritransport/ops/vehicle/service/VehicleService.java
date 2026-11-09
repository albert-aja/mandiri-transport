package com.mandiritransport.ops.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.MaintenanceLogRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.MaintenanceScheduleRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.RouteRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleDetailDto;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleDocumentRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleExpenseRequest;
import com.mandiritransport.ops.vehicle.dto.VehicleDtos.VehicleRequest;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.entity.Vehicle;
import com.mandiritransport.ops.vehicle.entity.VehicleDocument;
import com.mandiritransport.ops.vehicle.entity.VehicleExpense;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceLog;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceSchedule;
import com.mandiritransport.ops.vehicle.mapper.RouteMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleDocumentMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleExpenseMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMaintenanceLogMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMaintenanceScheduleMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleMapper vehicleMapper;
    private final VehicleDocumentMapper documentMapper;
    private final VehicleMaintenanceScheduleMapper scheduleMapper;
    private final VehicleMaintenanceLogMapper logMapper;
    private final VehicleExpenseMapper expenseMapper;
    private final RouteMapper routeMapper;
    private final AuditService auditService;

    public VehicleService(VehicleMapper vehicleMapper,
                          VehicleDocumentMapper documentMapper,
                          VehicleMaintenanceScheduleMapper scheduleMapper,
                          VehicleMaintenanceLogMapper logMapper,
                          VehicleExpenseMapper expenseMapper,
                          RouteMapper routeMapper,
                          AuditService auditService) {
        this.vehicleMapper = vehicleMapper;
        this.documentMapper = documentMapper;
        this.scheduleMapper = scheduleMapper;
        this.logMapper = logMapper;
        this.expenseMapper = expenseMapper;
        this.routeMapper = routeMapper;
        this.auditService = auditService;
    }

    public List<VehicleDetailDto> listVehicles() {
        return vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>()
                        .orderByAsc(Vehicle::getRegistrationNumber))
                .stream()
                .map(this::toDetailDto)
                .toList();
    }

    public VehicleDetailDto getVehicle(String id) {
        Vehicle v = vehicleMapper.selectById(id);
        if (v == null) {
            throw BusinessException.notFound("Kendaraan tidak ditemukan.");
        }
        return toDetailDto(v);
    }

    @Transactional
    public VehicleDetailDto createVehicle(VehicleRequest req) {
        SecurityUtils.requireOwner();
        Vehicle v = new Vehicle();
        v.setId(IdGenerator.newId());
        applyVehicleFields(v, req);
        v.setCreatedAt(OffsetDateTime.now());
        v.setUpdatedAt(OffsetDateTime.now());
        v.setVersion(0L);
        vehicleMapper.insert(v);
        auditService.log("VEHICLE", v.getId(), "CREATE", "Menambah armada baru: " + v.getRegistrationNumber(), null, v);
        return toDetailDto(v);
    }

    @Transactional
    public VehicleDetailDto updateVehicle(String id, VehicleRequest req) {
        SecurityUtils.requireOwner();
        Vehicle v = vehicleMapper.selectById(id);
        if (v == null) {
            throw BusinessException.notFound("Kendaraan tidak ditemukan.");
        }
        applyVehicleFields(v, req);
        v.setUpdatedAt(OffsetDateTime.now());
        vehicleMapper.updateById(v);
        auditService.log("VEHICLE", v.getId(), "UPDATE", "Memperbarui armada: " + v.getRegistrationNumber(), null, v);
        return toDetailDto(v);
    }

    @Transactional
    public VehicleDocument addDocument(String vehicleId, VehicleDocumentRequest req) {
        SecurityUtils.requireOwner();
        requireVehicleExists(vehicleId);
        VehicleDocument doc = new VehicleDocument();
        doc.setId(IdGenerator.newId());
        doc.setVehicleId(vehicleId);
        doc.setDocType(req.docType().trim().toUpperCase());
        doc.setDocNumber(req.docNumber());
        doc.setIssueDate(req.issueDate());
        doc.setExpiryDate(req.expiryDate());
        doc.setReminderDaysBefore(req.reminderDaysBefore() != null ? req.reminderDaysBefore() : 30);
        doc.setStoragePath(req.storagePath());
        doc.setNotes(req.notes());
        doc.setCreatedAt(OffsetDateTime.now());
        doc.setUpdatedAt(OffsetDateTime.now());
        documentMapper.insert(doc);
        auditService.log("VEHICLE_DOCUMENT", doc.getId(), "CREATE", "Menambah dokumen kendaraan " + doc.getDocType(), null, doc);
        return doc;
    }

    @Transactional
    public VehicleMaintenanceSchedule addMaintenanceSchedule(String vehicleId, MaintenanceScheduleRequest req) {
        SecurityUtils.requireOwner();
        requireVehicleExists(vehicleId);
        VehicleMaintenanceSchedule s = new VehicleMaintenanceSchedule();
        s.setId(IdGenerator.newId());
        s.setVehicleId(vehicleId);
        s.setMaintenanceType(req.maintenanceType().trim().toUpperCase());
        s.setIntervalKm(req.intervalKm());
        s.setIntervalMonths(req.intervalMonths());
        s.setLastDoneOdometerKm(req.lastDoneOdometerKm());
        s.setLastDoneDate(req.lastDoneDate());
        s.setNextDueOdometerKm(req.nextDueOdometerKm());
        s.setNextDueDate(req.nextDueDate());
        s.setEstimatedCost(req.estimatedCost());
        s.setIsActive(true);
        s.setCreatedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        scheduleMapper.insert(s);
        return s;
    }

    @Transactional
    public VehicleMaintenanceLog addMaintenanceLog(String vehicleId, MaintenanceLogRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        Vehicle v = requireVehicleExists(vehicleId);

        String mType = req.maintenanceType().trim().toUpperCase();
        VehicleMaintenanceLog mLog = new VehicleMaintenanceLog();
        mLog.setId(IdGenerator.newId());
        mLog.setVehicleId(vehicleId);
        mLog.setMaintenanceType(mType);
        mLog.setServiceDate(req.serviceDate());
        mLog.setOdometerKm(req.odometerKm());
        mLog.setVendorName(req.vendorName());
        mLog.setCostAmount(req.costAmount());
        mLog.setDowntimeHours(req.downtimeHours() != null ? req.downtimeHours() : BigDecimal.ZERO);
        mLog.setStartAt(req.startAt());
        mLog.setEndAt(req.endAt());
        mLog.setDescription(req.description());
        mLog.setReceiptPath(req.receiptPath());
        mLog.setRecordedBy(owner.userId());
        mLog.setCreatedAt(OffsetDateTime.now());
        mLog.setUpdatedAt(OffsetDateTime.now());
        logMapper.insert(mLog);

        // Update vehicle odometer if higher
        if (req.odometerKm() != null && (v.getCurrentOdometerKm() == null
                || req.odometerKm().compareTo(v.getCurrentOdometerKm()) > 0)) {
            v.setCurrentOdometerKm(req.odometerKm());
            v.setUpdatedAt(OffsetDateTime.now());
            vehicleMapper.updateById(v);
        }

        // Update matching maintenance schedule next due date / km
        List<VehicleMaintenanceSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<VehicleMaintenanceSchedule>()
                        .eq(VehicleMaintenanceSchedule::getVehicleId, vehicleId)
                        .eq(VehicleMaintenanceSchedule::getMaintenanceType, mType)
                        .eq(VehicleMaintenanceSchedule::getIsActive, true));
        for (VehicleMaintenanceSchedule sch : schedules) {
            sch.setLastDoneDate(req.serviceDate());
            if (req.odometerKm() != null) {
                sch.setLastDoneOdometerKm(req.odometerKm());
                if (sch.getIntervalKm() != null) {
                    sch.setNextDueOdometerKm(req.odometerKm().add(sch.getIntervalKm()));
                }
            }
            if (sch.getIntervalMonths() != null) {
                sch.setNextDueDate(req.serviceDate().plusMonths(sch.getIntervalMonths()));
            }
            sch.setUpdatedAt(OffsetDateTime.now());
            scheduleMapper.updateById(sch);
        }

        // Atomically record non-trip vehicle expense if requested (default true)
        boolean recordExpense = req.recordAsVehicleExpense() == null || req.recordAsVehicleExpense();
        if (recordExpense && req.costAmount() != null && req.costAmount().signum() > 0) {
            VehicleExpense exp = new VehicleExpense();
            exp.setId(IdGenerator.newId());
            exp.setVehicleId(vehicleId);
            exp.setMaintenanceLogId(mLog.getId());
            exp.setCategory(mapMaintenanceTypeToExpenseCategory(mType));
            exp.setAmount(req.costAmount());
            exp.setExpenseDate(req.serviceDate());
            exp.setDescription(req.description() != null ? req.description() : ("Biaya servis " + mType));
            exp.setReceiptPath(req.receiptPath());
            exp.setRecordedBy(owner.userId());
            exp.setIsVoided(false);
            exp.setCreatedAt(OffsetDateTime.now());
            exp.setUpdatedAt(OffsetDateTime.now());
            expenseMapper.insert(exp);
        }

        auditService.log("VEHICLE_MAINTENANCE_LOG", mLog.getId(), "CREATE",
                "Mencatat perawatan " + mType + " untuk " + v.getRegistrationNumber(), null, mLog);
        return mLog;
    }

    @Transactional
    public VehicleExpense addVehicleExpense(String vehicleId, VehicleExpenseRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        Vehicle v = requireVehicleExists(vehicleId);
        VehicleExpense exp = new VehicleExpense();
        exp.setId(IdGenerator.newId());
        exp.setVehicleId(vehicleId);
        exp.setCategory(req.category().trim().toUpperCase());
        exp.setAmount(req.amount());
        exp.setExpenseDate(req.expenseDate());
        exp.setDescription(req.description());
        exp.setReceiptPath(req.receiptPath());
        exp.setRecordedBy(owner.userId());
        exp.setIsVoided(false);
        exp.setCreatedAt(OffsetDateTime.now());
        exp.setUpdatedAt(OffsetDateTime.now());
        expenseMapper.insert(exp);
        auditService.log("VEHICLE_EXPENSE", exp.getId(), "CREATE",
                "Mencatat biaya armada " + v.getRegistrationNumber() + ": " + req.amount(), null, exp);
        return exp;
    }

    public List<VehicleExpense> listVehicleExpenses(String vehicleId) {
        return expenseMapper.selectList(new LambdaQueryWrapper<VehicleExpense>()
                .eq(VehicleExpense::getVehicleId, vehicleId)
                .eq(VehicleExpense::getIsVoided, false)
                .orderByDesc(VehicleExpense::getExpenseDate));
    }

    public List<Route> listRoutes() {
        return routeMapper.selectList(new LambdaQueryWrapper<Route>()
                .eq(Route::getIsActive, true)
                .orderByAsc(Route::getName));
    }

    @Transactional
    public Route createRoute(RouteRequest req) {
        SecurityUtils.requireOwner();
        Route r = new Route();
        r.setId(IdGenerator.newId());
        r.setCode(req.code().trim().toUpperCase());
        r.setName(req.name().trim());
        r.setOriginName(req.originName().trim());
        r.setDestinationName(req.destinationName().trim());
        r.setTypicalDistanceKm(req.typicalDistanceKm());
        r.setDefaultDurationMin(req.defaultDurationMin());
        r.setDefaultPrepBufferMin(req.defaultPrepBufferMin() != null ? req.defaultPrepBufferMin() : 15);
        r.setDefaultArrivalBufferMin(req.defaultArrivalBufferMin() != null ? req.defaultArrivalBufferMin() : 0);
        r.setDefaultRepositionBufferMin(req.defaultRepositionBufferMin() != null ? req.defaultRepositionBufferMin() : 15);
        r.setIsLongDistance(Boolean.TRUE.equals(req.isLongDistance()));
        r.setRequiredRestAfterMin(req.requiredRestAfterMin() != null ? req.requiredRestAfterMin() : 0);
        r.setIsActive(true);
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());
        routeMapper.insert(r);
        auditService.log("ROUTE", r.getId(), "CREATE", "Menambah rute baru: " + r.getName(), null, r);
        return r;
    }

    private Vehicle requireVehicleExists(String vehicleId) {
        Vehicle v = vehicleMapper.selectById(vehicleId);
        if (v == null) {
            throw BusinessException.notFound("Kendaraan tidak ditemukan.");
        }
        return v;
    }

    private void applyVehicleFields(Vehicle v, VehicleRequest req) {
        v.setRegistrationNumber(req.registrationNumber().trim().toUpperCase());
        v.setNameModel(req.nameModel().trim());
        v.setBrand(req.brand());
        v.setManufactureYear(req.manufactureYear());
        v.setVehicleType(req.vehicleType().trim().toUpperCase());
        v.setPassengerCapacity(req.passengerCapacity());
        v.setLuggageCapacityBags(req.luggageCapacityBags() != null ? req.luggageCapacityBags() : 4);
        v.setStatus(req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : "ACTIVE");
        v.setCurrentOdometerKm(req.currentOdometerKm() != null ? req.currentOdometerKm() : BigDecimal.ZERO);
        v.setFuelType(req.fuelType() != null ? req.fuelType() : "BENSIN");
        v.setSuitabilityTags(req.suitabilityTags());
        v.setNotes(req.notes());
    }

    private String mapMaintenanceTypeToExpenseCategory(String mType) {
        return switch (mType) {
            case "OIL_CHANGE" -> "OIL";
            case "TIRE_REPLACEMENT" -> "TIRE";
            case "BATTERY_REPLACEMENT" -> "BATTERY";
            case "REPAIR" -> "REPAIR";
            case "GENERAL_SERVICE" -> "SERVICE";
            default -> "OTHER";
        };
    }

    private VehicleDetailDto toDetailDto(Vehicle v) {
        List<VehicleDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<VehicleDocument>()
                        .eq(VehicleDocument::getVehicleId, v.getId())
                        .orderByAsc(VehicleDocument::getExpiryDate));
        List<VehicleMaintenanceSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<VehicleMaintenanceSchedule>()
                        .eq(VehicleMaintenanceSchedule::getVehicleId, v.getId())
                        .eq(VehicleMaintenanceSchedule::getIsActive, true));
        List<VehicleMaintenanceLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<VehicleMaintenanceLog>()
                        .eq(VehicleMaintenanceLog::getVehicleId, v.getId())
                        .orderByDesc(VehicleMaintenanceLog::getServiceDate)
                        .last("LIMIT 20"));
        List<VehicleExpense> expenses = expenseMapper.selectList(
                new LambdaQueryWrapper<VehicleExpense>()
                        .eq(VehicleExpense::getVehicleId, v.getId())
                        .eq(VehicleExpense::getIsVoided, false)
                        .orderByDesc(VehicleExpense::getExpenseDate)
                        .last("LIMIT 20"));

        return new VehicleDetailDto(
                v.getId(),
                v.getRegistrationNumber(),
                v.getNameModel(),
                v.getBrand(),
                v.getManufactureYear(),
                v.getVehicleType(),
                v.getPassengerCapacity(),
                v.getLuggageCapacityBags(),
                v.getStatus(),
                v.getCurrentOdometerKm(),
                v.getFuelType(),
                v.getSuitabilityTags(),
                v.getNotes(),
                docs,
                schedules,
                logs,
                expenses
        );
    }
}
