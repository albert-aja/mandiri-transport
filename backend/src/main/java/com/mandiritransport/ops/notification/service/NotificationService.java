package com.mandiritransport.ops.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.auth.entity.AppUser;
import com.mandiritransport.ops.auth.mapper.AppUserMapper;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.GeoAndLinkUtils;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.finance.entity.TripPayment;
import com.mandiritransport.ops.finance.mapper.TripPaymentMapper;
import com.mandiritransport.ops.notification.dto.NotificationDtos.FcmDeviceItemDto;
import com.mandiritransport.ops.notification.dto.NotificationDtos.FcmStatusDto;
import com.mandiritransport.ops.notification.dto.NotificationDtos.NotificationListDto;
import com.mandiritransport.ops.notification.dto.NotificationDtos.TestPushRequest;
import com.mandiritransport.ops.notification.dto.NotificationDtos.TestPushResponse;
import com.mandiritransport.ops.notification.dto.NotificationDtos.UpdateNotificationSettingRequest;
import com.mandiritransport.ops.notification.entity.Notification;
import com.mandiritransport.ops.notification.entity.NotificationSetting;
import com.mandiritransport.ops.notification.entity.UserFcmDevice;
import com.mandiritransport.ops.notification.mapper.NotificationMapper;
import com.mandiritransport.ops.notification.mapper.NotificationSettingMapper;
import com.mandiritransport.ops.notification.mapper.UserFcmDeviceMapper;
import com.mandiritransport.ops.notification.service.PushNotificationGateway.GatewayDiagnostics;
import com.mandiritransport.ops.notification.service.PushNotificationGateway.PushDeliveryResult;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.vehicle.entity.Vehicle;
import com.mandiritransport.ops.vehicle.entity.VehicleDocument;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceSchedule;
import com.mandiritransport.ops.vehicle.mapper.VehicleDocumentMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMaintenanceScheduleMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class NotificationService {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.forLanguageTag("id-ID"));

    private final NotificationMapper notificationMapper;
    private final NotificationSettingMapper settingMapper;
    private final UserFcmDeviceMapper fcmDeviceMapper;
    private final AppUserMapper appUserMapper;
    private final DriverMapper driverMapper;
    private final TripMapper tripMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleDocumentMapper vehicleDocumentMapper;
    private final VehicleMaintenanceScheduleMapper maintenanceScheduleMapper;
    private final TripPaymentMapper tripPaymentMapper;
    private final PushNotificationGateway pushGateway;

    public NotificationService(NotificationMapper notificationMapper,
                               NotificationSettingMapper settingMapper,
                               UserFcmDeviceMapper fcmDeviceMapper,
                               AppUserMapper appUserMapper,
                               DriverMapper driverMapper,
                               TripMapper tripMapper,
                               VehicleMapper vehicleMapper,
                               VehicleDocumentMapper vehicleDocumentMapper,
                               VehicleMaintenanceScheduleMapper maintenanceScheduleMapper,
                               TripPaymentMapper tripPaymentMapper,
                               PushNotificationGateway pushGateway) {
        this.notificationMapper = notificationMapper;
        this.settingMapper = settingMapper;
        this.fcmDeviceMapper = fcmDeviceMapper;
        this.appUserMapper = appUserMapper;
        this.driverMapper = driverMapper;
        this.tripMapper = tripMapper;
        this.vehicleMapper = vehicleMapper;
        this.vehicleDocumentMapper = vehicleDocumentMapper;
        this.maintenanceScheduleMapper = maintenanceScheduleMapper;
        this.tripPaymentMapper = tripPaymentMapper;
        this.pushGateway = pushGateway;
    }

    @Transactional
    public String registerDeviceToken(String userId, String rawToken, String platform, String deviceLabel) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("Akun pengguna tidak ditemukan.");
        }
        String token = rawToken.trim();
        String normPlatform = platform != null && !platform.isBlank()
                ? platform.trim().toUpperCase()
                : "ANDROID";
        String label = deviceLabel != null && !deviceLabel.isBlank()
                ? deviceLabel.trim()
                : normPlatform + " Device";

        OffsetDateTime now = OffsetDateTime.now();
        UserFcmDevice existing = fcmDeviceMapper.selectOne(new LambdaQueryWrapper<UserFcmDevice>()
                .eq(UserFcmDevice::getFcmToken, token)
                .last("LIMIT 1"));

        String deviceId;
        if (existing != null) {
            String previousUserId = existing.getUserId();
            existing.setUserId(userId);
            existing.setPlatform(normPlatform);
            existing.setDeviceLabel(label);
            existing.setIsActive(true);
            existing.setLastRegisteredAt(now);
            existing.setLastError(null);
            existing.setUpdatedAt(now);
            fcmDeviceMapper.updateById(existing);
            deviceId = existing.getId();

            // Prevent shared fleet phone from leaking push to previous logged-in user
            if (previousUserId != null && !previousUserId.equals(userId)) {
                AppUser prevUser = appUserMapper.selectById(previousUserId);
                if (prevUser != null && token.equals(prevUser.getFcmToken())) {
                    prevUser.setFcmToken(null);
                    prevUser.setUpdatedAt(now);
                    appUserMapper.updateById(prevUser);
                }
            }
        } else {
            UserFcmDevice created = new UserFcmDevice();
            created.setId(IdGenerator.newId());
            created.setUserId(userId);
            created.setFcmToken(token);
            created.setPlatform(normPlatform);
            created.setDeviceLabel(label);
            created.setIsActive(true);
            created.setLastRegisteredAt(now);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            fcmDeviceMapper.insert(created);
            deviceId = created.getId();
        }

        user.setFcmToken(token);
        user.setUpdatedAt(now);
        appUserMapper.updateById(user);
        return deviceId;
    }

    @Transactional
    public boolean unregisterDeviceToken(String userId, String rawToken) {
        String token = rawToken.trim();
        OffsetDateTime now = OffsetDateTime.now();
        UserFcmDevice existing = fcmDeviceMapper.selectOne(new LambdaQueryWrapper<UserFcmDevice>()
                .eq(UserFcmDevice::getFcmToken, token)
                .last("LIMIT 1"));
        boolean deactivated = false;
        if (existing != null && (userId.equals(existing.getUserId()) || SecurityUtils.currentUser().isOwner())) {
            existing.setIsActive(false);
            existing.setUpdatedAt(now);
            fcmDeviceMapper.updateById(existing);
            deactivated = true;
        }
        AppUser user = appUserMapper.selectById(userId);
        if (user != null && token.equals(user.getFcmToken())) {
            user.setFcmToken(null);
            user.setUpdatedAt(now);
            appUserMapper.updateById(user);
            deactivated = true;
        }
        return deactivated;
    }

    public FcmStatusDto getFcmStatus() {
        SecurityUser current = SecurityUtils.currentUser();
        GatewayDiagnostics diag = pushGateway.getDiagnostics();

        List<UserFcmDevice> myDevices = fcmDeviceMapper.selectList(new LambdaQueryWrapper<UserFcmDevice>()
                .eq(UserFcmDevice::getUserId, current.userId())
                .orderByDesc(UserFcmDevice::getLastRegisteredAt));

        int myActive = (int) myDevices.stream().filter(d -> Boolean.TRUE.equals(d.getIsActive())).count();
        int fleetActive = current.isOwner()
                ? Math.toIntExact(fcmDeviceMapper.selectCount(new LambdaQueryWrapper<UserFcmDevice>()
                        .eq(UserFcmDevice::getIsActive, true)))
                : myActive;

        List<FcmDeviceItemDto> items = myDevices.stream()
                .map(d -> new FcmDeviceItemDto(
                        d.getId(),
                        d.getPlatform(),
                        d.getDeviceLabel(),
                        maskToken(d.getFcmToken()),
                        d.getIsActive(),
                        d.getLastRegisteredAt() != null ? d.getLastRegisteredAt().toString() : null,
                        d.getLastPushAt() != null ? d.getLastPushAt().toString() : null
                ))
                .toList();

        return new FcmStatusDto(
                diag.fcmEnabled(),
                diag.liveCredentialsReady(),
                diag.credentialSource(),
                diag.projectId(),
                diag.clientEmailMasked(),
                diag.androidChannelId(),
                myActive,
                fleetActive,
                items,
                diag.statusSummary()
        );
    }

    @Transactional
    public TestPushResponse sendTestPush(TestPushRequest req) {
        SecurityUser current = SecurityUtils.currentUser();
        String targetUserId = current.userId();
        if (req != null && req.targetUserId() != null && !req.targetUserId().isBlank()) {
            if (!current.isOwner() && !req.targetUserId().equals(current.userId())) {
                throw BusinessException.forbidden("Hanya pemilik yang dapat mengirim uji push ke akun lain.");
            }
            targetUserId = req.targetUserId().trim();
        }

        AppUser targetUser = appUserMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw BusinessException.notFound("Akun tujuan uji push tidak ditemukan.");
        }

        String title = req != null && req.title() != null && !req.title().isBlank()
                ? req.title().trim()
                : "Uji Push FCM Mandiri Transport";
        String body = req != null && req.body() != null && !req.body().isBlank()
                ? req.body().trim()
                : "Notifikasi uji real-time berhasil diproses untuk " + targetUser.getFullName() + ".";

        DispatchSummary summary = createAndSendNotificationWithSummary(
                targetUser.getId(),
                targetUser.getRole(),
                "FCM_TEST_PUSH",
                "SYSTEM",
                targetUser.getId(),
                title,
                body,
                OffsetDateTime.now(),
                true
        );

        return new TestPushResponse(
                summary.notification.getId(),
                summary.notification.getFcmDeliveryStatus(),
                summary.devicesTargeted,
                summary.devicesDelivered,
                summary.notification.getFcmMessageId(),
                summary.diagnosticMessage
        );
    }

    public NotificationListDto listMyNotifications() {
        SecurityUser current = SecurityUtils.currentUser();
        List<Notification> list = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .ne(Notification::getStatus, "CANCELLED")
                .and(w -> w.eq(Notification::getRecipientUserId, current.userId())
                        .or(sub -> sub.isNull(Notification::getRecipientUserId)
                                .eq(Notification::getRecipientRole, current.role())))
                .orderByDesc(Notification::getScheduledFor)
                .last("LIMIT 100"));

        long unread = list.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();
        return new NotificationListDto(unread, list);
    }

    @Transactional
    public Notification acknowledge(String notificationId) {
        SecurityUser current = SecurityUtils.currentUser();
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null) {
            throw BusinessException.notFound("Notifikasi tidak ditemukan.");
        }
        if (n.getRecipientUserId() != null && !n.getRecipientUserId().equals(current.userId()) && !current.isOwner()) {
            throw BusinessException.forbidden("Tidak dapat mengonfirmasi notifikasi pengguna lain.");
        }
        n.setIsRead(true);
        n.setAcknowledgedAt(OffsetDateTime.now());
        notificationMapper.updateById(n);
        return n;
    }

    public List<NotificationSetting> listSettings() {
        return settingMapper.selectList(new LambdaQueryWrapper<NotificationSetting>()
                .orderByAsc(NotificationSetting::getReminderType));
    }

    @Transactional
    public List<NotificationSetting> updateSettings(List<UpdateNotificationSettingRequest> updates) {
        SecurityUtils.requireOwner();
        for (UpdateNotificationSettingRequest req : updates) {
            NotificationSetting existing = settingMapper.selectOne(new LambdaQueryWrapper<NotificationSetting>()
                    .eq(NotificationSetting::getReminderType, req.reminderType().trim().toUpperCase())
                    .last("LIMIT 1"));
            if (existing != null) {
                if (req.enabled() != null) {
                    existing.setEnabled(req.enabled());
                }
                if (req.leadTimeMinutes() != null) {
                    existing.setLeadTimeMinutes(req.leadTimeMinutes());
                }
                if (req.recipientScope() != null) {
                    existing.setRecipientScope(req.recipientScope().trim().toUpperCase());
                }
                if (req.acknowledgementEnabled() != null) {
                    existing.setAcknowledgementEnabled(req.acknowledgementEnabled());
                }
                existing.setUpdatedAt(OffsetDateTime.now());
                settingMapper.updateById(existing);
            }
        }
        return listSettings();
    }

    @Transactional
    public void scheduleTripAssignmentAndPickupReminders(Trip trip) {
        cancelPendingTripNotifications(trip.getId());

        String driverUserId = null;
        if (trip.getDriverId() != null) {
            Driver d = driverMapper.selectById(trip.getDriverId());
            if (d != null) {
                driverUserId = d.getUserId();
            }
        }

        NotificationSetting assignedSetting = findSetting("DRIVER_ASSIGNED");
        if (driverUserId != null && (assignedSetting == null || Boolean.TRUE.equals(assignedSetting.getEnabled()))) {
            String pickupText = trip.getScheduledPickupAt().atZoneSameInstant(IdGenerator.WIB).format(TIME_FMT) + " WIB";
            createAndSendNotificationWithSummary(
                    driverUserId,
                    "DRIVER",
                    "DRIVER_ASSIGNED",
                    "TRIP",
                    trip.getId(),
                    "Tugas Perjalanan Baru: " + trip.getTripCode(),
                    "Jemput " + pickupText + " di " + trip.getPickupAddress() + " menuju " + trip.getDestinationAddress(),
                    OffsetDateTime.now(),
                    true
            );
        }

        reschedulePickupNotifications(trip);
    }

    @Transactional
    public void reschedulePickupNotifications(Trip trip) {
        List<Notification> existingScheduled = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReferenceType, "TRIP")
                .eq(Notification::getReferenceId, trip.getId())
                .eq(Notification::getReminderType, "UPCOMING_PICKUP")
                .eq(Notification::getStatus, "SCHEDULED"));
        for (Notification n : existingScheduled) {
            n.setStatus("CANCELLED");
            notificationMapper.updateById(n);
        }

        NotificationSetting pickupSetting = findSetting("UPCOMING_PICKUP");
        if (pickupSetting != null && !Boolean.TRUE.equals(pickupSetting.getEnabled())) {
            return;
        }
        int leadMin = pickupSetting != null && pickupSetting.getLeadTimeMinutes() != null
                ? pickupSetting.getLeadTimeMinutes() : 60;
        String scope = pickupSetting != null && pickupSetting.getRecipientScope() != null
                ? pickupSetting.getRecipientScope() : "BOTH";

        OffsetDateTime scheduledFor = trip.getScheduledPickupAt().minusMinutes(leadMin);
        String pickupText = trip.getScheduledPickupAt().atZoneSameInstant(IdGenerator.WIB).format(TIME_FMT) + " WIB";
        String title = "Pengingat Jemput (" + leadMin + " menit): " + trip.getTripCode();
        String body = "Jadwal jemput " + pickupText + " dari " + trip.getPickupAddress();

        if ("OWNER".equalsIgnoreCase(scope) || "BOTH".equalsIgnoreCase(scope)) {
            List<AppUser> owners = listActiveOwners();
            for (AppUser owner : owners) {
                createAndSendNotificationWithSummary(owner.getId(), "OWNER", "UPCOMING_PICKUP", "TRIP",
                        trip.getId(), title, body, scheduledFor, false);
            }
        }

        if (("DRIVER".equalsIgnoreCase(scope) || "BOTH".equalsIgnoreCase(scope)) && trip.getDriverId() != null) {
            Driver drv = driverMapper.selectById(trip.getDriverId());
            if (drv != null && drv.getUserId() != null) {
                createAndSendNotificationWithSummary(drv.getUserId(), "DRIVER", "UPCOMING_PICKUP", "TRIP",
                        trip.getId(), title, body, scheduledFor, false);
            }
        }
    }

    @Transactional
    public void cancelPendingTripNotifications(String tripId) {
        List<Notification> scheduled = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReferenceType, "TRIP")
                .eq(Notification::getReferenceId, tripId)
                .eq(Notification::getStatus, "SCHEDULED"));
        for (Notification n : scheduled) {
            n.setStatus("CANCELLED");
            notificationMapper.updateById(n);
        }
    }

    @Transactional
    public void notifyOwnerImmediate(String reminderType, String refType, String refId, String title, String body) {
        for (AppUser owner : listActiveOwners()) {
            createAndSendNotificationWithSummary(owner.getId(), "OWNER", reminderType, refType, refId,
                    title, body, OffsetDateTime.now(), true);
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.check-interval-ms:60000}")
    @Transactional
    public void dispatchDueScheduledNotifications() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Notification> dueList = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getStatus, "SCHEDULED")
                .le(Notification::getScheduledFor, now));
        for (Notification n : dueList) {
            n.setStatus("SENT");
            n.setDispatchedAt(now);
            if (n.getRecipientUserId() != null) {
                PushBatchOutcome batch = dispatchPushToUserDevices(
                        n.getRecipientUserId(),
                        n.getId(),
                        n.getReminderType(),
                        n.getTitle(),
                        n.getBody(),
                        n.getReferenceType(),
                        n.getReferenceId()
                );
                n.setFcmDeliveryStatus(batch.deliveryStatus);
                n.setFcmMessageId(batch.firstMessageId);
                n.setFcmError(batch.errorSummary);
            }
            notificationMapper.updateById(n);
        }
    }

    /**
     * Scans operational conditions (not-started trips, overdue trips, expiring STNK/insurance,
     * scheduled maintenance, and aged driver-held cash) and dispatches deduplicated alerts.
     */
    @Scheduled(
            initialDelayString = "${app.notifications.operational-scan-initial-delay-ms:15000}",
            fixedDelayString = "${app.notifications.operational-scan-ms:300000}"
    )
    @Transactional
    public int scanOperationalReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now(IdGenerator.WIB);
        int generated = 0;

        // 1. DRIVER_NOT_STARTED & OVERDUE_TRIP
        NotificationSetting notStartedSetting = findSetting("DRIVER_NOT_STARTED");
        NotificationSetting overdueSetting = findSetting("OVERDUE_TRIP");
        List<Trip> activeOrUpcomingTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .notIn(Trip::getStatus, List.of("COMPLETED", "CANCELLED", "NO_SHOW", "REASSIGNED")));

        for (Trip trip : activeOrUpcomingTrips) {
            if (notStartedSetting == null || Boolean.TRUE.equals(notStartedSetting.getEnabled())) {
                int graceMin = notStartedSetting != null && notStartedSetting.getLeadTimeMinutes() != null
                        ? notStartedSetting.getLeadTimeMinutes() : 15;
                if (("ASSIGNED".equals(trip.getStatus()) || "ACCEPTED".equals(trip.getStatus()))
                        && trip.getScheduledPickupAt() != null
                        && trip.getScheduledPickupAt().plusMinutes(graceMin).isBefore(now)
                        && !existsRecentReminder("DRIVER_NOT_STARTED", "TRIP", trip.getId(), now.minusHours(12))) {
                    notifyByScope(
                            notStartedSetting != null ? notStartedSetting.getRecipientScope() : "OWNER",
                            trip.getDriverId(),
                            "DRIVER_NOT_STARTED",
                            "TRIP",
                            trip.getId(),
                            "Supir Belum Berangkat: " + trip.getTripCode(),
                            "Perjalanan " + trip.getTripCode() + " melewati jadwal jemput belum berstatus menuju lokasi."
                    );
                    generated++;
                }
            }

            if (overdueSetting == null || Boolean.TRUE.equals(overdueSetting.getEnabled())) {
                int overdueMin = overdueSetting != null && overdueSetting.getLeadTimeMinutes() != null
                        ? overdueSetting.getLeadTimeMinutes() : 30;
                if (trip.getReservedEndAt() != null
                        && trip.getReservedEndAt().plusMinutes(overdueMin).isBefore(now)
                        && !existsRecentReminder("OVERDUE_TRIP", "TRIP", trip.getId(), now.minusHours(12))) {
                    notifyByScope(
                            overdueSetting != null ? overdueSetting.getRecipientScope() : "OWNER",
                            trip.getDriverId(),
                            "OVERDUE_TRIP",
                            "TRIP",
                            trip.getId(),
                            "Perjalanan Melewati Estimasi Selesai: " + trip.getTripCode(),
                            "Perjalanan " + trip.getTripCode() + " melewati batas waktu estimasi selesai."
                    );
                    generated++;
                }
            }
        }

        // 2. VEHICLE_TAX_EXPIRY & INSURANCE_EXPIRY
        Map<String, Vehicle> vehiclesById = new LinkedHashMap<>();
        for (Vehicle v : vehicleMapper.selectList(null)) {
            vehiclesById.put(v.getId(), v);
        }
        NotificationSetting taxSetting = findSetting("VEHICLE_TAX_EXPIRY");
        NotificationSetting insSetting = findSetting("INSURANCE_EXPIRY");
        for (VehicleDocument doc : vehicleDocumentMapper.selectList(null)) {
            if (doc.getExpiryDate() == null) continue;
            int leadDays = doc.getReminderDaysBefore() != null ? doc.getReminderDaysBefore() : 30;
            if (!doc.getExpiryDate().isAfter(today.plusDays(leadDays))) {
                String remType = "INSURANCE".equalsIgnoreCase(doc.getDocType())
                        ? "INSURANCE_EXPIRY" : "VEHICLE_TAX_EXPIRY";
                NotificationSetting s = "INSURANCE_EXPIRY".equals(remType) ? insSetting : taxSetting;
                if ((s == null || Boolean.TRUE.equals(s.getEnabled()))
                        && !existsRecentReminder(remType, "VEHICLE_DOCUMENT", doc.getId(), now.minusHours(24))) {
                    Vehicle v = vehiclesById.get(doc.getVehicleId());
                    String plate = v != null ? v.getRegistrationNumber() : doc.getVehicleId();
                    notifyOwnerImmediate(
                            remType,
                            "VEHICLE_DOCUMENT",
                            doc.getId(),
                            "Pengingat Dokumen " + doc.getDocType() + ": " + plate,
                            "Dokumen " + doc.getDocType() + " unit " + plate + " jatuh tempo pada " + doc.getExpiryDate() + "."
                    );
                    generated++;
                }
            }
        }

        // 3. SCHEDULED_MAINTENANCE
        NotificationSetting maintSetting = findSetting("SCHEDULED_MAINTENANCE");
        if (maintSetting == null || Boolean.TRUE.equals(maintSetting.getEnabled())) {
            int leadDays = maintSetting != null && maintSetting.getLeadTimeMinutes() != null
                    ? Math.max(1, maintSetting.getLeadTimeMinutes() / 1440) : 7;
            for (VehicleMaintenanceSchedule sched : maintenanceScheduleMapper.selectList(
                    new LambdaQueryWrapper<VehicleMaintenanceSchedule>().eq(VehicleMaintenanceSchedule::getIsActive, true))) {
                Vehicle v = vehiclesById.get(sched.getVehicleId());
                if (v == null) continue;
                boolean dateDue = sched.getNextDueDate() != null && !sched.getNextDueDate().isAfter(today.plusDays(leadDays));
                boolean kmDue = sched.getNextDueOdometerKm() != null && v.getCurrentOdometerKm() != null
                        && v.getCurrentOdometerKm().add(new BigDecimal("500")).compareTo(sched.getNextDueOdometerKm()) >= 0;
                if ((dateDue || kmDue) && !existsRecentReminder("SCHEDULED_MAINTENANCE", "MAINTENANCE_SCHEDULE", sched.getId(), now.minusHours(24))) {
                    notifyOwnerImmediate(
                            "SCHEDULED_MAINTENANCE",
                            "MAINTENANCE_SCHEDULE",
                            sched.getId(),
                            "Jadwal Servis " + sched.getMaintenanceType() + ": " + v.getRegistrationNumber(),
                            "Unit " + v.getRegistrationNumber() + " mendekati jadwal " + sched.getMaintenanceType() + "."
                    );
                    generated++;
                }
            }
        }

        // 4. OUTSTANDING_CASH_HANDOVER
        NotificationSetting cashSetting = findSetting("OUTSTANDING_CASH_HANDOVER");
        if (cashSetting == null || Boolean.TRUE.equals(cashSetting.getEnabled())) {
            int ageMin = cashSetting != null && cashSetting.getLeadTimeMinutes() != null
                    ? cashSetting.getLeadTimeMinutes() : 1440;
            OffsetDateTime cutoff = now.minusMinutes(ageMin);
            List<TripPayment> unsettled = tripPaymentMapper.selectList(new LambdaQueryWrapper<TripPayment>()
                    .eq(TripPayment::getIsVoided, false)
                    .eq(TripPayment::getCustodySettled, false)
                    .isNotNull(TripPayment::getHeldByDriverId)
                    .le(TripPayment::getPaidAt, cutoff));

            Map<String, BigDecimal> agedByDriver = new LinkedHashMap<>();
            for (TripPayment p : unsettled) {
                BigDecimal rem = p.getAmount().subtract(p.getHandedOverAmount() != null ? p.getHandedOverAmount() : BigDecimal.ZERO);
                if (rem.compareTo(BigDecimal.ZERO) > 0) {
                    agedByDriver.merge(p.getHeldByDriverId(), rem, BigDecimal::add);
                }
            }
            for (Map.Entry<String, BigDecimal> entry : agedByDriver.entrySet()) {
                String drvId = entry.getKey();
                if (!existsRecentReminder("OUTSTANDING_CASH_HANDOVER", "DRIVER", drvId, now.minusHours(24))) {
                    Driver drv = driverMapper.selectById(drvId);
                    String drvName = drv != null ? drv.getFullName() : drvId;
                    notifyByScope(
                            cashSetting != null ? cashSetting.getRecipientScope() : "OWNER",
                            drvId,
                            "OUTSTANDING_CASH_HANDOVER",
                            "DRIVER",
                            drvId,
                            "Setoran Tunai Belum Dikonfirmasi: " + drvName,
                            "Terdapat uang tunai pelanggan sebesar " + GeoAndLinkUtils.formatIdr(entry.getValue())
                                    + " pada " + drvName + " yang belum disetor."
                    );
                    generated++;
                }
            }
        }

        return generated;
    }

    private void notifyByScope(String scope,
                               String driverId,
                               String reminderType,
                               String refType,
                               String refId,
                               String title,
                               String body) {
        String s = scope != null ? scope : "OWNER";
        if ("OWNER".equalsIgnoreCase(s) || "BOTH".equalsIgnoreCase(s)) {
            notifyOwnerImmediate(reminderType, refType, refId, title, body);
        }
        if (("DRIVER".equalsIgnoreCase(s) || "BOTH".equalsIgnoreCase(s)) && driverId != null) {
            Driver drv = driverMapper.selectById(driverId);
            if (drv != null && drv.getUserId() != null) {
                createAndSendNotificationWithSummary(drv.getUserId(), "DRIVER", reminderType, refType, refId,
                        title, body, OffsetDateTime.now(), true);
            }
        }
    }

    private boolean existsRecentReminder(String reminderType, String refType, String refId, OffsetDateTime since) {
        Long cnt = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReminderType, reminderType)
                .eq(Notification::getReferenceType, refType)
                .eq(Notification::getReferenceId, refId)
                .ne(Notification::getStatus, "CANCELLED")
                .ge(Notification::getCreatedAt, since));
        return cnt != null && cnt > 0;
    }

    private List<AppUser> listActiveOwners() {
        return appUserMapper.selectList(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getRole, "OWNER")
                .eq(AppUser::getIsActive, true));
    }

    private NotificationSetting findSetting(String reminderType) {
        return settingMapper.selectOne(new LambdaQueryWrapper<NotificationSetting>()
                .eq(NotificationSetting::getReminderType, reminderType)
                .last("LIMIT 1"));
    }

    private record PushBatchOutcome(
            int devicesTargeted,
            int devicesDelivered,
            String deliveryStatus,
            String firstMessageId,
            String errorSummary,
            String diagnosticMessage
    ) {
    }

    private record DispatchSummary(
            Notification notification,
            int devicesTargeted,
            int devicesDelivered,
            String diagnosticMessage
    ) {
    }

    private DispatchSummary createAndSendNotificationWithSummary(String userId,
                                                                 String role,
                                                                 String reminderType,
                                                                 String refType,
                                                                 String refId,
                                                                 String title,
                                                                 String body,
                                                                 OffsetDateTime scheduledFor,
                                                                 boolean sendImmediately) {
        OffsetDateTime now = OffsetDateTime.now();
        Notification n = new Notification();
        n.setId(IdGenerator.newId());
        n.setRecipientUserId(userId);
        n.setRecipientRole(role);
        n.setReminderType(reminderType);
        n.setReferenceType(refType);
        n.setReferenceId(refId);
        n.setTitle(title);
        n.setBody(body);
        n.setScheduledFor(scheduledFor);
        n.setStatus(sendImmediately ? "SENT" : "SCHEDULED");
        n.setDispatchedAt(sendImmediately ? now : null);
        n.setIsRead(false);
        n.setFcmDeliveryStatus(sendImmediately ? "IN_APP_ONLY" : "SCHEDULED");
        n.setCreatedAt(now);
        notificationMapper.insert(n);

        if (!sendImmediately || userId == null) {
            return new DispatchSummary(n, 0, 0, "Notifikasi dijadwalkan.");
        }

        PushBatchOutcome batch = dispatchPushToUserDevices(
                userId, n.getId(), reminderType, title, body, refType, refId
        );
        n.setFcmDeliveryStatus(batch.deliveryStatus);
        n.setFcmMessageId(batch.firstMessageId);
        n.setFcmError(batch.errorSummary);
        notificationMapper.updateById(n);

        return new DispatchSummary(n, batch.devicesTargeted, batch.devicesDelivered, batch.diagnosticMessage);
    }

    private PushBatchOutcome dispatchPushToUserDevices(String userId,
                                                       String notificationId,
                                                       String reminderType,
                                                       String title,
                                                       String body,
                                                       String refType,
                                                       String refId) {
        List<UserFcmDevice> activeDevices = new ArrayList<>(fcmDeviceMapper.selectList(
                new LambdaQueryWrapper<UserFcmDevice>()
                        .eq(UserFcmDevice::getUserId, userId)
                        .eq(UserFcmDevice::getIsActive, true)
        ));

        AppUser user = appUserMapper.selectById(userId);
        String legacyToken = user != null ? user.getFcmToken() : null;
        boolean legacyCovered = legacyToken == null || legacyToken.isBlank()
                || activeDevices.stream().anyMatch(d -> legacyToken.equals(d.getFcmToken()));

        if (activeDevices.isEmpty() && legacyCovered) {
            return new PushBatchOutcome(
                    0, 0, "PUSH_SKIPPED_NO_TOKEN", null, null,
                    "Disimpan di kotak masuk aplikasi (perangkat pengguna belum mendaftarkan token FCM)."
            );
        }

        int targeted = activeDevices.size() + (legacyCovered ? 0 : 1);
        int delivered = 0;
        String firstMsgId = null;
        String lastError = null;
        boolean liveActive = false;
        OffsetDateTime now = OffsetDateTime.now();

        for (UserFcmDevice device : activeDevices) {
            PushDeliveryResult res = pushGateway.sendPush(
                    device.getFcmToken(), notificationId, reminderType, title, body, refType, refId
            );
            liveActive = liveActive || res.liveGatewayActive();
            if (res.delivered()) {
                delivered++;
                if (firstMsgId == null) firstMsgId = res.messageId();
                device.setLastPushAt(now);
                device.setLastError(null);
                device.setUpdatedAt(now);
                fcmDeviceMapper.updateById(device);
            } else {
                lastError = res.errorMessage();
                if (res.tokenUnregistered()) {
                    device.setIsActive(false);
                    device.setLastError("UNREGISTERED — dinonaktifkan otomatis");
                    device.setUpdatedAt(now);
                    fcmDeviceMapper.updateById(device);
                    if (user != null && device.getFcmToken().equals(user.getFcmToken())) {
                        user.setFcmToken(null);
                        user.setUpdatedAt(now);
                        appUserMapper.updateById(user);
                    }
                } else if (res.liveGatewayActive()) {
                    device.setLastError(res.errorMessage());
                    device.setUpdatedAt(now);
                    fcmDeviceMapper.updateById(device);
                }
            }
        }

        if (!legacyCovered && legacyToken != null) {
            PushDeliveryResult res = pushGateway.sendPush(
                    legacyToken, notificationId, reminderType, title, body, refType, refId
            );
            liveActive = liveActive || res.liveGatewayActive();
            if (res.delivered()) {
                delivered++;
                if (firstMsgId == null) firstMsgId = res.messageId();
            } else {
                lastError = res.errorMessage();
                if (res.tokenUnregistered() && user != null) {
                    user.setFcmToken(null);
                    user.setUpdatedAt(now);
                    appUserMapper.updateById(user);
                }
            }
        }

        if (delivered > 0) {
            return new PushBatchOutcome(
                    targeted, delivered, "PUSH_SENT", firstMsgId, null,
                    "Push FCM HTTP v1 berhasil dikirim ke " + delivered + " dari " + targeted + " perangkat."
            );
        }
        if (!liveActive) {
            return new PushBatchOutcome(
                    targeted, 0, "IN_APP_ONLY", null, null,
                    "Disimpan di kotak masuk aplikasi (menunggu kredensial Firebase Service Account untuk push latar belakang)."
            );
        }
        return new PushBatchOutcome(
                targeted, 0, "PUSH_FAILED", null, lastError,
                "Notifikasi tersimpan di aplikasi, namun pengiriman FCM gagal: " + (lastError != null ? lastError : "Unknown")
        );
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "-";
        if (token.length() <= 14) return token;
        return token.substring(0, 8) + "..." + token.substring(token.length() - 6);
    }
}
