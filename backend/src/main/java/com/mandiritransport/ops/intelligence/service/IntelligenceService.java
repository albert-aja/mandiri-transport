package com.mandiritransport.ops.intelligence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.booking.entity.Booking;
import com.mandiritransport.ops.booking.mapper.BookingMapper;
import com.mandiritransport.ops.common.util.GeoAndLinkUtils;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.customer.entity.Customer;
import com.mandiritransport.ops.customer.mapper.CustomerMapper;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.finance.entity.TripCostSnapshot;
import com.mandiritransport.ops.finance.entity.TripExpense;
import com.mandiritransport.ops.finance.entity.TripPayment;
import com.mandiritransport.ops.finance.mapper.TripCostSnapshotMapper;
import com.mandiritransport.ops.finance.mapper.TripExpenseMapper;
import com.mandiritransport.ops.finance.mapper.TripPaymentMapper;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.AnomalyAlertDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.AnomalyReportDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.BiExplorerKpisDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.BiExplorerResponseDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.CustomerProfitabilityDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.DimensionBreakdownDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.DriverWorkloadItemDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.OperationalQaInsightDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.RoutePerformanceDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.VehicleReplacementItemDto;
import com.mandiritransport.ops.intelligence.dto.IntelligenceDtos.WorkloadReportDto;
import com.mandiritransport.ops.intelligence.entity.AnomalyRule;
import com.mandiritransport.ops.intelligence.mapper.AnomalyRuleMapper;
import com.mandiritransport.ops.pricing.entity.CustomerPricingAgreement;
import com.mandiritransport.ops.pricing.mapper.CustomerPricingAgreementMapper;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.entity.Vehicle;
import com.mandiritransport.ops.vehicle.entity.VehicleExpense;
import com.mandiritransport.ops.vehicle.entity.VehicleMaintenanceLog;
import com.mandiritransport.ops.vehicle.mapper.RouteMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleExpenseMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMaintenanceLogMapper;
import com.mandiritransport.ops.vehicle.mapper.VehicleMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IntelligenceService {

    public static final int ROUTE_MIN_SAMPLE_SIZE = 5;

    private final TripMapper tripMapper;
    private final BookingMapper bookingMapper;
    private final CustomerMapper customerMapper;
    private final CustomerPricingAgreementMapper agreementMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleMaintenanceLogMapper maintenanceLogMapper;
    private final VehicleExpenseMapper vehicleExpenseMapper;
    private final RouteMapper routeMapper;
    private final TripCostSnapshotMapper snapshotMapper;
    private final TripExpenseMapper tripExpenseMapper;
    private final TripPaymentMapper paymentMapper;
    private final AnomalyRuleMapper anomalyRuleMapper;

    public IntelligenceService(TripMapper tripMapper,
                               BookingMapper bookingMapper,
                               CustomerMapper customerMapper,
                               CustomerPricingAgreementMapper agreementMapper,
                               DriverMapper driverMapper,
                               VehicleMapper vehicleMapper,
                               VehicleMaintenanceLogMapper maintenanceLogMapper,
                               VehicleExpenseMapper vehicleExpenseMapper,
                               RouteMapper routeMapper,
                               TripCostSnapshotMapper snapshotMapper,
                               TripExpenseMapper tripExpenseMapper,
                               TripPaymentMapper paymentMapper,
                               AnomalyRuleMapper anomalyRuleMapper) {
        this.tripMapper = tripMapper;
        this.bookingMapper = bookingMapper;
        this.customerMapper = customerMapper;
        this.agreementMapper = agreementMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.maintenanceLogMapper = maintenanceLogMapper;
        this.vehicleExpenseMapper = vehicleExpenseMapper;
        this.routeMapper = routeMapper;
        this.snapshotMapper = snapshotMapper;
        this.tripExpenseMapper = tripExpenseMapper;
        this.paymentMapper = paymentMapper;
        this.anomalyRuleMapper = anomalyRuleMapper;
    }

    // 13.1 Driver Workload Balancing
    public WorkloadReportDto getWorkloadReport(LocalDate date) {
        SecurityUtils.requireOwner();
        LocalDate target = date != null ? date : LocalDate.now(IdGenerator.WIB);
        OffsetDateTime dayStart = target.atStartOfDay(IdGenerator.WIB).toOffsetDateTime();
        OffsetDateTime dayEnd = target.plusDays(1).atStartOfDay(IdGenerator.WIB).toOffsetDateTime();

        List<Driver> drivers = driverMapper.selectList(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getIsActive, true)
                .orderByAsc(Driver::getDriverCode));

        List<Trip> tripsOnDay = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .notIn(Trip::getStatus, Set.of("CANCELLED", "NO_SHOW", "REASSIGNED"))
                .lt(Trip::getReservedStartAt, dayEnd)
                .gt(Trip::getReservedEndAt, dayStart));

        List<DriverWorkloadItemDto> items = new ArrayList<>();
        int availableCount = 0;

        for (Driver d : drivers) {
            if ("AVAILABLE".equalsIgnoreCase(d.getAvailabilityStatus())) {
                availableCount++;
            }
            int tripCount = 0;
            long reservedMins = 0;
            long activeMins = 0;
            for (Trip t : tripsOnDay) {
                if (d.getId().equals(t.getDriverId())) {
                    tripCount++;
                    OffsetDateTime s = t.getReservedStartAt().isBefore(dayStart) ? dayStart : t.getReservedStartAt();
                    OffsetDateTime e = t.getReservedEndAt().isAfter(dayEnd) ? dayEnd : t.getReservedEndAt();
                    reservedMins += Math.max(0L, Duration.between(s, e).toMinutes());
                    activeMins += t.getActualDurationMin() != null ? t.getActualDurationMin() : t.getEstimatedDurationMin();
                }
            }

            BigDecimal reservedHrs = BigDecimal.valueOf(reservedMins / 60.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal activeHrs = BigDecimal.valueOf(activeMins / 60.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal maxDaily = d.getMaxDailyHours() != null ? d.getMaxDailyHours() : new BigDecimal("10.00");
            BigDecimal utilPct = maxDaily.signum() > 0
                    ? reservedHrs.multiply(BigDecimal.valueOf(100)).divide(maxDaily, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String workloadStatus;
            String rec;
            if (!"AVAILABLE".equalsIgnoreCase(d.getAvailabilityStatus())) {
                workloadStatus = "UNAVAILABLE";
                rec = "Supir sedang berstatus " + d.getAvailabilityStatus() + "; jangan dijadwalkan.";
            } else if (utilPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                workloadStatus = "OVERLOADED";
                rec = "Melebihi batas jam harian; alihkan perjalanan baru ke supir lain.";
            } else if (utilPct.compareTo(BigDecimal.valueOf(75)) >= 0) {
                workloadStatus = "NEAR_LIMIT";
                rec = "Beban kerja mendekati kapasitas penuh; prioritaskan supir dengan jam lebih ringan.";
            } else if (tripCount > 0) {
                workloadStatus = "BALANCED";
                rec = "Beban kerja seimbang dan siap menerima penugasan tambahan bila jadwal tidak bentrok.";
            } else {
                workloadStatus = "UNDERUTILIZED";
                rec = "Prioritas utama penugasan hari ini (0 jam teralokasi).";
            }

            items.add(new DriverWorkloadItemDto(
                    d.getId(),
                    d.getDriverCode(),
                    d.getFullName(),
                    d.getAvailabilityStatus(),
                    maxDaily,
                    tripCount,
                    reservedHrs,
                    activeHrs,
                    utilPct,
                    workloadStatus,
                    rec
            ));
        }

        return new WorkloadReportDto(target, drivers.size(), availableCount, items);
    }

    // 13.2 Route Performance History (minSampleSize = 5 guard)
    public List<RoutePerformanceDto> getRoutePerformance(String routeIdFilter, String vehicleTypeFilter) {
        SecurityUtils.requireOwner();
        LambdaQueryWrapper<Route> rQw = new LambdaQueryWrapper<Route>()
                .eq(Route::getIsActive, true)
                .orderByAsc(Route::getName);
        if (routeIdFilter != null && !routeIdFilter.isBlank()) {
            rQw.eq(Route::getId, routeIdFilter);
        }
        List<Route> routes = routeMapper.selectList(rQw);

        Map<String, Vehicle> vehicleMap = vehicleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v, (a, b) -> a));
        Map<String, TripCostSnapshot> snapByTrip = snapshotMapper.selectList(null).stream()
                .collect(Collectors.toMap(TripCostSnapshot::getTripId, s -> s, (a, b) -> a));

        Map<String, BigDecimal> fuelByTrip = new HashMap<>();
        for (TripExpense exp : tripExpenseMapper.selectList(new LambdaQueryWrapper<TripExpense>()
                .eq(TripExpense::getCategory, "FUEL")
                .eq(TripExpense::getIsVoided, false))) {
            fuelByTrip.merge(exp.getTripId(), exp.getAmount(), BigDecimal::add);
        }

        List<Trip> completedTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getStatus, "COMPLETED"));

        List<RoutePerformanceDto> result = new ArrayList<>();
        for (Route r : routes) {
            List<Trip> matching = completedTrips.stream()
                    .filter(t -> r.getId().equals(t.getRouteId()))
                    .filter(t -> {
                        if (vehicleTypeFilter == null || vehicleTypeFilter.isBlank()) {
                            return true;
                        }
                        Vehicle v = t.getVehicleId() != null ? vehicleMap.get(t.getVehicleId()) : null;
                        return v != null && vehicleTypeFilter.equalsIgnoreCase(v.getVehicleType());
                    })
                    .toList();

            int n = matching.size();
            boolean sufficient = n >= ROUTE_MIN_SAMPLE_SIZE;

            if (n == 0) {
                result.add(new RoutePerformanceDto(
                        r.getId(), r.getCode(), r.getName(), 0, ROUTE_MIN_SAMPLE_SIZE, false,
                        BigDecimal.ZERO, 0, 0, r.getTypicalDistanceKm(),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        "Data belum mencukupi untuk analisis rute (0 dari minimal " + ROUTE_MIN_SAMPLE_SIZE
                                + " perjalanan selesai)."
                ));
                continue;
            }

            int minDur = Integer.MAX_VALUE;
            int maxDur = 0;
            long sumDur = 0;
            BigDecimal sumDist = BigDecimal.ZERO;
            BigDecimal sumFuel = BigDecimal.ZERO;
            BigDecimal sumCost = BigDecimal.ZERO;
            BigDecimal sumFare = BigDecimal.ZERO;
            BigDecimal sumProfit = BigDecimal.ZERO;

            for (Trip t : matching) {
                int dur = t.getActualDurationMin() != null ? t.getActualDurationMin() : t.getEstimatedDurationMin();
                minDur = Math.min(minDur, dur);
                maxDur = Math.max(maxDur, dur);
                sumDur += dur;

                TripCostSnapshot snap = snapByTrip.get(t.getId());
                BigDecimal dist = snap != null ? snap.getDistanceKmUsed() : r.getTypicalDistanceKm();
                BigDecimal effCost = snap != null ? snap.getTotalEffectiveCost() : BigDecimal.ZERO;
                BigDecimal fare = t.getAgreedFareSnapshot() != null ? t.getAgreedFareSnapshot() : BigDecimal.ZERO;
                BigDecimal profit = snap != null ? snap.getActualProfit() : fare.subtract(effCost);

                sumDist = sumDist.add(dist);
                sumFuel = sumFuel.add(fuelByTrip.getOrDefault(t.getId(), BigDecimal.ZERO));
                sumCost = sumCost.add(effCost);
                sumFare = sumFare.add(fare);
                sumProfit = sumProfit.add(profit);
            }

            BigDecimal divisor = BigDecimal.valueOf(n);
            BigDecimal avgDur = BigDecimal.valueOf(sumDur).divide(divisor, 1, RoundingMode.HALF_UP);
            BigDecimal avgDist = sumDist.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal avgFuel = sumFuel.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal avgCost = sumCost.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal avgFare = sumFare.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal avgProfit = sumProfit.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal marginPct = sumFare.signum() > 0
                    ? sumProfit.multiply(BigDecimal.valueOf(100)).divide(sumFare, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String explanation = sufficient
                    ? "Sampel memadai (" + n + " perjalanan selesai). Rata-rata laba bersih "
                    + GeoAndLinkUtils.formatIdr(avgProfit) + " per perjalanan (margin " + marginPct + "%)."
                    : "Data belum mencukupi untuk kesimpulan statistik (" + n + " dari minimal "
                    + ROUTE_MIN_SAMPLE_SIZE + " perjalanan selesai). Angka hanya referensi awal.";

            result.add(new RoutePerformanceDto(
                    r.getId(), r.getCode(), r.getName(), n, ROUTE_MIN_SAMPLE_SIZE, sufficient,
                    avgDur, minDur, maxDur, avgDist, avgFuel, avgCost, avgFare, avgProfit, marginPct, explanation
            ));
        }

        return result;
    }

    // 13.5 Respectful Customer Profitability Analysis
    public List<CustomerProfitabilityDto> getCustomerProfitability(LocalDate dateFrom, LocalDate dateTo) {
        SecurityUtils.requireOwner();
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getIsActive, true)
                .orderByAsc(Customer::getName));

        Map<String, Booking> bookingMap = bookingMapper.selectList(null).stream()
                .collect(Collectors.toMap(Booking::getId, b -> b, (a, b) -> a));
        Map<String, TripCostSnapshot> snapByTrip = snapshotMapper.selectList(null).stream()
                .collect(Collectors.toMap(TripCostSnapshot::getTripId, s -> s, (a, b) -> a));
        Set<String> custWithAgreements = agreementMapper.selectList(new LambdaQueryWrapper<CustomerPricingAgreement>()
                        .eq(CustomerPricingAgreement::getIsActive, true))
                .stream()
                .map(CustomerPricingAgreement::getCustomerId)
                .collect(Collectors.toSet());

        List<Trip> trips = filterTripsByDate(tripMapper.selectList(null), dateFrom, dateTo);

        List<CustomerProfitabilityDto> out = new ArrayList<>();
        for (Customer c : customers) {
            List<Trip> custTrips = trips.stream()
                    .filter(t -> {
                        Booking b = bookingMap.get(t.getBookingId());
                        return b != null && c.getId().equals(b.getCustomerId());
                    })
                    .toList();

            int totalTrips = custTrips.size();
            int completed = 0;
            int cancelledOrNoShow = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;
            BigDecimal profit = BigDecimal.ZERO;
            BigDecimal unpaid = BigDecimal.ZERO;

            for (Trip t : custTrips) {
                if ("COMPLETED".equals(t.getStatus())) {
                    completed++;
                    BigDecimal fare = t.getAgreedFareSnapshot() != null ? t.getAgreedFareSnapshot() : BigDecimal.ZERO;
                    TripCostSnapshot snap = snapByTrip.get(t.getId());
                    BigDecimal effCost = snap != null ? snap.getTotalEffectiveCost() : BigDecimal.ZERO;
                    BigDecimal actProfit = snap != null ? snap.getActualProfit() : fare.subtract(effCost);
                    revenue = revenue.add(fare);
                    cost = cost.add(effCost);
                    profit = profit.add(actProfit);
                } else if ("CANCELLED".equals(t.getStatus()) || "NO_SHOW".equals(t.getStatus())) {
                    cancelledOrNoShow++;
                }

                if (!"CANCELLED".equals(t.getStatus()) && !"NO_SHOW".equals(t.getStatus())) {
                    BigDecimal fare = t.getAgreedFareSnapshot() != null ? t.getAgreedFareSnapshot() : BigDecimal.ZERO;
                    BigDecimal paid = t.getTotalPaidAmount() != null ? t.getTotalPaidAmount() : BigDecimal.ZERO;
                    BigDecimal rem = fare.subtract(paid);
                    if (rem.signum() > 0) {
                        unpaid = unpaid.add(rem);
                    }
                }
            }

            BigDecimal avgProfit = completed > 0
                    ? profit.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal marginPct = revenue.signum() > 0
                    ? profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            boolean hasAgr = custWithAgreements.contains(c.getId());

            String insight;
            if (completed == 0) {
                insight = "Belum ada perjalanan selesai pada periode terpilih.";
            } else if (marginPct.compareTo(BigDecimal.valueOf(25)) < 0 && hasAgr) {
                insight = "Pelanggan setia dengan kesepakatan tarif khusus; margin rata-rata " + marginPct
                        + "% dapat ditinjau berkala secara santun apabila biaya BBM/tol meningkat.";
            } else {
                insight = "Kontribusi margin sehat (" + marginPct + "%) dengan hubungan operasional yang baik.";
            }

            out.add(new CustomerProfitabilityDto(
                    c.getId(),
                    c.getName(),
                    c.getCompanyName(),
                    totalTrips,
                    completed,
                    cancelledOrNoShow,
                    revenue,
                    cost,
                    profit,
                    avgProfit,
                    marginPct,
                    unpaid,
                    hasAgr,
                    insight
            ));
        }

        out.sort(Comparator.comparing(CustomerProfitabilityDto::totalActualProfit).reversed());
        return out;
    }

    // 13.6 Operational Anomaly Detection (sample-size guarded, neutral review hints)
    public AnomalyReportDto getAnomalies() {
        SecurityUtils.requireOwner();
        Map<String, AnomalyRule> rules = anomalyRuleMapper.selectList(new LambdaQueryWrapper<AnomalyRule>()
                        .eq(AnomalyRule::getEnabled, true))
                .stream()
                .collect(Collectors.toMap(AnomalyRule::getRuleCode, r -> r, (a, b) -> a));

        Map<String, Route> routeMap = routeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Route::getId, r -> r, (a, b) -> a));
        Map<String, Driver> driverMap = driverMapper.selectList(null).stream()
                .collect(Collectors.toMap(Driver::getId, d -> d, (a, b) -> a));
        Map<String, Vehicle> vehicleMap = vehicleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v, (a, b) -> a));
        Map<String, Trip> tripMap = tripMapper.selectList(null).stream()
                .collect(Collectors.toMap(Trip::getId, t -> t, (a, b) -> a));

        List<Trip> completedTrips = tripMap.values().stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .toList();

        List<TripExpense> allActiveExpenses = tripExpenseMapper.selectList(new LambdaQueryWrapper<TripExpense>()
                .eq(TripExpense::getIsVoided, false));
        Map<String, BigDecimal> fuelByTrip = new HashMap<>();
        for (TripExpense e : allActiveExpenses) {
            if ("FUEL".equalsIgnoreCase(e.getCategory())) {
                fuelByTrip.merge(e.getTripId(), e.getAmount(), BigDecimal::add);
            }
        }

        List<AnomalyAlertDto> alerts = new ArrayList<>();

        // 1. HIGH_FUEL_EXPENSE (guarded by minSampleSize per route)
        AnomalyRule fuelRule = rules.get("HIGH_FUEL_EXPENSE");
        if (fuelRule != null) {
            int minN = fuelRule.getMinSampleSize() != null ? fuelRule.getMinSampleSize() : 5;
            BigDecimal mult = fuelRule.getThresholdMultiplier() != null ? fuelRule.getThresholdMultiplier() : new BigDecimal("1.40");
            Map<String, List<Trip>> byRoute = completedTrips.stream()
                    .filter(t -> t.getRouteId() != null && fuelByTrip.containsKey(t.getId()))
                    .collect(Collectors.groupingBy(Trip::getRouteId));

            for (Map.Entry<String, List<Trip>> entry : byRoute.entrySet()) {
                List<Trip> rTrips = entry.getValue();
                if (rTrips.size() < minN) {
                    continue; // Sample size guard!
                }
                BigDecimal sum = rTrips.stream()
                        .map(t -> fuelByTrip.getOrDefault(t.getId(), BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(BigDecimal.valueOf(rTrips.size()), 2, RoundingMode.HALF_UP);
                BigDecimal threshold = avg.multiply(mult).setScale(2, RoundingMode.HALF_UP);
                Route route = routeMap.get(entry.getKey());

                for (Trip t : rTrips) {
                    BigDecimal obs = fuelByTrip.getOrDefault(t.getId(), BigDecimal.ZERO);
                    if (obs.compareTo(threshold) > 0) {
                        alerts.add(new AnomalyAlertDto(
                                fuelRule.getRuleCode(),
                                fuelRule.getName(),
                                "TRIP",
                                t.getId(),
                                t.getTripCode() + (route != null ? " (" + route.getName() + ")" : ""),
                                obs,
                                avg,
                                threshold,
                                "Biaya BBM tercatat " + GeoAndLinkUtils.formatIdr(obs) + ", melebihi ambang "
                                        + GeoAndLinkUtils.formatIdr(threshold) + " (rata-rata rute " + GeoAndLinkUtils.formatIdr(avg) + ").",
                                "Tinjau catatan kondisi lalu lintas/kemacetan atau struk SPBU bersama supir untuk konfirmasi wajar."
                        ));
                    }
                }
            }
        }

        // 2. LONG_TRIP_DURATION (guarded by minSampleSize per route)
        AnomalyRule durRule = rules.get("LONG_TRIP_DURATION");
        if (durRule != null) {
            int minN = durRule.getMinSampleSize() != null ? durRule.getMinSampleSize() : 5;
            BigDecimal mult = durRule.getThresholdMultiplier() != null ? durRule.getThresholdMultiplier() : new BigDecimal("1.50");
            Map<String, List<Trip>> byRoute = completedTrips.stream()
                    .filter(t -> t.getRouteId() != null && t.getActualDurationMin() != null)
                    .collect(Collectors.groupingBy(Trip::getRouteId));

            for (Map.Entry<String, List<Trip>> entry : byRoute.entrySet()) {
                List<Trip> rTrips = entry.getValue();
                if (rTrips.size() < minN) {
                    continue; // Sample size guard!
                }
                BigDecimal sum = rTrips.stream()
                        .map(t -> BigDecimal.valueOf(t.getActualDurationMin()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(BigDecimal.valueOf(rTrips.size()), 1, RoundingMode.HALF_UP);
                BigDecimal threshold = avg.multiply(mult).setScale(1, RoundingMode.HALF_UP);
                Route route = routeMap.get(entry.getKey());

                for (Trip t : rTrips) {
                    BigDecimal obs = BigDecimal.valueOf(t.getActualDurationMin());
                    if (obs.compareTo(threshold) > 0) {
                        alerts.add(new AnomalyAlertDto(
                                durRule.getRuleCode(),
                                durRule.getName(),
                                "TRIP",
                                t.getId(),
                                t.getTripCode() + (route != null ? " (" + route.getName() + ")" : ""),
                                obs,
                                avg,
                                threshold,
                                "Durasi perjalanan " + obs.intValue() + " menit melebihi ambang "
                                        + threshold + " menit (rata-rata rute " + avg + " menit).",
                                "Periksa apakah terdapat penundaan penerbangan, pengalihan jalur, atau waktu tunggu tambahan pelanggan."
                        ));
                    }
                }
            }
        }

        // 3. UNEXPECTED_TRIP_EXPENSE
        AnomalyRule expRule = rules.get("UNEXPECTED_TRIP_EXPENSE");
        if (expRule != null && expRule.getThresholdAbsolute() != null) {
            BigDecimal threshold = expRule.getThresholdAbsolute();
            for (TripExpense e : allActiveExpenses) {
                if (!"FUEL".equalsIgnoreCase(e.getCategory()) && e.getAmount().compareTo(threshold) > 0) {
                    Trip t = tripMap.get(e.getTripId());
                    alerts.add(new AnomalyAlertDto(
                            expRule.getRuleCode(),
                            expRule.getName(),
                            "TRIP_EXPENSE",
                            e.getId(),
                            (t != null ? t.getTripCode() : e.getTripId()) + " - " + e.getCategory(),
                            e.getAmount(),
                            threshold,
                            threshold,
                            "Pengeluaran non-BBM (" + e.getCategory() + ") sebesar "
                                    + GeoAndLinkUtils.formatIdr(e.getAmount()) + " di atas ambang tinjauan.",
                            "Pastikan bukti pembayaran tersimpan dan cek apakah biaya dapat ditagihkan kembali sesuai kesepakatan."
                    ));
                }
            }
        }

        // 4. UNUSUAL_MAINTENANCE_COST
        AnomalyRule maintRule = rules.get("UNUSUAL_MAINTENANCE_COST");
        if (maintRule != null) {
            List<VehicleMaintenanceLog> logs = maintenanceLogMapper.selectList(null);
            int minN = maintRule.getMinSampleSize() != null ? maintRule.getMinSampleSize() : 3;
            if (logs.size() >= minN) {
                BigDecimal sum = logs.stream().map(VehicleMaintenanceLog::getCostAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(BigDecimal.valueOf(logs.size()), 2, RoundingMode.HALF_UP);
                BigDecimal absThresh = maintRule.getThresholdAbsolute() != null
                        ? maintRule.getThresholdAbsolute() : new BigDecimal("4000000");
                for (VehicleMaintenanceLog m : logs) {
                    if (m.getCostAmount().compareTo(absThresh) > 0) {
                        Vehicle v = vehicleMap.get(m.getVehicleId());
                        alerts.add(new AnomalyAlertDto(
                                maintRule.getRuleCode(),
                                maintRule.getName(),
                                "VEHICLE_MAINTENANCE",
                                m.getId(),
                                (v != null ? v.getRegistrationNumber() + " " + v.getNameModel() : m.getVehicleId()),
                                m.getCostAmount(),
                                avg,
                                absThresh,
                                "Biaya perawatan/perbaikan " + m.getMaintenanceType() + " mencapai "
                                        + GeoAndLinkUtils.formatIdr(m.getCostAmount()) + ".",
                                "Evaluasi riwayat perbaikan berulang pada unit terkait untuk perencanaan peremajaan armada."
                        ));
                    }
                }
            }
        }

        // 5. AGED_DRIVER_CASH
        AnomalyRule cashRule = rules.get("AGED_DRIVER_CASH");
        if (cashRule != null) {
            BigDecimal absThresh = cashRule.getThresholdAbsolute() != null
                    ? cashRule.getThresholdAbsolute() : new BigDecimal("300000");
            OffsetDateTime cutoff = OffsetDateTime.now().minusHours(24);
            List<TripPayment> unsettled = paymentMapper.selectList(new LambdaQueryWrapper<TripPayment>()
                    .eq(TripPayment::getCustodySettled, false)
                    .eq(TripPayment::getIsVoided, false)
                    .eq(TripPayment::getIsRefund, false));

            for (TripPayment p : unsettled) {
                BigDecimal rem = p.getAmount().subtract(p.getHandedOverAmount() != null ? p.getHandedOverAmount() : BigDecimal.ZERO);
                boolean aged = p.getPaidAt() != null && p.getPaidAt().isBefore(cutoff);
                boolean highAmount = rem.compareTo(absThresh) > 0;
                if (rem.signum() > 0 && (aged || highAmount)) {
                    Driver d = p.getHeldByDriverId() != null ? driverMap.get(p.getHeldByDriverId()) : null;
                    Trip t = tripMap.get(p.getTripId());
                    alerts.add(new AnomalyAlertDto(
                            cashRule.getRuleCode(),
                            cashRule.getName(),
                            "TRIP_PAYMENT",
                            p.getId(),
                            (d != null ? d.getFullName() : "Supir") + " - " + (t != null ? t.getTripCode() : p.getTripId()),
                            rem,
                            BigDecimal.ZERO,
                            absThresh,
                            "Uang tunai pelanggan sebesar " + GeoAndLinkUtils.formatIdr(rem)
                                    + " masih menunggu setoran/konfirmasi" + (aged ? " (> 24 jam)." : "."),
                            "Jadwalkan serah terima setoran tunai saat supir kembali ke garasi."
                    ));
                }
            }
        }

        // 6. COST_ESTIMATE_VARIANCE
        AnomalyRule varRule = rules.get("COST_ESTIMATE_VARIANCE");
        if (varRule != null && varRule.getThresholdMultiplier() != null) {
            BigDecimal mult = varRule.getThresholdMultiplier();
            for (TripCostSnapshot s : snapshotMapper.selectList(null)) {
                if (s.getEstimatedOperatingCost().signum() > 0) {
                    BigDecimal threshold = s.getEstimatedOperatingCost().multiply(mult).setScale(2, RoundingMode.HALF_UP);
                    if (s.getTotalEffectiveCost().compareTo(threshold) > 0) {
                        Trip t = tripMap.get(s.getTripId());
                        alerts.add(new AnomalyAlertDto(
                                varRule.getRuleCode(),
                                varRule.getName(),
                                "TRIP",
                                s.getTripId(),
                                t != null ? t.getTripCode() : s.getTripId(),
                                s.getTotalEffectiveCost(),
                                s.getEstimatedOperatingCost(),
                                threshold,
                                "Biaya efektif perjalanan (" + GeoAndLinkUtils.formatIdr(s.getTotalEffectiveCost())
                                        + ") melampaui 1,35x estimasi formula (" + GeoAndLinkUtils.formatIdr(s.getEstimatedOperatingCost()) + ").",
                                "Tinjau komponen tol/BBM aktual untuk memastikan asumsi formula biaya tetap akurat."
                        ));
                    }
                }
            }
        }

        return new AnomalyReportDto(alerts.size(), alerts);
    }

    // 13.7 Vehicle Replacement Intelligence
    public List<VehicleReplacementItemDto> getVehicleReplacementAnalysis() {
        SecurityUtils.requireOwner();
        List<Vehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>()
                .orderByAsc(Vehicle::getRegistrationNumber));
        List<Trip> completedTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getStatus, "COMPLETED"));
        Map<String, TripCostSnapshot> snapByTrip = snapshotMapper.selectList(null).stream()
                .collect(Collectors.toMap(TripCostSnapshot::getTripId, s -> s, (a, b) -> a));

        List<VehicleMaintenanceLog> maintLogs = maintenanceLogMapper.selectList(null);
        List<VehicleExpense> vehExpenses = vehicleExpenseMapper.selectList(new LambdaQueryWrapper<VehicleExpense>()
                .eq(VehicleExpense::getIsVoided, false));

        List<VehicleReplacementItemDto> out = new ArrayList<>();
        for (Vehicle v : vehicles) {
            List<Trip> vTrips = completedTrips.stream().filter(t -> v.getId().equals(t.getVehicleId())).toList();
            BigDecimal rev = BigDecimal.ZERO;
            BigDecimal tripOpCost = BigDecimal.ZERO;
            BigDecimal tripKm = BigDecimal.ZERO;
            for (Trip t : vTrips) {
                rev = rev.add(t.getAgreedFareSnapshot() != null ? t.getAgreedFareSnapshot() : BigDecimal.ZERO);
                TripCostSnapshot snap = snapByTrip.get(t.getId());
                if (snap != null) {
                    tripOpCost = tripOpCost.add(snap.getTotalEffectiveCost());
                    tripKm = tripKm.add(snap.getDistanceKmUsed());
                }
            }

            BigDecimal nonTripExp = vehExpenses.stream()
                    .filter(e -> v.getId().equals(e.getVehicleId()))
                    .map(VehicleExpense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<VehicleMaintenanceLog> vLogs = maintLogs.stream()
                    .filter(m -> v.getId().equals(m.getVehicleId()))
                    .toList();
            BigDecimal maintTotal = vLogs.stream()
                    .map(VehicleMaintenanceLog::getCostAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int repairCount = (int) vLogs.stream()
                    .filter(m -> "REPAIR".equalsIgnoreCase(m.getMaintenanceType()))
                    .count();
            BigDecimal downtimeHrs = vLogs.stream()
                    .map(m -> m.getDowntimeHours() != null ? m.getDowntimeHours() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal odo = v.getCurrentOdometerKm() != null ? v.getCurrentOdometerKm() : BigDecimal.ONE;
            BigDecimal denomKm = tripKm.compareTo(new BigDecimal("100")) >= 0 ? tripKm : odo.max(BigDecimal.ONE);
            BigDecimal maintPerKm = maintTotal.divide(denomKm, 2, RoundingMode.HALF_UP);
            BigDecimal netContribution = rev.subtract(tripOpCost).subtract(nonTripExp);

            String signal;
            String summary;
            if (repairCount >= 2 || downtimeHrs.compareTo(BigDecimal.valueOf(24)) >= 0
                    || maintTotal.compareTo(new BigDecimal("8000000")) >= 0) {
                signal = "EVALUATE_REPLACEMENT";
                summary = "Perlu evaluasi peremajaan/penggantian unit: biaya perawatan mencapai "
                        + GeoAndLinkUtils.formatIdr(maintTotal) + " dengan " + repairCount
                        + " perbaikan besar dan downtime " + downtimeHrs + " jam.";
            } else if (maintTotal.compareTo(new BigDecimal("3000000")) >= 0) {
                signal = "MONITOR_MAINTENANCE";
                summary = "Kondisi operasional baik dengan pengeluaran perawatan terjadwal (mis. penggantian ban/servis); pantau berkala.";
            } else {
                signal = "HEALTHY_KEEP";
                summary = "Unit produktif dengan kontribusi bersih positif (" + GeoAndLinkUtils.formatIdr(netContribution)
                        + ") dan downtime rendah.";
            }

            out.add(new VehicleReplacementItemDto(
                    v.getId(),
                    v.getRegistrationNumber(),
                    v.getNameModel(),
                    v.getManufactureYear(),
                    v.getStatus(),
                    v.getCurrentOdometerKm(),
                    vTrips.size(),
                    rev,
                    tripOpCost,
                    nonTripExp,
                    maintTotal,
                    repairCount,
                    downtimeHrs,
                    maintPerKm,
                    netContribution,
                    signal,
                    summary
            ));
        }

        return out;
    }

    // Section 14 Central Cross-Filtered BI Explorer
    public BiExplorerResponseDto getExplorer(LocalDate dateFrom,
                                             LocalDate dateTo,
                                             String driverId,
                                             String vehicleId,
                                             String customerId,
                                             String routeId) {
        SecurityUtils.requireOwner();

        Map<String, Booking> bookingMap = bookingMapper.selectList(null).stream()
                .collect(Collectors.toMap(Booking::getId, b -> b, (a, b) -> a));
        Map<String, Customer> customerMap = customerMapper.selectList(null).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
        Map<String, Driver> driverMap = driverMapper.selectList(null).stream()
                .collect(Collectors.toMap(Driver::getId, d -> d, (a, b) -> a));
        Map<String, Vehicle> vehicleMap = vehicleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v, (a, b) -> a));
        Map<String, Route> routeMap = routeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Route::getId, r -> r, (a, b) -> a));
        Map<String, TripCostSnapshot> snapByTrip = snapshotMapper.selectList(null).stream()
                .collect(Collectors.toMap(TripCostSnapshot::getTripId, s -> s, (a, b) -> a));

        List<Trip> filteredTrips = filterTripsByDate(tripMapper.selectList(null), dateFrom, dateTo).stream()
                .filter(t -> driverId == null || driverId.isBlank() || driverId.equals(t.getDriverId()))
                .filter(t -> vehicleId == null || vehicleId.isBlank() || vehicleId.equals(t.getVehicleId()))
                .filter(t -> routeId == null || routeId.isBlank() || routeId.equals(t.getRouteId()))
                .filter(t -> {
                    if (customerId == null || customerId.isBlank()) {
                        return true;
                    }
                    Booking b = bookingMap.get(t.getBookingId());
                    return b != null && customerId.equals(b.getCustomerId());
                })
                .toList();

        int totalTrips = filteredTrips.size();
        int completedCount = 0;
        int cancelledCount = 0;
        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal collectedPayments = BigDecimal.ZERO;
        BigDecimal outstandingReceivables = BigDecimal.ZERO;
        BigDecimal driverHeldCash = BigDecimal.ZERO;
        BigDecimal tripOperatingCost = BigDecimal.ZERO;
        BigDecimal netOperatingProfit = BigDecimal.ZERO;

        Map<String, Acc> routeAcc = new HashMap<>();
        Map<String, Acc> vehicleAcc = new HashMap<>();
        Map<String, Acc> driverAcc = new HashMap<>();
        Map<String, Acc> customerAcc = new HashMap<>();

        for (Trip t : filteredTrips) {
            collectedPayments = collectedPayments.add(t.getTotalPaidAmount() != null ? t.getTotalPaidAmount() : BigDecimal.ZERO);
            driverHeldCash = driverHeldCash.add(t.getDriverCashHeldAmount() != null ? t.getDriverCashHeldAmount() : BigDecimal.ZERO);

            if ("CANCELLED".equals(t.getStatus()) || "NO_SHOW".equals(t.getStatus())) {
                cancelledCount++;
                continue;
            }

            BigDecimal fare = t.getAgreedFareSnapshot() != null ? t.getAgreedFareSnapshot() : BigDecimal.ZERO;
            BigDecimal paid = t.getTotalPaidAmount() != null ? t.getTotalPaidAmount() : BigDecimal.ZERO;
            if (fare.compareTo(paid) > 0) {
                outstandingReceivables = outstandingReceivables.add(fare.subtract(paid));
            }

            if ("COMPLETED".equals(t.getStatus())) {
                completedCount++;
                TripCostSnapshot snap = snapByTrip.get(t.getId());
                BigDecimal effCost = snap != null ? snap.getTotalEffectiveCost() : BigDecimal.ZERO;
                BigDecimal profit = snap != null ? snap.getActualProfit() : fare.subtract(effCost);

                grossRevenue = grossRevenue.add(fare);
                tripOperatingCost = tripOperatingCost.add(effCost);
                netOperatingProfit = netOperatingProfit.add(profit);

                if (t.getRouteId() != null) {
                    Route r = routeMap.get(t.getRouteId());
                    routeAcc.computeIfAbsent(t.getRouteId(), id -> new Acc(id, r != null ? r.getName() : id))
                            .add(fare, effCost, profit);
                }
                if (t.getVehicleId() != null) {
                    Vehicle v = vehicleMap.get(t.getVehicleId());
                    vehicleAcc.computeIfAbsent(t.getVehicleId(), id -> new Acc(id, v != null ? v.getRegistrationNumber() + " - " + v.getNameModel() : id))
                            .add(fare, effCost, profit);
                }
                if (t.getDriverId() != null) {
                    Driver d = driverMap.get(t.getDriverId());
                    driverAcc.computeIfAbsent(t.getDriverId(), id -> new Acc(id, d != null ? d.getFullName() : id))
                            .add(fare, effCost, profit);
                }
                Booking b = bookingMap.get(t.getBookingId());
                if (b != null && b.getCustomerId() != null) {
                    Customer c = customerMap.get(b.getCustomerId());
                    customerAcc.computeIfAbsent(b.getCustomerId(), id -> new Acc(id, c != null ? c.getName() : id))
                            .add(fare, effCost, profit);
                }
            }
        }

        // Non-trip vehicle expenses filtered by date & vehicleId (when applicable)
        BigDecimal vehMaintExpenses = vehicleExpenseMapper.selectList(new LambdaQueryWrapper<VehicleExpense>()
                        .eq(VehicleExpense::getIsVoided, false))
                .stream()
                .filter(e -> vehicleId == null || vehicleId.isBlank() || vehicleId.equals(e.getVehicleId()))
                .filter(e -> dateFrom == null || !e.getExpenseDate().isBefore(dateFrom))
                .filter(e -> dateTo == null || !e.getExpenseDate().isAfter(dateTo))
                .map(VehicleExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fleetProfitAfterVehExp = netOperatingProfit.subtract(vehMaintExpenses);

        List<DimensionBreakdownDto> byRoute = toSortedList(routeAcc);
        List<DimensionBreakdownDto> byVehicle = toSortedList(vehicleAcc);
        List<DimensionBreakdownDto> byDriver = toSortedList(driverAcc);
        List<DimensionBreakdownDto> byCustomer = toSortedList(customerAcc);

        List<OperationalQaInsightDto> qa = new ArrayList<>();
        if (!byRoute.isEmpty()) {
            DimensionBreakdownDto topRoute = byRoute.get(0);
            qa.add(new OperationalQaInsightDto(
                    "Rute mana dengan kontribusi laba operasional tertinggi?",
                    topRoute.label() + " menghasilkan laba bersih " + GeoAndLinkUtils.formatIdr(topRoute.profit()),
                    topRoute.completedTrips() + " perjalanan selesai dari total pendapatan " + GeoAndLinkUtils.formatIdr(topRoute.revenue())
            ));
        }
        if (!byCustomer.isEmpty()) {
            DimensionBreakdownDto topCust = byCustomer.get(0);
            qa.add(new OperationalQaInsightDto(
                    "Pelanggan mana yang paling berkontribusi pada laba periode ini?",
                    topCust.label() + " (" + GeoAndLinkUtils.formatIdr(topCust.profit()) + ")",
                    topCust.completedTrips() + " perjalanan selesai"
            ));
        }
        qa.add(new OperationalQaInsightDto(
                "Berapa posisi kas tunai di tangan supir & piutang belum terbayar?",
                "Tunai di supir: " + GeoAndLinkUtils.formatIdr(driverHeldCash)
                        + " | Piutang belum lunas: " + GeoAndLinkUtils.formatIdr(outstandingReceivables),
                "Total pembayaran masuk terkonfirmasi: " + GeoAndLinkUtils.formatIdr(collectedPayments)
        ));

        Map<String, String> filters = new LinkedHashMap<>();
        if (dateFrom != null) filters.put("dateFrom", dateFrom.toString());
        if (dateTo != null) filters.put("dateTo", dateTo.toString());
        if (driverId != null && !driverId.isBlank()) filters.put("driverId", driverId);
        if (vehicleId != null && !vehicleId.isBlank()) filters.put("vehicleId", vehicleId);
        if (customerId != null && !customerId.isBlank()) filters.put("customerId", customerId);
        if (routeId != null && !routeId.isBlank()) filters.put("routeId", routeId);

        BiExplorerKpisDto kpis = new BiExplorerKpisDto(
                totalTrips,
                completedCount,
                cancelledCount,
                grossRevenue,
                collectedPayments,
                outstandingReceivables,
                driverHeldCash,
                tripOperatingCost,
                vehMaintExpenses,
                netOperatingProfit,
                fleetProfitAfterVehExp
        );

        return new BiExplorerResponseDto(filters, kpis, byRoute, byVehicle, byDriver, byCustomer, qa);
    }

    private List<Trip> filterTripsByDate(List<Trip> trips, LocalDate from, LocalDate to) {
        return trips.stream()
                .filter(t -> {
                    if (t.getScheduledPickupAt() == null) {
                        return false;
                    }
                    LocalDate d = t.getScheduledPickupAt().atZoneSameInstant(IdGenerator.WIB).toLocalDate();
                    if (from != null && d.isBefore(from)) {
                        return false;
                    }
                    if (to != null && d.isAfter(to)) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private List<DimensionBreakdownDto> toSortedList(Map<String, Acc> map) {
        return map.values().stream()
                .map(a -> new DimensionBreakdownDto(a.id, a.label, a.trips, a.rev, a.cost, a.profit))
                .sorted(Comparator.comparing(DimensionBreakdownDto::profit).reversed())
                .toList();
    }

    private static final class Acc {
        final String id;
        final String label;
        int trips = 0;
        BigDecimal rev = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;

        Acc(String id, String label) {
            this.id = id;
            this.label = label;
        }

        void add(BigDecimal r, BigDecimal c, BigDecimal p) {
            this.trips++;
            this.rev = this.rev.add(r);
            this.cost = this.cost.add(c);
            this.profit = this.profit.add(p);
        }
    }
}
