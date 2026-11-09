package com.mandiritransport.ops.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CostFormulaComponentRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CostFormulaVersionDto;
import com.mandiritransport.ops.finance.dto.FinanceDtos.CreateCostFormulaRequest;
import com.mandiritransport.ops.finance.dto.FinanceDtos.TripProfitabilityDto;
import com.mandiritransport.ops.finance.entity.CostFormulaComponent;
import com.mandiritransport.ops.finance.entity.CostFormulaVersion;
import com.mandiritransport.ops.finance.entity.TripCostSnapshot;
import com.mandiritransport.ops.finance.entity.TripCostSnapshotItem;
import com.mandiritransport.ops.finance.entity.TripExpense;
import com.mandiritransport.ops.finance.mapper.CostFormulaComponentMapper;
import com.mandiritransport.ops.finance.mapper.CostFormulaVersionMapper;
import com.mandiritransport.ops.finance.mapper.TripCostSnapshotItemMapper;
import com.mandiritransport.ops.finance.mapper.TripCostSnapshotMapper;
import com.mandiritransport.ops.finance.mapper.TripExpenseMapper;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.entity.Vehicle;
import com.mandiritransport.ops.vehicle.mapper.RouteMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CostEngineService {

    private final CostFormulaVersionMapper versionMapper;
    private final CostFormulaComponentMapper componentMapper;
    private final TripCostSnapshotMapper snapshotMapper;
    private final TripCostSnapshotItemMapper snapshotItemMapper;
    private final TripExpenseMapper tripExpenseMapper;
    private final TripMapper tripMapper;
    private final RouteMapper routeMapper;
    private final VehicleMapper vehicleMapper;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public CostEngineService(CostFormulaVersionMapper versionMapper,
                             CostFormulaComponentMapper componentMapper,
                             TripCostSnapshotMapper snapshotMapper,
                             TripCostSnapshotItemMapper snapshotItemMapper,
                             TripExpenseMapper tripExpenseMapper,
                             TripMapper tripMapper,
                             RouteMapper routeMapper,
                             VehicleMapper vehicleMapper,
                             ObjectMapper objectMapper,
                             AuditService auditService) {
        this.versionMapper = versionMapper;
        this.componentMapper = componentMapper;
        this.snapshotMapper = snapshotMapper;
        this.snapshotItemMapper = snapshotItemMapper;
        this.tripExpenseMapper = tripExpenseMapper;
        this.tripMapper = tripMapper;
        this.routeMapper = routeMapper;
        this.vehicleMapper = vehicleMapper;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    public List<CostFormulaVersionDto> listFormulaVersions() {
        List<CostFormulaVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<CostFormulaVersion>().orderByDesc(CostFormulaVersion::getVersionNumber));
        return versions.stream().map(this::toVersionDto).toList();
    }

    @Transactional
    public CostFormulaVersionDto createFormulaVersion(CreateCostFormulaRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();

        int nextVersionNum = req.versionNumber() != null ? req.versionNumber() : determineNextVersionNumber();

        // Validate any FORMULA_ALLOCATION expressions upfront before saving
        for (CostFormulaComponentRequest comp : req.components()) {
            if ("FORMULA_ALLOCATION".equalsIgnoreCase(comp.calcType())) {
                Map<String, BigDecimal> sampleVars = buildVariables(
                        new BigDecimal("40"), new BigDecimal("1.5"), new BigDecimal("250000"), comp.parametersJson());
                FormulaExpressionEvaluator.evaluate(comp.expressionFormula(), sampleVars);
            }
        }

        boolean active = req.isActive() == null || req.isActive();
        if (active) {
            // Close previous open-ended active formula versions without touching any trip_cost_snapshots!
            List<CostFormulaVersion> activeVersions = versionMapper.selectList(
                    new LambdaQueryWrapper<CostFormulaVersion>().eq(CostFormulaVersion::getIsActive, true));
            for (CostFormulaVersion prev : activeVersions) {
                if (prev.getEffectiveTo() == null || !prev.getEffectiveTo().isBefore(req.effectiveFrom())) {
                    prev.setEffectiveTo(req.effectiveFrom().minusDays(1));
                }
                prev.setIsActive(false);
                versionMapper.updateById(prev);
            }
        }

        CostFormulaVersion ver = new CostFormulaVersion();
        ver.setId(IdGenerator.newId());
        ver.setVersionNumber(nextVersionNum);
        ver.setName(req.name().trim());
        ver.setEffectiveFrom(req.effectiveFrom());
        ver.setEffectiveTo(req.effectiveTo());
        ver.setNotes(req.notes());
        ver.setIsActive(active);
        ver.setCreatedBy(owner.userId());
        ver.setCreatedAt(OffsetDateTime.now());
        versionMapper.insert(ver);

        int order = 1;
        for (CostFormulaComponentRequest cr : req.components()) {
            CostFormulaComponent c = new CostFormulaComponent();
            c.setId(IdGenerator.newId());
            c.setFormulaVersionId(ver.getId());
            c.setComponentCode(cr.componentCode().trim().toUpperCase());
            c.setName(cr.name().trim());
            c.setCalcType(cr.calcType().trim().toUpperCase());
            c.setReconciliationPolicy(cr.reconciliationPolicy().trim().toUpperCase());
            c.setExpenseCategoryLink(cr.expenseCategoryLink() != null && !cr.expenseCategoryLink().isBlank()
                    ? cr.expenseCategoryLink().trim().toUpperCase() : null);
            c.setFixedAmount(cr.fixedAmount());
            c.setRateValue(cr.rateValue());
            c.setMonthlyAmount(cr.monthlyAmount());
            c.setAssumedMonthlyTrips(cr.assumedMonthlyTrips());
            c.setExpressionFormula(cr.expressionFormula());
            c.setParametersJson(cr.parametersJson());
            c.setVehicleTypeFilter(cr.vehicleTypeFilter());
            c.setSortOrder(cr.sortOrder() != null ? cr.sortOrder() : order++);
            componentMapper.insert(c);
        }

        auditService.log("COST_FORMULA_VERSION", ver.getId(), "CREATE",
                "Membuat versi formula biaya v" + ver.getVersionNumber() + ": " + ver.getName(), null, ver);
        return toVersionDto(ver);
    }

    @Transactional
    public TripCostSnapshot calculateAndSaveSnapshot(Trip trip) {
        TripCostSnapshot existing = snapshotMapper.selectOne(new LambdaQueryWrapper<TripCostSnapshot>()
                .eq(TripCostSnapshot::getTripId, trip.getId())
                .last("LIMIT 1"));

        CostFormulaVersion formulaVersion;
        if (existing != null) {
            // Preserve immutable historical formula version even if newer formulas were created later!
            formulaVersion = versionMapper.selectById(existing.getFormulaVersionId());
        } else {
            LocalDate tripDate = trip.getScheduledPickupAt() != null
                    ? trip.getScheduledPickupAt().atZoneSameInstant(IdGenerator.WIB).toLocalDate()
                    : LocalDate.now(IdGenerator.WIB);
            formulaVersion = resolveEffectiveFormulaVersion(tripDate);
        }

        if (formulaVersion == null) {
            throw BusinessException.badRequest("NO_COST_FORMULA", "Belum ada versi formula biaya operasional yang tersedia.");
        }

        ComputedCalculation calc = computeForTrip(trip, formulaVersion, existing != null ? existing.getId() : IdGenerator.newId());

        if (existing != null) {
            snapshotItemMapper.delete(new LambdaQueryWrapper<TripCostSnapshotItem>()
                    .eq(TripCostSnapshotItem::getSnapshotId, existing.getId()));
            TripCostSnapshot updated = calc.snapshot;
            updated.setId(existing.getId());
            snapshotMapper.updateById(updated);
            for (TripCostSnapshotItem item : calc.items) {
                item.setSnapshotId(existing.getId());
                snapshotItemMapper.insert(item);
            }
            return updated;
        } else {
            snapshotMapper.insert(calc.snapshot);
            for (TripCostSnapshotItem item : calc.items) {
                snapshotItemMapper.insert(item);
            }
            return calc.snapshot;
        }
    }

    public TripProfitabilityDto getTripProfitability(String tripId) {
        SecurityUtils.requireOwner();
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw BusinessException.notFound("Perjalanan tidak ditemukan.");
        }

        TripCostSnapshot locked = snapshotMapper.selectOne(new LambdaQueryWrapper<TripCostSnapshot>()
                .eq(TripCostSnapshot::getTripId, tripId)
                .last("LIMIT 1"));

        if (locked != null) {
            CostFormulaVersion ver = versionMapper.selectById(locked.getFormulaVersionId());
            List<TripCostSnapshotItem> items = snapshotItemMapper.selectList(
                    new LambdaQueryWrapper<TripCostSnapshotItem>().eq(TripCostSnapshotItem::getSnapshotId, locked.getId()));
            if (items.isEmpty() && ver != null) {
                // Build non-mutating item breakdown for seeded historical snapshots
                items = computeForTrip(trip, ver, locked.getId()).items;
            }
            return new TripProfitabilityDto(
                    trip.getId(),
                    trip.getTripCode(),
                    true,
                    locked.getFormulaVersionNumber(),
                    ver != null ? ver.getName() : ("Formula v" + locked.getFormulaVersionNumber()),
                    locked.getDistanceKmUsed(),
                    locked.getDistanceSource(),
                    locked.getDurationHoursUsed(),
                    locked.getAgreedFareSnapshot(),
                    locked.getEstimatedOperatingCost(),
                    locked.getActualExpensesTotal(),
                    locked.getAllocatedOperatingCost(),
                    locked.getTotalEffectiveCost(),
                    locked.getEstimatedProfit(),
                    locked.getActualProfit(),
                    items
            );
        }

        // Live preview for non-completed trips
        LocalDate tripDate = trip.getScheduledPickupAt() != null
                ? trip.getScheduledPickupAt().atZoneSameInstant(IdGenerator.WIB).toLocalDate()
                : LocalDate.now(IdGenerator.WIB);
        CostFormulaVersion ver = resolveEffectiveFormulaVersion(tripDate);
        ComputedCalculation preview = computeForTrip(trip, ver, "preview");
        return new TripProfitabilityDto(
                trip.getId(),
                trip.getTripCode(),
                false,
                ver.getVersionNumber(),
                ver.getName(),
                preview.snapshot.getDistanceKmUsed(),
                preview.snapshot.getDistanceSource(),
                preview.snapshot.getDurationHoursUsed(),
                preview.snapshot.getAgreedFareSnapshot(),
                preview.snapshot.getEstimatedOperatingCost(),
                preview.snapshot.getActualExpensesTotal(),
                preview.snapshot.getAllocatedOperatingCost(),
                preview.snapshot.getTotalEffectiveCost(),
                preview.snapshot.getEstimatedProfit(),
                preview.snapshot.getActualProfit(),
                preview.items
        );
    }

    private ComputedCalculation computeForTrip(Trip trip, CostFormulaVersion ver, String snapshotId) {
        Route route = trip.getRouteId() != null ? routeMapper.selectById(trip.getRouteId()) : null;
        Vehicle vehicle = trip.getVehicleId() != null ? vehicleMapper.selectById(trip.getVehicleId()) : null;
        String vehicleType = vehicle != null ? vehicle.getVehicleType() : null;

        // 1. Resolve distance & source
        BigDecimal distanceKm;
        String distanceSource;
        if (trip.getOdometerStartKm() != null && trip.getOdometerEndKm() != null
                && trip.getOdometerEndKm().compareTo(trip.getOdometerStartKm()) > 0) {
            distanceKm = trip.getOdometerEndKm().subtract(trip.getOdometerStartKm()).setScale(2, RoundingMode.HALF_UP);
            distanceSource = "ODOMETER";
        } else if (trip.getGpsDistanceKm() != null && trip.getGpsDistanceKm().signum() > 0) {
            distanceKm = trip.getGpsDistanceKm().setScale(2, RoundingMode.HALF_UP);
            distanceSource = "GPS";
        } else if (route != null && route.getTypicalDistanceKm() != null) {
            distanceKm = route.getTypicalDistanceKm().setScale(2, RoundingMode.HALF_UP);
            distanceSource = "ROUTE_DEFAULT";
        } else {
            distanceKm = new BigDecimal("25.00");
            distanceSource = "ROUTE_DEFAULT";
        }

        // 2. Resolve duration hours
        int durationMin = trip.getActualDurationMin() != null && trip.getActualDurationMin() > 0
                ? trip.getActualDurationMin()
                : (trip.getEstimatedDurationMin() != null ? trip.getEstimatedDurationMin() : 60);
        BigDecimal durationHours = BigDecimal.valueOf(durationMin)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal agreedFare = trip.getAgreedFareSnapshot() != null ? trip.getAgreedFareSnapshot() : BigDecimal.ZERO;

        // 3. Load actual non-voided trip expenses grouped by category
        List<TripExpense> expenses = tripExpenseMapper.selectList(new LambdaQueryWrapper<TripExpense>()
                .eq(TripExpense::getTripId, trip.getId())
                .eq(TripExpense::getIsVoided, false));
        Map<String, BigDecimal> actualByCategory = new HashMap<>();
        BigDecimal actualExpensesTotal = BigDecimal.ZERO;
        for (TripExpense exp : expenses) {
            String cat = exp.getCategory().toUpperCase();
            actualByCategory.merge(cat, exp.getAmount(), BigDecimal::add);
            actualExpensesTotal = actualExpensesTotal.add(exp.getAmount());
        }

        List<CostFormulaComponent> components = componentMapper.selectList(
                new LambdaQueryWrapper<CostFormulaComponent>()
                        .eq(CostFormulaComponent::getFormulaVersionId, ver.getId())
                        .orderByAsc(CostFormulaComponent::getSortOrder));

        Set<String> consumedExpenseCategories = new HashSet<>();
        List<TripCostSnapshotItem> items = new ArrayList<>();
        BigDecimal estimatedOperatingCost = BigDecimal.ZERO;
        BigDecimal allocatedOperatingCost = BigDecimal.ZERO;

        for (CostFormulaComponent comp : components) {
            if (comp.getVehicleTypeFilter() != null && !comp.getVehicleTypeFilter().isBlank()
                    && vehicleType != null && !comp.getVehicleTypeFilter().equalsIgnoreCase(vehicleType)) {
                continue;
            }

            BigDecimal est = evaluateComponentEstimate(comp, distanceKm, durationHours, agreedFare);
            String policy = comp.getReconciliationPolicy().toUpperCase();
            String linkedCat = comp.getExpenseCategoryLink() != null ? comp.getExpenseCategoryLink().toUpperCase() : null;

            BigDecimal actualForComp = BigDecimal.ZERO;
            if (linkedCat != null && !consumedExpenseCategories.contains(linkedCat)) {
                actualForComp = actualByCategory.getOrDefault(linkedCat, BigDecimal.ZERO);
                if ("ACTUAL_OVERRIDES_ESTIMATE".equals(policy) || "ACTUAL_EXPENSE_ONLY".equals(policy)) {
                    consumedExpenseCategories.add(linkedCat);
                }
            }

            BigDecimal effective;
            String detail;
            switch (policy) {
                case "ACTUAL_EXPENSE_ONLY" -> {
                    effective = actualForComp;
                    detail = "Aktual pengeluaran (" + (linkedCat != null ? linkedCat : comp.getComponentCode()) + ")";
                }
                case "ACTUAL_OVERRIDES_ESTIMATE" -> {
                    if (actualForComp.signum() > 0) {
                        // Actual replaces estimate — prevents double counting!
                        effective = actualForComp;
                        detail = "Aktual menggantikan estimasi (mencegah hitung ganda)";
                    } else {
                        effective = est;
                        allocatedOperatingCost = allocatedOperatingCost.add(est);
                        detail = "Estimasi formula digunakan karena belum ada struk aktual";
                    }
                }
                default -> { // ALLOCATED_ONLY
                    effective = est;
                    allocatedOperatingCost = allocatedOperatingCost.add(est);
                    detail = "Alokasi formula biaya operasional (" + comp.getCalcType() + ")";
                }
            }

            estimatedOperatingCost = estimatedOperatingCost.add(est);

            TripCostSnapshotItem item = new TripCostSnapshotItem();
            item.setId(IdGenerator.newId());
            item.setSnapshotId(snapshotId);
            item.setComponentCode(comp.getComponentCode());
            item.setComponentName(comp.getName());
            item.setReconciliationPolicy(policy);
            item.setEstimatedAmount(est);
            item.setActualExpenseAmount(actualForComp);
            item.setEffectiveCostAmount(effective);
            item.setCalculationDetail(detail);
            items.add(item);
        }

        // Include any unlinked actual trip expenses (e.g. DRIVER_MEAL / OTHER) so nothing is missed
        for (Map.Entry<String, BigDecimal> entry : actualByCategory.entrySet()) {
            if (!consumedExpenseCategories.contains(entry.getKey()) && entry.getValue().signum() > 0) {
                TripCostSnapshotItem unlinked = new TripCostSnapshotItem();
                unlinked.setId(IdGenerator.newId());
                unlinked.setSnapshotId(snapshotId);
                unlinked.setComponentCode(entry.getKey());
                unlinked.setComponentName("Pengeluaran Aktual Lainnya (" + entry.getKey() + ")");
                unlinked.setReconciliationPolicy("ACTUAL_EXPENSE_ONLY");
                unlinked.setEstimatedAmount(BigDecimal.ZERO);
                unlinked.setActualExpenseAmount(entry.getValue());
                unlinked.setEffectiveCostAmount(entry.getValue());
                unlinked.setCalculationDetail("Pengeluaran perjalanan aktual di luar komponen terikat");
                items.add(unlinked);
            }
        }

        estimatedOperatingCost = estimatedOperatingCost.setScale(2, RoundingMode.HALF_UP);
        actualExpensesTotal = actualExpensesTotal.setScale(2, RoundingMode.HALF_UP);
        allocatedOperatingCost = allocatedOperatingCost.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEffectiveCost = actualExpensesTotal.add(allocatedOperatingCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedProfit = agreedFare.subtract(estimatedOperatingCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualProfit = agreedFare.subtract(totalEffectiveCost).setScale(2, RoundingMode.HALF_UP);

        TripCostSnapshot snap = new TripCostSnapshot();
        snap.setId(snapshotId);
        snap.setTripId(trip.getId());
        snap.setFormulaVersionId(ver.getId());
        snap.setFormulaVersionNumber(ver.getVersionNumber());
        snap.setDistanceKmUsed(distanceKm);
        snap.setDistanceSource(distanceSource);
        snap.setDurationHoursUsed(durationHours);
        snap.setAgreedFareSnapshot(agreedFare);
        snap.setEstimatedOperatingCost(estimatedOperatingCost);
        snap.setActualExpensesTotal(actualExpensesTotal);
        snap.setAllocatedOperatingCost(allocatedOperatingCost);
        snap.setTotalEffectiveCost(totalEffectiveCost);
        snap.setEstimatedProfit(estimatedProfit);
        snap.setActualProfit(actualProfit);
        snap.setCalculatedAt(OffsetDateTime.now());

        return new ComputedCalculation(snap, items);
    }

    private BigDecimal evaluateComponentEstimate(CostFormulaComponent comp,
                                                 BigDecimal distanceKm,
                                                 BigDecimal durationHours,
                                                 BigDecimal agreedFare) {
        String type = comp.getCalcType().toUpperCase();
        BigDecimal raw = switch (type) {
            case "FIXED_PER_TRIP" -> comp.getFixedAmount() != null ? comp.getFixedAmount() : BigDecimal.ZERO;
            case "PER_KM" -> comp.getRateValue() != null ? comp.getRateValue().multiply(distanceKm) : BigDecimal.ZERO;
            case "PERCENT_OF_FARE" -> {
                if (comp.getRateValue() == null) {
                    yield BigDecimal.ZERO;
                }
                BigDecimal rate = comp.getRateValue();
                yield rate.compareTo(BigDecimal.ONE) <= 0
                        ? agreedFare.multiply(rate)
                        : agreedFare.multiply(rate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            }
            case "HOURLY_ALLOCATION" -> comp.getRateValue() != null
                    ? comp.getRateValue().multiply(durationHours) : BigDecimal.ZERO;
            case "MONTHLY_ALLOCATION" -> {
                if (comp.getMonthlyAmount() == null) {
                    yield BigDecimal.ZERO;
                }
                int tripsPerMonth = (comp.getAssumedMonthlyTrips() != null && comp.getAssumedMonthlyTrips() > 0)
                        ? comp.getAssumedMonthlyTrips() : 60;
                yield comp.getMonthlyAmount().divide(BigDecimal.valueOf(tripsPerMonth), 6, RoundingMode.HALF_UP);
            }
            case "FORMULA_ALLOCATION" -> {
                Map<String, BigDecimal> vars = buildVariables(distanceKm, durationHours, agreedFare, comp.getParametersJson());
                yield FormulaExpressionEvaluator.evaluate(comp.getExpressionFormula(), vars);
            }
            default -> BigDecimal.ZERO;
        };
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> buildVariables(BigDecimal distanceKm,
                                                   BigDecimal durationHours,
                                                   BigDecimal agreedFare,
                                                   String parametersJson) {
        Map<String, BigDecimal> vars = new HashMap<>();
        vars.put("distance_km", distanceKm != null ? distanceKm : BigDecimal.ZERO);
        vars.put("duration_hours", durationHours != null ? durationHours : BigDecimal.ZERO);
        vars.put("agreed_fare", agreedFare != null ? agreedFare : BigDecimal.ZERO);
        if (parametersJson != null && !parametersJson.isBlank()) {
            try {
                Map<String, Object> rawMap = objectMapper.readValue(parametersJson, new TypeReference<>() {});
                for (Map.Entry<String, Object> e : rawMap.entrySet()) {
                    if (e.getValue() != null) {
                        vars.put(e.getKey(), new BigDecimal(String.valueOf(e.getValue())));
                    }
                }
            } catch (Exception ex) {
                throw BusinessException.badRequest("INVALID_PARAMETERS_JSON",
                        "Format JSON parameter formula tidak valid: " + ex.getMessage());
            }
        }
        return vars;
    }

    private CostFormulaVersion resolveEffectiveFormulaVersion(LocalDate tripDate) {
        List<CostFormulaVersion> all = versionMapper.selectList(
                new LambdaQueryWrapper<CostFormulaVersion>().orderByDesc(CostFormulaVersion::getVersionNumber));
        for (CostFormulaVersion v : all) {
            boolean afterStart = v.getEffectiveFrom() == null || !tripDate.isBefore(v.getEffectiveFrom());
            boolean beforeEnd = v.getEffectiveTo() == null || !tripDate.isAfter(v.getEffectiveTo());
            if (afterStart && beforeEnd) {
                return v;
            }
        }
        return all.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .findFirst()
                .orElse(all.isEmpty() ? null : all.get(0));
    }

    private int determineNextVersionNumber() {
        CostFormulaVersion top = versionMapper.selectOne(new LambdaQueryWrapper<CostFormulaVersion>()
                .orderByDesc(CostFormulaVersion::getVersionNumber)
                .last("LIMIT 1"));
        return top != null && top.getVersionNumber() != null ? top.getVersionNumber() + 1 : 1;
    }

    private CostFormulaVersionDto toVersionDto(CostFormulaVersion v) {
        List<CostFormulaComponent> comps = componentMapper.selectList(
                new LambdaQueryWrapper<CostFormulaComponent>()
                        .eq(CostFormulaComponent::getFormulaVersionId, v.getId())
                        .orderByAsc(CostFormulaComponent::getSortOrder));
        return new CostFormulaVersionDto(
                v.getId(),
                v.getVersionNumber(),
                v.getName(),
                v.getEffectiveFrom(),
                v.getEffectiveTo(),
                v.getIsActive(),
                v.getNotes(),
                comps
        );
    }

    private record ComputedCalculation(TripCostSnapshot snapshot, List<TripCostSnapshotItem> items) {
    }
}
