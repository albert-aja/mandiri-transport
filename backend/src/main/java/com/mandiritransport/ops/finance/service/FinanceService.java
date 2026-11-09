package com.mandiritransport.ops.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CashCustodySummaryDto;
import com.mandiritransport.ops.finance.dto.FinanceDtos.ConfirmCashHandoverRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CreateCashHandoverRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.DriverCashCustodyDto;
import com.mandiritransport.ops.finance.dto.FinanceDtos.RecordPaymentRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.RecordTripExpenseRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.VoidPaymentRequest;
import com.mandiritransport.ops.finance.entity.CashHandover;
import com.mandiritransport.ops.finance.entity.CashHandoverItem;
import com.mandiritransport.ops.finance.entity.TripExpense;
import com.mandiritransport.ops.finance.entity.TripPayment;
import com.mandiritransport.ops.finance.mapper.CashHandoverItemMapper;
import com.mandiritransport.ops.finance.mapper.CashHandoverMapper;
import com.mandiritransport.ops.finance.mapper.TripExpenseMapper;
import com.mandiritransport.ops.finance.mapper.TripPaymentMapper;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private final TripPaymentMapper paymentMapper;
    private final CashHandoverMapper handoverMapper;
    private final CashHandoverItemMapper handoverItemMapper;
    private final TripExpenseMapper tripExpenseMapper;
    private final TripMapper tripMapper;
    private final DriverMapper driverMapper;
    private final CostEngineService costEngineService;
    private final AuditService auditService;

    public FinanceService(TripPaymentMapper paymentMapper,
                          CashHandoverMapper handoverMapper,
                          CashHandoverItemMapper handoverItemMapper,
                          TripExpenseMapper tripExpenseMapper,
                          TripMapper tripMapper,
                          DriverMapper driverMapper,
                          CostEngineService costEngineService,
                          AuditService auditService) {
        this.paymentMapper = paymentMapper;
        this.handoverMapper = handoverMapper;
        this.handoverItemMapper = handoverItemMapper;
        this.tripExpenseMapper = tripExpenseMapper;
        this.tripMapper = tripMapper;
        this.driverMapper = driverMapper;
        this.costEngineService = costEngineService;
        this.auditService = auditService;
    }

    @Transactional
    public TripPayment recordPayment(String tripId,
                                     RecordPaymentRequest req,
                                     String headerMutationId,
                                     boolean allowOfflineReassignedDriver) {
        SecurityUser current = SecurityUtils.currentUser();
        String mutationId = (req.clientMutationId() != null && !req.clientMutationId().isBlank())
                ? req.clientMutationId().trim()
                : (headerMutationId != null && !headerMutationId.isBlank() ? headerMutationId.trim() : null);

        if (mutationId != null) {
            TripPayment existing = paymentMapper.selectOne(new LambdaQueryWrapper<TripPayment>()
                    .eq(TripPayment::getClientMutationId, mutationId)
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
            String myDriverId = SecurityUtils.requireDriverId();
            if (!myDriverId.equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Supir hanya dapat mencatat pembayaran pada perjalanan yang ditugaskan kepadanya.");
            }
        }

        if (req.amount() == null || req.amount().signum() <= 0) {
            throw BusinessException.badRequest("INVALID_AMOUNT", "Jumlah pembayaran harus lebih besar dari 0.");
        }

        String method = req.paymentMethod().trim().toUpperCase();
        boolean isRefund = Boolean.TRUE.equals(req.isRefund());
        String collectedByRole = current.isDriver()
                ? "DRIVER"
                : (req.collectedByRole() != null && !req.collectedByRole().isBlank()
                ? req.collectedByRole().trim().toUpperCase() : "OWNER");

        String heldByDriverId = null;
        boolean custodySettled = true;

        if ("CASH".equals(method) && "DRIVER".equals(collectedByRole) && !isRefund) {
            heldByDriverId = current.isDriver() ? SecurityUtils.requireDriverId() : trip.getDriverId();
            if (heldByDriverId == null) {
                throw BusinessException.badRequest("DRIVER_REQUIRED_FOR_CASH",
                        "Tidak ada supir yang ditugaskan untuk memegang uang tunai perjalanan ini.");
            }
            custodySettled = false;
        }

        TripPayment p = new TripPayment();
        p.setId(IdGenerator.newId());
        p.setTripId(tripId);
        p.setClientMutationId(mutationId);
        p.setAmount(req.amount());
        p.setPaymentMethod(method);
        p.setIsRefund(isRefund);
        p.setCollectedByUserId(current.userId());
        p.setCollectedByRole(collectedByRole);
        p.setHeldByDriverId(heldByDriverId);
        p.setHandedOverAmount(BigDecimal.ZERO);
        p.setCustodySettled(custodySettled);
        p.setPaidAt(req.paidAt() != null ? req.paidAt() : OffsetDateTime.now());
        p.setReferenceNumber(req.referenceNumber());
        p.setReceiptPath(req.receiptPath());
        p.setNotes(req.notes());
        p.setIsVoided(false);
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        paymentMapper.insert(p);

        recalculateTripFinancialStatuses(tripId);
        auditService.log("TRIP_PAYMENT", p.getId(), "CREATE",
                "Mencatat pembayaran " + method + " sebesar " + p.getAmount() + " untuk " + trip.getTripCode(), null, p);
        return p;
    }

    @Transactional
    public TripPayment voidPayment(String paymentId, VoidPaymentRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        TripPayment p = paymentMapper.selectById(paymentId);
        if (p == null) {
            throw BusinessException.notFound("Data pembayaran tidak ditemukan.");
        }
        if (Boolean.TRUE.equals(p.getIsVoided())) {
            return p;
        }
        p.setIsVoided(true);
        p.setVoidedBy(owner.userId());
        p.setVoidReason(req.reason().trim());
        p.setUpdatedAt(OffsetDateTime.now());
        paymentMapper.updateById(p);

        recalculateTripFinancialStatuses(p.getTripId());
        auditService.log("TRIP_PAYMENT", p.getId(), "VOID",
                "Membatalkan (void) pembayaran: " + req.reason(), null, p);
        return p;
    }

    public CashCustodySummaryDto getCashCustodySummary() {
        SecurityUser current = SecurityUtils.currentUser();

        LambdaQueryWrapper<Driver> drvQw = new LambdaQueryWrapper<Driver>()
                .eq(Driver::getIsActive, true)
                .orderByAsc(Driver::getDriverCode);
        if (current.isDriver()) {
            drvQw.eq(Driver::getId, SecurityUtils.requireDriverId());
        }
        List<Driver> drivers = driverMapper.selectList(drvQw);

        BigDecimal totalHeld = BigDecimal.ZERO;
        List<DriverCashCustodyDto> byDriver = new ArrayList<>();

        for (Driver d : drivers) {
            List<TripPayment> unsettled = paymentMapper.selectList(new LambdaQueryWrapper<TripPayment>()
                    .eq(TripPayment::getHeldByDriverId, d.getId())
                    .eq(TripPayment::getCustodySettled, false)
                    .eq(TripPayment::getIsVoided, false)
                    .eq(TripPayment::getIsRefund, false)
                    .orderByAsc(TripPayment::getPaidAt));

            BigDecimal drvSum = BigDecimal.ZERO;
            for (TripPayment p : unsettled) {
                BigDecimal rem = remainingHeldOnPayment(p);
                drvSum = drvSum.add(rem);
            }
            totalHeld = totalHeld.add(drvSum);
            byDriver.add(new DriverCashCustodyDto(d.getId(), d.getFullName(), drvSum, unsettled));
        }

        LambdaQueryWrapper<CashHandover> hoQw = new LambdaQueryWrapper<CashHandover>()
                .orderByDesc(CashHandover::getHandedOverAt)
                .last("LIMIT 30");
        if (current.isDriver()) {
            hoQw.eq(CashHandover::getDriverId, SecurityUtils.requireDriverId());
        }
        List<CashHandover> handovers = handoverMapper.selectList(hoQw);

        long pendingCount = handoverMapper.selectCount(new LambdaQueryWrapper<CashHandover>()
                .eq(CashHandover::getStatus, "PENDING_CONFIRMATION")
                .eq(current.isDriver(), CashHandover::getDriverId, current.driverId()));
        long disputedCount = handoverMapper.selectCount(new LambdaQueryWrapper<CashHandover>()
                .eq(CashHandover::getStatus, "DISPUTED")
                .eq(current.isDriver(), CashHandover::getDriverId, current.driverId()));

        return new CashCustodySummaryDto(totalHeld, pendingCount, disputedCount, byDriver, handovers);
    }

    @Transactional
    public CashHandover createCashHandover(CreateCashHandoverRequest req) {
        SecurityUser current = SecurityUtils.currentUser();
        String driverId = current.isDriver()
                ? SecurityUtils.requireDriverId()
                : req.driverId();
        if (driverId == null || driverId.isBlank()) {
            throw BusinessException.badRequest("DRIVER_REQUIRED", "driverId wajib diisi untuk setoran tunai.");
        }
        if (current.isDriver() && req.driverId() != null && !driverId.equals(req.driverId())) {
            throw BusinessException.forbidden("Supir hanya dapat membuat setoran tunai atas namanya sendiri.");
        }
        if (req.declaredAmount() == null || req.declaredAmount().signum() <= 0) {
            throw BusinessException.badRequest("INVALID_AMOUNT", "Jumlah setoran harus lebih besar dari 0.");
        }

        List<TripPayment> targetPayments;
        if (req.paymentIds() != null && !req.paymentIds().isEmpty()) {
            targetPayments = paymentMapper.selectByIds(req.paymentIds());
        } else {
            targetPayments = paymentMapper.selectList(new LambdaQueryWrapper<TripPayment>()
                    .eq(TripPayment::getHeldByDriverId, driverId)
                    .eq(TripPayment::getCustodySettled, false)
                    .eq(TripPayment::getIsVoided, false)
                    .eq(TripPayment::getIsRefund, false)
                    .orderByAsc(TripPayment::getPaidAt));
        }

        if (targetPayments.isEmpty()) {
            throw BusinessException.badRequest("NO_UNSETTLED_CASH", "Tidak ada uang tunai belum disetor untuk supir ini.");
        }

        for (TripPayment p : targetPayments) {
            if (!driverId.equals(p.getHeldByDriverId()) || Boolean.TRUE.equals(p.getIsVoided()) || Boolean.TRUE.equals(p.getCustodySettled())) {
                throw BusinessException.badRequest("INVALID_PAYMENT_FOR_HANDOVER",
                        "Pembayaran " + p.getId() + " tidak valid atau sudah lunas disetor.");
            }
        }

        CashHandover ho = new CashHandover();
        ho.setId(IdGenerator.newId());
        ho.setHandoverCode(IdGenerator.handoverCode(OffsetDateTime.now()));
        ho.setDriverId(driverId);
        ho.setDeclaredAmount(req.declaredAmount());
        ho.setConfirmedAmount(BigDecimal.ZERO);
        ho.setStatus("PENDING_CONFIRMATION");
        ho.setDriverNotes(req.driverNotes());
        ho.setHandedOverAt(OffsetDateTime.now());
        ho.setCreatedAt(OffsetDateTime.now());
        ho.setUpdatedAt(OffsetDateTime.now());
        ho.setVersion(0L);
        handoverMapper.insert(ho);

        BigDecimal remainingToAllocate = req.declaredAmount();
        for (TripPayment p : targetPayments) {
            if (remainingToAllocate.signum() <= 0) {
                break;
            }
            BigDecimal remOnPayment = remainingHeldOnPayment(p);
            if (remOnPayment.signum() <= 0) {
                continue;
            }
            BigDecimal applied = remOnPayment.min(remainingToAllocate);
            CashHandoverItem item = new CashHandoverItem();
            item.setId(IdGenerator.newId());
            item.setHandoverId(ho.getId());
            item.setTripPaymentId(p.getId());
            item.setTripId(p.getTripId());
            item.setAmountApplied(applied);
            item.setCreatedAt(OffsetDateTime.now());
            handoverItemMapper.insert(item);
            remainingToAllocate = remainingToAllocate.subtract(applied);
        }

        auditService.log("CASH_HANDOVER", ho.getId(), "CREATE",
                "Inisiasi setoran tunai " + ho.getHandoverCode() + ": " + ho.getDeclaredAmount(), null, ho);
        return ho;
    }

    @Transactional
    public CashHandover confirmCashHandover(String handoverId, ConfirmCashHandoverRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        CashHandover ho = handoverMapper.selectById(handoverId);
        if (ho == null) {
            throw BusinessException.notFound("Setoran tunai tidak ditemukan.");
        }
        if ("HANDED_OVER_CONFIRMED".equals(ho.getStatus())) {
            throw BusinessException.badRequest("ALREADY_CONFIRMED", "Setoran tunai ini sudah pernah dikonfirmasi lunas.");
        }

        List<CashHandoverItem> items = handoverItemMapper.selectList(
                new LambdaQueryWrapper<CashHandoverItem>().eq(CashHandoverItem::getHandoverId, handoverId));

        BigDecimal confirmed = req.confirmedAmount() != null ? req.confirmedAmount() : BigDecimal.ZERO;
        boolean markDisputed = Boolean.TRUE.equals(req.markDisputed());

        BigDecimal remainingConfirm = confirmed.max(BigDecimal.ZERO);
        Set<String> affectedTripIds = new HashSet<>();
        boolean anyPaymentStillUnsettled = false;

        for (CashHandoverItem item : items) {
            affectedTripIds.add(item.getTripId());
            TripPayment p = paymentMapper.selectById(item.getTripPaymentId());
            if (p == null || Boolean.TRUE.equals(p.getIsVoided())) {
                continue;
            }
            BigDecimal remOnPayment = remainingHeldOnPayment(p);
            BigDecimal toApply = remOnPayment.min(item.getAmountApplied()).min(remainingConfirm);
            if (toApply.signum() > 0) {
                BigDecimal currentHanded = p.getHandedOverAmount() != null ? p.getHandedOverAmount() : BigDecimal.ZERO;
                BigDecimal nextHanded = currentHanded.add(toApply);
                p.setHandedOverAmount(nextHanded);
                if (nextHanded.compareTo(p.getAmount()) >= 0) {
                    p.setCustodySettled(true);
                } else {
                    anyPaymentStillUnsettled = true;
                }
                p.setUpdatedAt(OffsetDateTime.now());
                paymentMapper.updateById(p);
                remainingConfirm = remainingConfirm.subtract(toApply);
            } else if (!Boolean.TRUE.equals(p.getCustodySettled())) {
                anyPaymentStillUnsettled = true;
            }
        }

        ho.setReceivedByOwnerId(owner.userId());
        ho.setConfirmedAmount(confirmed);
        ho.setOwnerNotes(req.ownerNotes());
        ho.setConfirmedAt(OffsetDateTime.now());
        ho.setUpdatedAt(OffsetDateTime.now());

        if (markDisputed) {
            ho.setStatus("DISPUTED");
        } else if (confirmed.compareTo(ho.getDeclaredAmount()) < 0 || anyPaymentStillUnsettled) {
            ho.setStatus("PARTIALLY_HANDED_OVER");
        } else {
            ho.setStatus("HANDED_OVER_CONFIRMED");
        }
        handoverMapper.updateById(ho);

        for (String tripId : affectedTripIds) {
            recalculateTripFinancialStatuses(tripId);
        }

        auditService.log("CASH_HANDOVER", ho.getId(), "CONFIRM",
                "Konfirmasi setoran " + ho.getHandoverCode() + " status=" + ho.getStatus(), null, ho);
        return ho;
    }

    @Transactional
    public TripExpense recordTripExpense(String tripId,
                                         RecordTripExpenseRequest req,
                                         String headerMutationId,
                                         boolean allowOfflineReassignedDriver) {
        SecurityUser current = SecurityUtils.currentUser();
        String mutationId = (req.clientMutationId() != null && !req.clientMutationId().isBlank())
                ? req.clientMutationId().trim()
                : (headerMutationId != null && !headerMutationId.isBlank() ? headerMutationId.trim() : null);

        if (mutationId != null) {
            TripExpense existing = tripExpenseMapper.selectOne(new LambdaQueryWrapper<TripExpense>()
                    .eq(TripExpense::getClientMutationId, mutationId)
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
            String myDriverId = SecurityUtils.requireDriverId();
            if (!myDriverId.equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Supir hanya dapat mencatat pengeluaran pada perjalanan miliknya.");
            }
        }
        if (req.amount() == null || req.amount().signum() <= 0) {
            throw BusinessException.badRequest("INVALID_AMOUNT", "Jumlah pengeluaran harus lebih besar dari 0.");
        }

        TripExpense exp = new TripExpense();
        exp.setId(IdGenerator.newId());
        exp.setTripId(tripId);
        exp.setClientMutationId(mutationId);
        exp.setCategory(req.category().trim().toUpperCase());
        exp.setAmount(req.amount());
        exp.setPaidByUserId(current.userId());
        exp.setPaidByRole(current.role());
        exp.setFuelLiters(req.fuelLiters());
        exp.setReceiptPath(req.receiptPath());
        exp.setNotes(req.notes());
        exp.setIncurredAt(req.incurredAt() != null ? req.incurredAt() : OffsetDateTime.now());
        exp.setIsVoided(false);
        exp.setCreatedAt(OffsetDateTime.now());
        exp.setUpdatedAt(OffsetDateTime.now());
        tripExpenseMapper.insert(exp);

        // If trip is already completed, reconcile actual vs estimated preserving locked formula version
        if ("COMPLETED".equalsIgnoreCase(trip.getStatus())) {
            costEngineService.calculateAndSaveSnapshot(trip);
        }

        auditService.log("TRIP_EXPENSE", exp.getId(), "CREATE",
                "Mencatat pengeluaran " + exp.getCategory() + " Rp " + exp.getAmount() + " pada " + trip.getTripCode(), null, exp);
        return exp;
    }

    @Transactional
    public void recalculateTripFinancialStatuses(String tripId) {
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            return;
        }
        List<TripPayment> activePayments = paymentMapper.selectList(new LambdaQueryWrapper<TripPayment>()
                .eq(TripPayment::getTripId, tripId)
                .eq(TripPayment::getIsVoided, false));

        // 1. Orthogonal Payment Status (Customer -> Business)
        BigDecimal grossPaid = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        for (TripPayment p : activePayments) {
            if (Boolean.TRUE.equals(p.getIsRefund())) {
                refunded = refunded.add(p.getAmount());
            } else {
                grossPaid = grossPaid.add(p.getAmount());
            }
        }
        BigDecimal netPaid = grossPaid.subtract(refunded);
        BigDecimal agreedFare = trip.getAgreedFareSnapshot() != null ? trip.getAgreedFareSnapshot() : BigDecimal.ZERO;

        String paymentStatus;
        if (refunded.signum() > 0 && netPaid.signum() <= 0) {
            paymentStatus = "REFUNDED";
        } else if (netPaid.signum() <= 0) {
            paymentStatus = "UNPAID";
        } else if (netPaid.compareTo(agreedFare) < 0) {
            paymentStatus = "PARTIALLY_PAID";
        } else if (netPaid.compareTo(agreedFare) == 0) {
            paymentStatus = "PAID";
        } else {
            paymentStatus = "OVERPAID";
        }

        // 2. Orthogonal Cash Custody Status (Driver -> Owner)
        List<TripPayment> driverCashPayments = activePayments.stream()
                .filter(p -> p.getHeldByDriverId() != null && !Boolean.TRUE.equals(p.getIsRefund()))
                .toList();

        String custodyStatus;
        BigDecimal driverHeld = BigDecimal.ZERO;

        if (driverCashPayments.isEmpty()) {
            custodyStatus = "NO_DRIVER_CASH_DUE";
        } else {
            BigDecimal totalDriverCash = BigDecimal.ZERO;
            BigDecimal totalHandedOver = BigDecimal.ZERO;
            for (TripPayment p : driverCashPayments) {
                totalDriverCash = totalDriverCash.add(p.getAmount());
                totalHandedOver = totalHandedOver.add(p.getHandedOverAmount() != null ? p.getHandedOverAmount() : BigDecimal.ZERO);
            }
            driverHeld = totalDriverCash.subtract(totalHandedOver).max(BigDecimal.ZERO);

            // Check if any linked handover for this trip is DISPUTED
            List<CashHandoverItem> tripHoItems = handoverItemMapper.selectList(
                    new LambdaQueryWrapper<CashHandoverItem>().eq(CashHandoverItem::getTripId, tripId));
            boolean hasDisputedHandover = false;
            if (!tripHoItems.isEmpty()) {
                Set<String> hoIds = tripHoItems.stream().map(CashHandoverItem::getHandoverId).collect(Collectors.toSet());
                Map<String, String> hoStatuses = handoverMapper.selectByIds(hoIds).stream()
                        .collect(Collectors.toMap(CashHandover::getId, CashHandover::getStatus));
                hasDisputedHandover = hoStatuses.containsValue("DISPUTED");
            }

            if (hasDisputedHandover && driverHeld.signum() > 0) {
                custodyStatus = "DISPUTED";
            } else if (driverHeld.signum() == 0) {
                custodyStatus = "HANDED_OVER_CONFIRMED";
            } else if (totalHandedOver.signum() > 0) {
                custodyStatus = "PARTIALLY_HANDED_OVER";
            } else {
                custodyStatus = "HELD_BY_DRIVER";
            }
        }

        trip.setTotalPaidAmount(netPaid.max(BigDecimal.ZERO));
        trip.setPaymentStatus(paymentStatus);
        trip.setDriverCashHeldAmount(driverHeld);
        trip.setCashCustodyStatus(custodyStatus);
        trip.setUpdatedAt(OffsetDateTime.now());
        tripMapper.updateById(trip);
    }

    private BigDecimal remainingHeldOnPayment(TripPayment p) {
        BigDecimal handed = p.getHandedOverAmount() != null ? p.getHandedOverAmount() : BigDecimal.ZERO;
        return p.getAmount().subtract(handed).max(BigDecimal.ZERO);
    }
}
