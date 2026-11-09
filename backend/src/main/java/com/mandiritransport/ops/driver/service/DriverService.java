package com.mandiritransport.ops.driver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.auth.entity.AppUser;
import com.mandiritransport.ops.auth.mapper.AppUserMapper;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.GeoAndLinkUtils;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.driver.dto.DriverDtos.CreateDriverRequest;
import com.mandiritransport.ops.driver.dto.DriverDtos.DriverDto;
import com.mandiritransport.ops.driver.dto.DriverDtos.UnavailabilityWindowRequest;
import com.mandiritransport.ops.driver.dto.DriverDtos.UpdateAvailabilityRequest;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.entity.DriverUnavailabilityWindow;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.driver.mapper.DriverUnavailabilityWindowMapper;
import com.mandiritransport.ops.finance.entity.TripPayment;
import com.mandiritransport.ops.finance.mapper.TripPaymentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DriverService {

    private static final Set<String> VALID_AVAILABILITY = Set.of(
            "AVAILABLE", "UNAVAILABLE", "ON_LEAVE", "TEMPORARILY_UNAVAILABLE"
    );

    private final DriverMapper driverMapper;
    private final DriverUnavailabilityWindowMapper windowMapper;
    private final AppUserMapper appUserMapper;
    private final TripPaymentMapper paymentMapper;
    private final AuditService auditService;

    public DriverService(DriverMapper driverMapper,
                         DriverUnavailabilityWindowMapper windowMapper,
                         AppUserMapper appUserMapper,
                         TripPaymentMapper paymentMapper,
                         AuditService auditService) {
        this.driverMapper = driverMapper;
        this.windowMapper = windowMapper;
        this.appUserMapper = appUserMapper;
        this.paymentMapper = paymentMapper;
        this.auditService = auditService;
    }

    public List<DriverDto> listDrivers() {
        SecurityUser current = SecurityUtils.currentUser();
        LambdaQueryWrapper<Driver> qw = new LambdaQueryWrapper<Driver>()
                .eq(Driver::getIsActive, true)
                .orderByAsc(Driver::getDriverCode);
        if (current.isDriver()) {
            qw.eq(Driver::getId, SecurityUtils.requireDriverId());
        }
        return driverMapper.selectList(qw).stream()
                .map(this::toDto)
                .toList();
    }

    public DriverDto getDriverDto(String driverId) {
        Driver d = driverMapper.selectById(driverId);
        if (d == null) {
            throw BusinessException.notFound("Supir tidak ditemukan.");
        }
        return toDto(d);
    }

    @Transactional
    public DriverDto createDriver(CreateDriverRequest req) {
        SecurityUtils.requireOwner();
        String status = req.availabilityStatus() != null && !req.availabilityStatus().isBlank()
                ? req.availabilityStatus().trim().toUpperCase() : "AVAILABLE";
        if (!VALID_AVAILABILITY.contains(status)) {
            throw BusinessException.badRequest("INVALID_STATUS", "Status ketersediaan supir tidak valid: " + status);
        }

        String userId = null;
        if (req.username() != null && !req.username().isBlank()) {
            String uname = req.username().trim().toLowerCase();
            AppUser existingUser = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                    .eq(AppUser::getUsername, uname)
                    .last("LIMIT 1"));
            if (existingUser != null) {
                throw BusinessException.badRequest("DUPLICATE_USERNAME", "Username sudah digunakan: " + uname);
            }
            AppUser newUser = new AppUser();
            newUser.setId(IdGenerator.newId());
            newUser.setUsername(uname);
            newUser.setFullName(req.fullName().trim());
            newUser.setPhone(req.phone().trim());
            newUser.setRole("DRIVER");
            newUser.setIsActive(true);
            newUser.setCreatedAt(OffsetDateTime.now());
            newUser.setUpdatedAt(OffsetDateTime.now());
            newUser.setVersion(0L);
            appUserMapper.insert(newUser);
            userId = newUser.getId();
        }

        Driver d = new Driver();
        d.setId(IdGenerator.newId());
        d.setUserId(userId);
        d.setDriverCode(IdGenerator.driverCode());
        d.setFullName(req.fullName().trim());
        d.setPhone(req.phone().trim());
        d.setLicenseNumber(req.licenseNumber());
        d.setLicenseExpiryDate(req.licenseExpiryDate());
        d.setAvailabilityStatus(status);
        d.setMaxDailyHours(req.maxDailyHours() != null ? req.maxDailyHours() : new BigDecimal("10.00"));
        d.setNotes(req.notes());
        d.setIsActive(true);
        d.setCreatedAt(OffsetDateTime.now());
        d.setUpdatedAt(OffsetDateTime.now());
        d.setVersion(0L);
        driverMapper.insert(d);
        auditService.log("DRIVER", d.getId(), "CREATE", "Menambah supir baru: " + d.getFullName(), null, d);
        return toDto(d);
    }

    @Transactional
    public DriverDto updateAvailability(String driverId, UpdateAvailabilityRequest req) {
        SecurityUser current = SecurityUtils.currentUser();
        if (current.isDriver() && !driverId.equals(current.driverId())) {
            throw BusinessException.forbidden("Supir hanya dapat memperbarui status ketersediaannya sendiri.");
        }
        Driver d = driverMapper.selectById(driverId);
        if (d == null) {
            throw BusinessException.notFound("Supir tidak ditemukan.");
        }
        String newStatus = req.availabilityStatus().trim().toUpperCase();
        if (!VALID_AVAILABILITY.contains(newStatus)) {
            throw BusinessException.badRequest("INVALID_STATUS", "Status ketersediaan tidak valid: " + newStatus);
        }
        String prev = d.getAvailabilityStatus();
        d.setAvailabilityStatus(newStatus);
        if (req.notes() != null) {
            d.setNotes(req.notes());
        }
        d.setUpdatedAt(OffsetDateTime.now());
        driverMapper.updateById(d);
        auditService.log("DRIVER", d.getId(), "UPDATE_AVAILABILITY",
                "Status supir " + d.getFullName() + ": " + prev + " -> " + newStatus, prev, newStatus);
        return toDto(d);
    }

    @Transactional
    public DriverUnavailabilityWindow addUnavailabilityWindow(String driverId, UnavailabilityWindowRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        Driver d = driverMapper.selectById(driverId);
        if (d == null) {
            throw BusinessException.notFound("Supir tidak ditemukan.");
        }
        if (!req.endAt().isAfter(req.startAt())) {
            throw BusinessException.badRequest("INVALID_WINDOW", "Waktu selesai harus setelah waktu mulai.");
        }
        DriverUnavailabilityWindow win = new DriverUnavailabilityWindow();
        win.setId(IdGenerator.newId());
        win.setDriverId(driverId);
        win.setStatusType(req.statusType() != null && !req.statusType().isBlank() ? req.statusType() : "ON_LEAVE");
        win.setReason(req.reason());
        win.setStartAt(req.startAt());
        win.setEndAt(req.endAt());
        win.setCreatedBy(owner.userId());
        win.setCreatedAt(OffsetDateTime.now());
        windowMapper.insert(win);
        auditService.log("DRIVER_UNAVAILABILITY", win.getId(), "CREATE",
                "Menambah jadwal izin/tidak tersedia untuk " + d.getFullName(), null, win);
        return win;
    }

    public List<DriverUnavailabilityWindow> listUnavailabilityWindows(String driverId) {
        return windowMapper.selectList(new LambdaQueryWrapper<DriverUnavailabilityWindow>()
                .eq(DriverUnavailabilityWindow::getDriverId, driverId)
                .orderByDesc(DriverUnavailabilityWindow::getStartAt));
    }

    public BigDecimal calculateDriverCashHeld(String driverId) {
        List<TripPayment> unsettled = paymentMapper.selectList(new LambdaQueryWrapper<TripPayment>()
                .eq(TripPayment::getHeldByDriverId, driverId)
                .eq(TripPayment::getCustodySettled, false)
                .eq(TripPayment::getIsVoided, false)
                .eq(TripPayment::getIsRefund, false));
        BigDecimal sum = BigDecimal.ZERO;
        for (TripPayment p : unsettled) {
            BigDecimal rem = p.getAmount().subtract(p.getHandedOverAmount() != null ? p.getHandedOverAmount() : BigDecimal.ZERO);
            if (rem.signum() > 0) {
                sum = sum.add(rem);
            }
        }
        return sum;
    }

    private DriverDto toDto(Driver d) {
        List<DriverUnavailabilityWindow> windows = listUnavailabilityWindows(d.getId());
        return new DriverDto(
                d.getId(),
                d.getUserId(),
                d.getDriverCode(),
                d.getFullName(),
                d.getPhone(),
                GeoAndLinkUtils.buildWhatsappUrl(d.getPhone(), null),
                d.getLicenseNumber(),
                d.getLicenseExpiryDate(),
                d.getAvailabilityStatus(),
                d.getMaxDailyHours(),
                d.getNotes(),
                calculateDriverCashHeld(d.getId()),
                windows
        );
    }
}
