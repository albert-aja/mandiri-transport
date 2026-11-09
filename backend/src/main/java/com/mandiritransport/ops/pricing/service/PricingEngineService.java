package com.mandiritransport.ops.pricing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.GeoAndLinkUtils;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.customer.entity.Customer;
import com.mandiritransport.ops.customer.mapper.CustomerMapper;
import com.mandiritransport.ops.pricing.dto.PricingDtos.CustomerPricingAgreementRequest;
import com.mandiritransport.ops.pricing.dto.PricingDtos.PricingResolveRequest;
import com.mandiritransport.ops.pricing.dto.PricingDtos.PricingResolveResponse;
import com.mandiritransport.ops.pricing.dto.PricingDtos.RoutePricingRuleRequest;
import com.mandiritransport.ops.pricing.entity.CustomerPricingAgreement;
import com.mandiritransport.ops.pricing.entity.RoutePricingRule;
import com.mandiritransport.ops.pricing.entity.StandardPricingConfig;
import com.mandiritransport.ops.pricing.mapper.CustomerPricingAgreementMapper;
import com.mandiritransport.ops.pricing.mapper.RoutePricingRuleMapper;
import com.mandiritransport.ops.pricing.mapper.StandardPricingConfigMapper;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.mapper.RouteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class PricingEngineService {

    private final CustomerPricingAgreementMapper agreementMapper;
    private final RoutePricingRuleMapper routeRuleMapper;
    private final StandardPricingConfigMapper standardConfigMapper;
    private final RouteMapper routeMapper;
    private final CustomerMapper customerMapper;
    private final AuditService auditService;

    public PricingEngineService(CustomerPricingAgreementMapper agreementMapper,
                                RoutePricingRuleMapper routeRuleMapper,
                                StandardPricingConfigMapper standardConfigMapper,
                                RouteMapper routeMapper,
                                CustomerMapper customerMapper,
                                AuditService auditService) {
        this.agreementMapper = agreementMapper;
        this.routeRuleMapper = routeRuleMapper;
        this.standardConfigMapper = standardConfigMapper;
        this.routeMapper = routeMapper;
        this.customerMapper = customerMapper;
        this.auditService = auditService;
    }

    public PricingResolveResponse resolvePrice(PricingResolveRequest req) {
        LocalDate serviceDate = req.serviceDate() != null ? req.serviceDate() : LocalDate.now(IdGenerator.WIB);
        Route route = (req.routeId() != null && !req.routeId().isBlank()) ? routeMapper.selectById(req.routeId()) : null;
        Customer customer = (req.customerId() != null && !req.customerId().isBlank()) ? customerMapper.selectById(req.customerId()) : null;

        String effectiveVehicleType = (req.vehicleType() != null && !req.vehicleType().isBlank())
                ? req.vehicleType().trim().toUpperCase()
                : (customer != null && customer.getPreferredVehicleType() != null && !customer.getPreferredVehicleType().isBlank()
                ? customer.getPreferredVehicleType().trim().toUpperCase() : null);

        BigDecimal effectiveDistanceKm = req.distanceKm() != null ? req.distanceKm()
                : (route != null ? route.getTypicalDistanceKm() : null);

        // 1. CUSTOMER_ROUTE_AGREEMENT
        if (customer != null && route != null) {
            CustomerPricingAgreement routeAgr = findBestCustomerAgreement(
                    customer.getId(), route.getId(), effectiveVehicleType, serviceDate);
            if (routeAgr != null) {
                BigDecimal fare = evaluateAgreementFare(routeAgr, route, effectiveVehicleType, req.tripType(),
                        effectiveDistanceKm, req.charterHours());
                if (fare != null) {
                    return new PricingResolveResponse(
                            roundCurrency(fare),
                            "CUSTOMER_ROUTE_AGREEMENT",
                            "Menggunakan kesepakatan harga khusus rute pelanggan: " + routeAgr.getAgreementName()
                                    + " (" + GeoAndLinkUtils.formatIdr(fare) + ")",
                            routeAgr.getAgreementName()
                    );
                }
            }
        }

        // 2. CUSTOMER_GENERAL_AGREEMENT (route_id IS NULL)
        if (customer != null) {
            CustomerPricingAgreement generalAgr = findBestCustomerAgreement(
                    customer.getId(), null, effectiveVehicleType, serviceDate);
            if (generalAgr != null) {
                BigDecimal fare = evaluateAgreementFare(generalAgr, route, effectiveVehicleType, req.tripType(),
                        effectiveDistanceKm, req.charterHours());
                if (fare != null) {
                    return new PricingResolveResponse(
                            roundCurrency(fare),
                            "CUSTOMER_GENERAL_AGREEMENT",
                            "Menggunakan kesepakatan harga umum pelanggan: " + generalAgr.getAgreementName()
                                    + " (" + GeoAndLinkUtils.formatIdr(fare) + ")",
                            generalAgr.getAgreementName()
                    );
                }
            }
        }

        // 3. ROUTE_PRICING_RULE
        if (route != null) {
            RoutePricingRule rule = findBestRouteRule(route.getId(), effectiveVehicleType, req.tripType());
            if (rule != null) {
                BigDecimal fare = roundCurrency(rule.getSuggestedFare());
                return new PricingResolveResponse(
                        fare,
                        "ROUTE_PRICING_RULE",
                        "Menggunakan aturan tarif standar rute " + route.getName()
                                + (rule.getVehicleType() != null ? " (" + rule.getVehicleType() + ")" : "")
                                + ": " + GeoAndLinkUtils.formatIdr(fare),
                        null
                );
            }
        }

        // 4. STANDARD_CALCULATION
        boolean isCharter = "HOURLY_CHARTER".equalsIgnoreCase(req.tripType())
                || "FULL_DAY_CHARTER".equalsIgnoreCase(req.tripType())
                || (req.charterHours() != null && req.charterHours() > 0);
        boolean hasDistance = effectiveDistanceKm != null && effectiveDistanceKm.signum() > 0;

        if (isCharter || hasDistance) {
            String stdVehType = effectiveVehicleType != null ? effectiveVehicleType : "MPV";
            StandardPricingConfig std = findStandardConfig(stdVehType);
            if (std != null) {
                BigDecimal calc = calculateStandardFare(std, req.tripType(), effectiveDistanceKm, req.charterHours());
                BigDecimal rounded = roundCurrency(calc);
                return new PricingResolveResponse(
                        rounded,
                        "STANDARD_CALCULATION",
                        "Perhitungan tarif standar " + std.getVehicleType() + ": " + GeoAndLinkUtils.formatIdr(rounded),
                        null
                );
            }
        }

        // 5. MANUAL_CUSTOM
        if (req.customFare() != null) {
            BigDecimal custom = roundCurrency(req.customFare());
            return new PricingResolveResponse(
                    custom,
                    "MANUAL_CUSTOM",
                    "Menggunakan tarif kustom manual: " + GeoAndLinkUtils.formatIdr(custom),
                    null
            );
        }

        // Fallback minimum fare if neither distance nor customFare supplied
        StandardPricingConfig fallbackStd = findStandardConfig(effectiveVehicleType != null ? effectiveVehicleType : "MPV");
        if (fallbackStd != null) {
            return new PricingResolveResponse(
                    roundCurrency(fallbackStd.getMinimumFare()),
                    "STANDARD_CALCULATION",
                    "Tarif minimum standar " + fallbackStd.getVehicleType(),
                    null
            );
        }

        return new PricingResolveResponse(BigDecimal.ZERO, "MANUAL_CUSTOM", "Tarif belum ditentukan", null);
    }

    public List<CustomerPricingAgreement> listCustomerAgreements(String customerId) {
        LambdaQueryWrapper<CustomerPricingAgreement> qw = new LambdaQueryWrapper<CustomerPricingAgreement>()
                .orderByDesc(CustomerPricingAgreement::getCreatedAt);
        if (customerId != null && !customerId.isBlank()) {
            qw.eq(CustomerPricingAgreement::getCustomerId, customerId);
        }
        return agreementMapper.selectList(qw);
    }

    @Transactional
    public CustomerPricingAgreement createCustomerAgreement(CustomerPricingAgreementRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        CustomerPricingAgreement a = new CustomerPricingAgreement();
        a.setId(IdGenerator.newId());
        applyAgreementFields(a, req);
        a.setCreatedBy(owner.userId());
        a.setCreatedAt(OffsetDateTime.now());
        a.setUpdatedAt(OffsetDateTime.now());
        agreementMapper.insert(a);
        auditService.log("CUSTOMER_PRICING_AGREEMENT", a.getId(), "CREATE",
                "Membuat kesepakatan harga pelanggan: " + a.getAgreementName(), null, a);
        return a;
    }

    @Transactional
    public CustomerPricingAgreement updateCustomerAgreement(String id, CustomerPricingAgreementRequest req) {
        SecurityUtils.requireOwner();
        CustomerPricingAgreement a = agreementMapper.selectById(id);
        if (a == null) {
            throw BusinessException.notFound("Kesepakatan harga tidak ditemukan.");
        }
        applyAgreementFields(a, req);
        a.setUpdatedAt(OffsetDateTime.now());
        agreementMapper.updateById(a);
        auditService.log("CUSTOMER_PRICING_AGREEMENT", a.getId(), "UPDATE",
                "Memperbarui kesepakatan harga pelanggan: " + a.getAgreementName(), null, a);
        return a;
    }

    public List<RoutePricingRule> listRouteRules(String routeId) {
        LambdaQueryWrapper<RoutePricingRule> qw = new LambdaQueryWrapper<RoutePricingRule>()
                .orderByAsc(RoutePricingRule::getRouteId);
        if (routeId != null && !routeId.isBlank()) {
            qw.eq(RoutePricingRule::getRouteId, routeId);
        }
        return routeRuleMapper.selectList(qw);
    }

    @Transactional
    public RoutePricingRule createRouteRule(RoutePricingRuleRequest req) {
        SecurityUtils.requireOwner();
        RoutePricingRule r = new RoutePricingRule();
        r.setId(IdGenerator.newId());
        r.setRouteId(req.routeId());
        r.setVehicleType(req.vehicleType() != null && !req.vehicleType().isBlank() ? req.vehicleType().trim().toUpperCase() : null);
        r.setTripType(req.tripType() != null && !req.tripType().isBlank() ? req.tripType().trim().toUpperCase() : null);
        r.setSuggestedFare(req.suggestedFare());
        r.setNotes(req.notes());
        r.setIsActive(req.isActive() == null || req.isActive());
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());
        routeRuleMapper.insert(r);
        auditService.log("ROUTE_PRICING_RULE", r.getId(), "CREATE", "Menambah aturan tarif rute", null, r);
        return r;
    }

    @Transactional
    public RoutePricingRule updateRouteRule(String id, RoutePricingRuleRequest req) {
        SecurityUtils.requireOwner();
        RoutePricingRule r = routeRuleMapper.selectById(id);
        if (r == null) {
            throw BusinessException.notFound("Aturan tarif rute tidak ditemukan.");
        }
        r.setRouteId(req.routeId());
        r.setVehicleType(req.vehicleType() != null && !req.vehicleType().isBlank() ? req.vehicleType().trim().toUpperCase() : null);
        r.setTripType(req.tripType() != null && !req.tripType().isBlank() ? req.tripType().trim().toUpperCase() : null);
        r.setSuggestedFare(req.suggestedFare());
        r.setNotes(req.notes());
        if (req.isActive() != null) {
            r.setIsActive(req.isActive());
        }
        r.setUpdatedAt(OffsetDateTime.now());
        routeRuleMapper.updateById(r);
        auditService.log("ROUTE_PRICING_RULE", r.getId(), "UPDATE", "Memperbarui aturan tarif rute", null, r);
        return r;
    }

    public List<StandardPricingConfig> listStandardConfigs() {
        return standardConfigMapper.selectList(new LambdaQueryWrapper<StandardPricingConfig>()
                .eq(StandardPricingConfig::getIsActive, true)
                .orderByAsc(StandardPricingConfig::getVehicleType));
    }

    private void applyAgreementFields(CustomerPricingAgreement a, CustomerPricingAgreementRequest req) {
        a.setCustomerId(req.customerId());
        a.setAgreementName(req.agreementName().trim());
        a.setRouteId(req.routeId() != null && !req.routeId().isBlank() ? req.routeId() : null);
        a.setVehicleType(req.vehicleType() != null && !req.vehicleType().isBlank() ? req.vehicleType().trim().toUpperCase() : null);
        a.setPricingMode(req.pricingMode().trim().toUpperCase());
        a.setAgreedFare(req.agreedFare());
        a.setPerKmRate(req.perKmRate());
        a.setHourlyRate(req.hourlyRate());
        a.setFullDayRate(req.fullDayRate());
        a.setDiscountPercent(req.discountPercent());
        a.setEffectiveFrom(req.effectiveFrom());
        a.setEffectiveTo(req.effectiveTo());
        a.setConditionsNote(req.conditionsNote());
        a.setIsActive(req.isActive() == null || req.isActive());
    }

    private CustomerPricingAgreement findBestCustomerAgreement(String customerId,
                                                               String routeId,
                                                               String vehicleType,
                                                               LocalDate serviceDate) {
        LambdaQueryWrapper<CustomerPricingAgreement> qw = new LambdaQueryWrapper<CustomerPricingAgreement>()
                .eq(CustomerPricingAgreement::getCustomerId, customerId)
                .eq(CustomerPricingAgreement::getIsActive, true);
        if (routeId != null) {
            qw.eq(CustomerPricingAgreement::getRouteId, routeId);
        } else {
            qw.isNull(CustomerPricingAgreement::getRouteId);
        }
        List<CustomerPricingAgreement> candidates = agreementMapper.selectList(qw);
        return candidates.stream()
                .filter(a -> isDateWithin(serviceDate, a.getEffectiveFrom(), a.getEffectiveTo()))
                .filter(a -> matchesVehicleType(a.getVehicleType(), vehicleType))
                .sorted(Comparator.comparingInt((CustomerPricingAgreement a) ->
                        (a.getVehicleType() != null && a.getVehicleType().equalsIgnoreCase(vehicleType)) ? 0 : 1))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal evaluateAgreementFare(CustomerPricingAgreement agr,
                                             Route route,
                                             String vehicleType,
                                             String tripType,
                                             BigDecimal distanceKm,
                                             Integer charterHours) {
        String mode = agr.getPricingMode() != null ? agr.getPricingMode().toUpperCase() : "FIXED_FARE";
        return switch (mode) {
            case "FIXED_FARE" -> agr.getAgreedFare();
            case "PER_KM" -> (agr.getPerKmRate() != null && distanceKm != null)
                    ? agr.getPerKmRate().multiply(distanceKm) : agr.getAgreedFare();
            case "HOURLY" -> {
                int hours = (charterHours != null && charterHours > 0) ? charterHours : 4;
                yield agr.getHourlyRate() != null ? agr.getHourlyRate().multiply(BigDecimal.valueOf(hours)) : agr.getAgreedFare();
            }
            case "FULL_DAY" -> agr.getFullDayRate() != null ? agr.getFullDayRate() : agr.getAgreedFare();
            case "DISCOUNT_PERCENT" -> {
                if (agr.getDiscountPercent() == null) {
                    yield null;
                }
                BigDecimal basePrice = resolveBaseRouteOrStandardPrice(route, vehicleType, tripType, distanceKm, charterHours);
                if (basePrice == null) {
                    yield null;
                }
                BigDecimal factor = BigDecimal.valueOf(100).subtract(agr.getDiscountPercent())
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                yield basePrice.multiply(factor);
            }
            default -> agr.getAgreedFare();
        };
    }

    private BigDecimal resolveBaseRouteOrStandardPrice(Route route,
                                                       String vehicleType,
                                                       String tripType,
                                                       BigDecimal distanceKm,
                                                       Integer charterHours) {
        if (route != null) {
            RoutePricingRule rule = findBestRouteRule(route.getId(), vehicleType, tripType);
            if (rule != null) {
                return rule.getSuggestedFare();
            }
        }
        StandardPricingConfig std = findStandardConfig(vehicleType != null ? vehicleType : "MPV");
        if (std != null) {
            return calculateStandardFare(std, tripType, distanceKm, charterHours);
        }
        return null;
    }

    private RoutePricingRule findBestRouteRule(String routeId, String vehicleType, String tripType) {
        String effVeh = vehicleType != null ? vehicleType : "MPV";
        List<RoutePricingRule> rules = routeRuleMapper.selectList(new LambdaQueryWrapper<RoutePricingRule>()
                .eq(RoutePricingRule::getRouteId, routeId)
                .eq(RoutePricingRule::getIsActive, true));
        return rules.stream()
                .filter(r -> r.getVehicleType() == null || r.getVehicleType().equalsIgnoreCase(effVeh))
                .filter(r -> r.getTripType() == null || tripType == null || r.getTripType().equalsIgnoreCase(tripType))
                .sorted(Comparator.comparingInt((RoutePricingRule r) -> {
                    int score = 0;
                    if (r.getVehicleType() != null && r.getVehicleType().equalsIgnoreCase(effVeh)) {
                        score -= 2;
                    }
                    if (r.getTripType() != null && tripType != null && r.getTripType().equalsIgnoreCase(tripType)) {
                        score -= 1;
                    }
                    return score;
                }))
                .findFirst()
                .orElse(null);
    }

    private StandardPricingConfig findStandardConfig(String vehicleType) {
        StandardPricingConfig cfg = standardConfigMapper.selectOne(new LambdaQueryWrapper<StandardPricingConfig>()
                .eq(StandardPricingConfig::getVehicleType, vehicleType.toUpperCase())
                .eq(StandardPricingConfig::getIsActive, true)
                .last("LIMIT 1"));
        if (cfg == null && !"MPV".equalsIgnoreCase(vehicleType)) {
            cfg = standardConfigMapper.selectOne(new LambdaQueryWrapper<StandardPricingConfig>()
                    .eq(StandardPricingConfig::getVehicleType, "MPV")
                    .eq(StandardPricingConfig::getIsActive, true)
                    .last("LIMIT 1"));
        }
        return cfg;
    }

    private BigDecimal calculateStandardFare(StandardPricingConfig std,
                                             String tripType,
                                             BigDecimal distanceKm,
                                             Integer charterHours) {
        if ("FULL_DAY_CHARTER".equalsIgnoreCase(tripType)) {
            return std.getFullDayCharterRate();
        }
        if ("HOURLY_CHARTER".equalsIgnoreCase(tripType) || (charterHours != null && charterHours > 0)) {
            int hrs = (charterHours != null && charterHours > 0) ? charterHours : 4;
            BigDecimal raw = std.getHourlyCharterRate().multiply(BigDecimal.valueOf(hrs));
            return raw.max(std.getMinimumFare());
        }
        BigDecimal dist = distanceKm != null ? distanceKm : BigDecimal.ZERO;
        BigDecimal raw = std.getBaseFare().add(std.getPerKmRate().multiply(dist));
        return raw.max(std.getMinimumFare());
    }

    private boolean isDateWithin(LocalDate target, LocalDate from, LocalDate to) {
        if (from != null && target.isBefore(from)) {
            return false;
        }
        if (to != null && target.isAfter(to)) {
            return false;
        }
        return true;
    }

    private boolean matchesVehicleType(String ruleVehType, String reqVehType) {
        if (ruleVehType == null || ruleVehType.isBlank()) {
            return true;
        }
        if (reqVehType == null || reqVehType.isBlank()) {
            return true;
        }
        return ruleVehType.equalsIgnoreCase(reqVehType);
    }

    private BigDecimal roundCurrency(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val.setScale(2, RoundingMode.HALF_UP);
    }
}
