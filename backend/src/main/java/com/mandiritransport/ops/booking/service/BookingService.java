package com.mandiritransport.ops.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.booking.dto.BookingDtos.BookingDetailDto;
import com.mandiritransport.ops.booking.dto.BookingDtos.BookingTemplateRequest;
import com.mandiritransport.ops.booking.dto.BookingDtos.QuickBookingRequest;
import com.mandiritransport.ops.booking.dto.BookingDtos.TripStopInput;
import com.mandiritransport.ops.booking.entity.Booking;
import com.mandiritransport.ops.booking.entity.BookingTemplate;
import com.mandiritransport.ops.booking.entity.FareOverrideAudit;
import com.mandiritransport.ops.booking.mapper.BookingMapper;
import com.mandiritransport.ops.booking.mapper.BookingTemplateMapper;
import com.mandiritransport.ops.booking.mapper.FareOverrideAuditMapper;
import com.mandiritransport.ops.booking.service.FlightInfoAdapter.FlightInfo;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.customer.entity.Customer;
import com.mandiritransport.ops.customer.mapper.CustomerMapper;
import com.mandiritransport.ops.pricing.dto.PricingDtos.PricingResolveRequest;
import com.mandiritransport.ops.pricing.dto.PricingDtos.PricingResolveResponse;
import com.mandiritransport.ops.pricing.service.PricingEngineService;
import com.mandiritransport.ops.trip.dto.TripDtos.TripDetailDto;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.entity.TripStop;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.trip.mapper.TripStopMapper;
import com.mandiritransport.ops.trip.service.TripService;
import com.mandiritransport.ops.vehicle.entity.Route;
import com.mandiritransport.ops.vehicle.mapper.RouteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingTemplateMapper templateMapper;
    private final BookingMapper bookingMapper;
    private final FareOverrideAuditMapper fareOverrideMapper;
    private final CustomerMapper customerMapper;
    private final RouteMapper routeMapper;
    private final TripMapper tripMapper;
    private final TripStopMapper tripStopMapper;
    private final PricingEngineService pricingEngineService;
    private final FlightInfoAdapter flightInfoAdapter;
    private final TripService tripService;
    private final AuditService auditService;

    public BookingService(BookingTemplateMapper templateMapper,
                          BookingMapper bookingMapper,
                          FareOverrideAuditMapper fareOverrideMapper,
                          CustomerMapper customerMapper,
                          RouteMapper routeMapper,
                          TripMapper tripMapper,
                          TripStopMapper tripStopMapper,
                          PricingEngineService pricingEngineService,
                          FlightInfoAdapter flightInfoAdapter,
                          TripService tripService,
                          AuditService auditService) {
        this.templateMapper = templateMapper;
        this.bookingMapper = bookingMapper;
        this.fareOverrideMapper = fareOverrideMapper;
        this.customerMapper = customerMapper;
        this.routeMapper = routeMapper;
        this.tripMapper = tripMapper;
        this.tripStopMapper = tripStopMapper;
        this.pricingEngineService = pricingEngineService;
        this.flightInfoAdapter = flightInfoAdapter;
        this.tripService = tripService;
        this.auditService = auditService;
    }

    public List<BookingTemplate> listTemplates() {
        SecurityUtils.requireOwner();
        return templateMapper.selectList(new LambdaQueryWrapper<BookingTemplate>()
                .orderByAsc(BookingTemplate::getName));
    }

    @Transactional
    public BookingTemplate createTemplate(BookingTemplateRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        BookingTemplate tpl = new BookingTemplate();
        tpl.setId(IdGenerator.newId());
        tpl.setName(req.name().trim());
        tpl.setCustomerId(req.customerId() != null && !req.customerId().isBlank() ? req.customerId() : null);
        tpl.setRouteId(req.routeId() != null && !req.routeId().isBlank() ? req.routeId() : null);
        tpl.setTripType(req.tripType().trim().toUpperCase());
        tpl.setBookingSource(req.bookingSource() != null && !req.bookingSource().isBlank()
                ? req.bookingSource().trim().toUpperCase() : "RETURNING_CUSTOMER");
        tpl.setPickupAddress(req.pickupAddress().trim());
        tpl.setDestinationAddress(req.destinationAddress().trim());
        tpl.setDefaultPassengerCount(req.defaultPassengerCount() != null ? req.defaultPassengerCount() : 1);
        tpl.setDefaultLuggageCount(req.defaultLuggageCount() != null ? req.defaultLuggageCount() : 1);
        tpl.setLuggageNotes(req.luggageNotes());
        tpl.setPreferredVehicleType(req.preferredVehicleType());
        tpl.setNotes(req.notes());
        tpl.setCreatedBy(owner.userId());
        tpl.setCreatedAt(OffsetDateTime.now());
        tpl.setUpdatedAt(OffsetDateTime.now());
        // Strictly prefill-only: never inserts bookings or trips!
        templateMapper.insert(tpl);
        auditService.log("BOOKING_TEMPLATE", tpl.getId(), "CREATE",
                "Menyimpan template pesanan: " + tpl.getName(), null, tpl);
        return tpl;
    }

    @Transactional
    public void deleteTemplate(String id) {
        SecurityUtils.requireOwner();
        BookingTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("Template pesanan tidak ditemukan.");
        }
        templateMapper.deleteById(id);
        auditService.log("BOOKING_TEMPLATE", id, "DELETE", "Menghapus template pesanan: " + existing.getName(), existing, null);
    }

    public List<BookingDetailDto> listBookings(String status) {
        SecurityUtils.requireOwner();
        LambdaQueryWrapper<Booking> qw = new LambdaQueryWrapper<Booking>()
                .orderByDesc(Booking::getRequestedPickupAt);
        if (status != null && !status.isBlank()) {
            qw.eq(Booking::getStatus, status.trim().toUpperCase());
        }
        return bookingMapper.selectList(qw).stream()
                .map(this::toBookingDetail)
                .toList();
    }

    public BookingDetailDto getBooking(String id) {
        SecurityUtils.requireOwner();
        Booking b = bookingMapper.selectById(id);
        if (b == null) {
            throw BusinessException.notFound("Booking tidak ditemukan.");
        }
        return toBookingDetail(b);
    }

    @Transactional
    public BookingDetailDto createQuickBooking(QuickBookingRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Resolve or inline-create customer
        Customer customer = resolveOrCreateCustomer(req, owner.userId());

        // 2. Resolve route & flight info
        Route route = (req.routeId() != null && !req.routeId().isBlank()) ? routeMapper.selectById(req.routeId()) : null;
        FlightInfo flightInfo = flightInfoAdapter.resolveFlight(req.flightNumber(), req.flightScheduledAt());

        String tripType = req.tripType().trim().toUpperCase();
        String vehiclePref = (req.vehicleTypePreference() != null && !req.vehicleTypePreference().isBlank())
                ? req.vehicleTypePreference().trim().toUpperCase()
                : customer.getPreferredVehicleType();

        // 3. Resolve suggested fare via 5-tier Pricing Priority Engine
        LocalDate serviceDate = req.requestedPickupAt().atZoneSameInstant(IdGenerator.WIB).toLocalDate();
        PricingResolveResponse pricing = pricingEngineService.resolvePrice(new PricingResolveRequest(
                customer.getId(),
                route != null ? route.getId() : null,
                vehiclePref,
                tripType,
                route != null ? route.getTypicalDistanceKm() : null,
                req.charterHours(),
                serviceDate,
                req.agreedFare()
        ));

        BigDecimal suggestedFare = pricing.suggestedFare();
        BigDecimal finalAgreedFare = req.agreedFare() != null ? req.agreedFare() : suggestedFare;

        // 4. Create Booking
        Booking booking = new Booking();
        booking.setId(IdGenerator.newId());
        booking.setBookingCode(IdGenerator.bookingCode(req.requestedPickupAt()));
        booking.setCustomerId(customer.getId());
        booking.setContactName(customer.getName());
        booking.setContactPhone(customer.getPhone());
        booking.setBookingSource(req.bookingSource() != null && !req.bookingSource().isBlank()
                ? req.bookingSource().trim().toUpperCase() : "WHATSAPP");
        booking.setTripType(tripType);
        booking.setRouteId(route != null ? route.getId() : null);
        booking.setRequestedPickupAt(req.requestedPickupAt());
        booking.setReturnPickupAt(req.returnPickupAt());
        booking.setCharterHours(req.charterHours());
        booking.setPickupAddress(req.pickupAddress().trim());
        booking.setPickupLat(req.pickupLat());
        booking.setPickupLng(req.pickupLng());
        booking.setDestinationAddress(req.destinationAddress().trim());
        booking.setDestinationLat(req.destinationLat());
        booking.setDestinationLng(req.destinationLng());
        booking.setPassengerCount(req.passengerCount() != null ? req.passengerCount() : 1);
        booking.setLuggageCount(req.luggageCount() != null ? req.luggageCount() : 0);
        booking.setLuggageNotes(req.luggageNotes());
        booking.setFlightNumber(flightInfo.normalizedFlightNumber());
        booking.setFlightScheduledAt(flightInfo.scheduledAt());
        booking.setSuggestedFare(suggestedFare);
        booking.setPricingTierUsed(pricing.pricingTierUsed());
        booking.setPricingExplanation(pricing.explanation());
        booking.setAgreedFare(finalAgreedFare);
        booking.setSpecialInstructions(req.specialInstructions());
        booking.setStatus("CONFIRMED");
        booking.setCreatedBy(owner.userId());
        booking.setUpdatedBy(owner.userId());
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        booking.setVersion(0L);
        bookingMapper.insert(booking);

        // 5. Create Leg 1 Trip with buffered reservation window & immutable agreed_fare_snapshot
        Trip leg1 = createTripLeg(
                booking,
                1,
                "Outbound",
                route,
                tripType,
                req.requestedPickupAt(),
                req.charterHours(),
                req.pickupAddress().trim(),
                req.pickupLat(),
                req.pickupLng(),
                req.destinationAddress().trim(),
                req.destinationLat(),
                req.destinationLng(),
                flightInfo.normalizedFlightNumber(),
                finalAgreedFare,
                owner.userId()
        );

        // Log fare override audit if agreedFare != suggestedFare
        if (suggestedFare != null && finalAgreedFare.compareTo(suggestedFare) != 0) {
            recordFareOverrideAudit(booking.getId(), leg1.getId(), suggestedFare,
                    pricing.pricingTierUsed(), finalAgreedFare, req.overrideReason(), owner.userId());
        }

        // Persist multi-stop charter stops if provided
        if (req.stops() != null && !req.stops().isEmpty()) {
            int order = 1;
            for (TripStopInput s : req.stops()) {
                TripStop stop = new TripStop();
                stop.setId(IdGenerator.newId());
                stop.setTripId(leg1.getId());
                stop.setStopOrder(order++);
                stop.setStopAddress(s.stopAddress().trim());
                stop.setLatitude(s.latitude());
                stop.setLongitude(s.longitude());
                stop.setPlannedArrivalAt(s.plannedArrivalAt());
                stop.setNotes(s.notes());
                stop.setCreatedAt(now);
                tripStopMapper.insert(stop);
            }
        }

        // 6. Create Leg 2 Return Trip when ROUND_TRIP or returnPickupAt != null
        boolean hasReturnLeg = "ROUND_TRIP".equalsIgnoreCase(tripType) || req.returnPickupAt() != null;
        if (hasReturnLeg) {
            OffsetDateTime returnPickup = req.returnPickupAt() != null
                    ? req.returnPickupAt()
                    : req.requestedPickupAt().plusHours(6);
            Route reverseRoute = findReverseRoute(route);
            String returnTripType = determineReturnTripType(tripType, reverseRoute);

            LocalDate returnDate = returnPickup.atZoneSameInstant(IdGenerator.WIB).toLocalDate();
            PricingResolveResponse returnPricing = pricingEngineService.resolvePrice(new PricingResolveRequest(
                    customer.getId(),
                    reverseRoute != null ? reverseRoute.getId() : null,
                    vehiclePref,
                    returnTripType,
                    reverseRoute != null ? reverseRoute.getTypicalDistanceKm() : (route != null ? route.getTypicalDistanceKm() : null),
                    req.charterHours(),
                    returnDate,
                    req.returnAgreedFare() != null ? req.returnAgreedFare() : finalAgreedFare
            ));

            BigDecimal returnSuggested = returnPricing.suggestedFare();
            BigDecimal returnAgreed = req.returnAgreedFare() != null
                    ? req.returnAgreedFare()
                    : (returnSuggested != null && returnSuggested.signum() > 0 ? returnSuggested : finalAgreedFare);

            Trip leg2 = createTripLeg(
                    booking,
                    2,
                    "Return",
                    reverseRoute,
                    returnTripType,
                    returnPickup,
                    req.charterHours(),
                    req.destinationAddress().trim(),
                    req.destinationLat(),
                    req.destinationLng(),
                    req.pickupAddress().trim(),
                    req.pickupLat(),
                    req.pickupLng(),
                    null,
                    returnAgreed,
                    owner.userId()
            );

            if (returnSuggested != null && returnAgreed.compareTo(returnSuggested) != 0) {
                recordFareOverrideAudit(booking.getId(), leg2.getId(), returnSuggested,
                        returnPricing.pricingTierUsed(), returnAgreed, req.overrideReason(), owner.userId());
            }
        }

        auditService.log("BOOKING", booking.getId(), "QUICK_CREATE",
                "Membuat pesanan cepat " + booking.getBookingCode() + " untuk " + customer.getName(), null, booking);
        return toBookingDetail(booking);
    }

    private Customer resolveOrCreateCustomer(QuickBookingRequest req, String ownerUserId) {
        if (req.customerId() != null && !req.customerId().isBlank()) {
            Customer existing = customerMapper.selectById(req.customerId());
            if (existing == null) {
                throw BusinessException.notFound("Pelanggan tidak ditemukan: " + req.customerId());
            }
            return existing;
        }
        if (req.newCustomerName() == null || req.newCustomerName().isBlank()
                || req.newCustomerPhone() == null || req.newCustomerPhone().isBlank()) {
            throw BusinessException.badRequest("CUSTOMER_REQUIRED",
                    "Pilih customerId atau isi newCustomerName dan newCustomerPhone untuk pelanggan baru.");
        }
        Customer c = new Customer();
        c.setId(IdGenerator.newId());
        c.setName(req.newCustomerName().trim());
        c.setPhone(req.newCustomerPhone().trim());
        c.setWhatsappPhone(req.newCustomerPhone().trim());
        c.setPreferredVehicleType(req.vehicleTypePreference());
        c.setIsActive(true);
        c.setCreatedBy(ownerUserId);
        c.setUpdatedBy(ownerUserId);
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        c.setVersion(0L);
        customerMapper.insert(c);
        return c;
    }

    private Trip createTripLeg(Booking booking,
                               int legNumber,
                               String legLabel,
                               Route route,
                               String tripType,
                               OffsetDateTime pickupAt,
                               Integer charterHours,
                               String pickupAddr,
                               BigDecimal pickupLat,
                               BigDecimal pickupLng,
                               String destAddr,
                               BigDecimal destLat,
                               BigDecimal destLng,
                               String flightNumber,
                               BigDecimal agreedFareSnapshot,
                               String userId) {
        int durationMin = (charterHours != null && charterHours > 0)
                ? charterHours * 60
                : (route != null && route.getDefaultDurationMin() != null ? route.getDefaultDurationMin() : 60);
        int prepMin = route != null && route.getDefaultPrepBufferMin() != null ? route.getDefaultPrepBufferMin() : 15;
        int arrivalMin = route != null && route.getDefaultArrivalBufferMin() != null
                ? route.getDefaultArrivalBufferMin()
                : ("AIRPORT_PICKUP".equalsIgnoreCase(tripType) ? 30 : 0);
        int repositionMin = route != null && route.getDefaultRepositionBufferMin() != null ? route.getDefaultRepositionBufferMin() : 15;

        OffsetDateTime reservedStart = pickupAt.minusMinutes(prepMin);
        OffsetDateTime reservedEnd = pickupAt.plusMinutes((long) durationMin + arrivalMin + repositionMin);

        Trip t = new Trip();
        t.setId(IdGenerator.newId());
        t.setTripCode(IdGenerator.tripCode(pickupAt));
        t.setBookingId(booking.getId());
        t.setLegNumber(legNumber);
        t.setLegLabel(legLabel);
        t.setRouteId(route != null ? route.getId() : null);
        t.setTripType(tripType);
        t.setScheduledPickupAt(pickupAt);
        t.setEstimatedDurationMin(durationMin);
        t.setPrepBufferMin(prepMin);
        t.setArrivalBufferMin(arrivalMin);
        t.setRepositionBufferMin(repositionMin);
        t.setReservedStartAt(reservedStart);
        t.setReservedEndAt(reservedEnd);
        t.setPickupAddress(pickupAddr);
        t.setPickupLat(pickupLat);
        t.setPickupLng(pickupLng);
        t.setDestinationAddress(destAddr);
        t.setDestinationLat(destLat);
        t.setDestinationLng(destLng);
        t.setPassengerCount(booking.getPassengerCount());
        t.setLuggageCount(booking.getLuggageCount());
        t.setFlightNumber(flightNumber);
        t.setAgreedFareSnapshot(agreedFareSnapshot);
        t.setStatus("UNASSIGNED");
        t.setTrackingActive(false);
        t.setPaymentStatus("UNPAID");
        t.setTotalPaidAmount(BigDecimal.ZERO);
        t.setCashCustodyStatus("NO_DRIVER_CASH_DUE");
        t.setDriverCashHeldAmount(BigDecimal.ZERO);
        t.setCreatedBy(userId);
        t.setUpdatedBy(userId);
        t.setCreatedAt(OffsetDateTime.now());
        t.setUpdatedAt(OffsetDateTime.now());
        t.setVersion(0L);
        tripMapper.insert(t);

        tripService.recordStatusHistory(t.getId(), null, "UNASSIGNED", userId, null, "Pesanan dibuat");
        return t;
    }

    private Route findReverseRoute(Route outbound) {
        if (outbound == null) {
            return null;
        }
        // Try code swap e.g. MDN_KNO -> KNO_MDN
        if (outbound.getCode() != null && outbound.getCode().contains("_")) {
            String[] parts = outbound.getCode().split("_", 2);
            String swappedCode = parts[1] + "_" + parts[0];
            Route byCode = routeMapper.selectOne(new LambdaQueryWrapper<Route>()
                    .eq(Route::getCode, swappedCode)
                    .eq(Route::getIsActive, true)
                    .last("LIMIT 1"));
            if (byCode != null) {
                return byCode;
            }
        }
        // Try origin <-> destination match
        Route byEndpoints = routeMapper.selectOne(new LambdaQueryWrapper<Route>()
                .eq(Route::getOriginName, outbound.getDestinationName())
                .eq(Route::getDestinationName, outbound.getOriginName())
                .eq(Route::getIsActive, true)
                .last("LIMIT 1"));
        return byEndpoints != null ? byEndpoints : outbound;
    }

    private String determineReturnTripType(String outboundType, Route reverseRoute) {
        if (reverseRoute != null && "KNO_MDN".equalsIgnoreCase(reverseRoute.getCode())) {
            return "AIRPORT_PICKUP";
        }
        if (reverseRoute != null && "MDN_KNO".equalsIgnoreCase(reverseRoute.getCode())) {
            return "AIRPORT_DROPOFF";
        }
        if ("AIRPORT_DROPOFF".equalsIgnoreCase(outboundType)) {
            return "AIRPORT_PICKUP";
        }
        if ("AIRPORT_PICKUP".equalsIgnoreCase(outboundType)) {
            return "AIRPORT_DROPOFF";
        }
        return outboundType;
    }

    private void recordFareOverrideAudit(String bookingId,
                                         String tripId,
                                         BigDecimal suggestedFare,
                                         String tierUsed,
                                         BigDecimal finalFare,
                                         String reason,
                                         String userId) {
        FareOverrideAudit audit = new FareOverrideAudit();
        audit.setId(IdGenerator.newId());
        audit.setBookingId(bookingId);
        audit.setTripId(tripId);
        audit.setSuggestedFare(suggestedFare);
        audit.setPricingTierUsed(tierUsed);
        audit.setFinalAgreedFare(finalFare);
        audit.setOverrideReason(reason != null ? reason : "Kesepakatan harga khusus saat booking");
        audit.setAuthorizedBy(userId);
        audit.setCreatedAt(OffsetDateTime.now());
        fareOverrideMapper.insert(audit);
    }

    private BookingDetailDto toBookingDetail(Booking b) {
        List<TripDetailDto> trips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                        .eq(Trip::getBookingId, b.getId())
                        .orderByAsc(Trip::getLegNumber))
                .stream()
                .map(t -> tripService.toDetailDto(t, false))
                .toList();
        return new BookingDetailDto(b, trips);
    }
}
