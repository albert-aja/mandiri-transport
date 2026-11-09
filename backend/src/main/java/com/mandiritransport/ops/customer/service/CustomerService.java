package com.mandiritransport.ops.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.booking.entity.Booking;
import com.mandiritransport.ops.booking.mapper.BookingMapper;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.GeoAndLinkUtils;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerAddressRequest;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerDetailDto;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerNoteRequest;
import com.mandiritransport.ops.customer.dto.CustomerDtos.CustomerRequest;
import com.mandiritransport.ops.customer.dto.CustomerDtos.RenderWhatsappRequest;
import com.mandiritransport.ops.customer.dto.CustomerDtos.RenderWhatsappResponse;
import com.mandiritransport.ops.customer.entity.Customer;
import com.mandiritransport.ops.customer.entity.CustomerAddress;
import com.mandiritransport.ops.customer.entity.CustomerNote;
import com.mandiritransport.ops.customer.entity.WhatsappTemplate;
import com.mandiritransport.ops.customer.mapper.CustomerAddressMapper;
import com.mandiritransport.ops.customer.mapper.CustomerMapper;
import com.mandiritransport.ops.customer.mapper.CustomerNoteMapper;
import com.mandiritransport.ops.customer.mapper.WhatsappTemplateMapper;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.pricing.entity.CustomerPricingAgreement;
import com.mandiritransport.ops.pricing.mapper.CustomerPricingAgreementMapper;
import com.mandiritransport.ops.trip.entity.Trip;
import com.mandiritransport.ops.trip.mapper.TripMapper;
import com.mandiritransport.ops.vehicle.entity.Vehicle;
import com.mandiritransport.ops.vehicle.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final DateTimeFormatter WA_TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"));

    private final CustomerMapper customerMapper;
    private final CustomerAddressMapper addressMapper;
    private final CustomerNoteMapper noteMapper;
    private final WhatsappTemplateMapper whatsappTemplateMapper;
    private final CustomerPricingAgreementMapper agreementMapper;
    private final TripMapper tripMapper;
    private final BookingMapper bookingMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final AuditService auditService;

    public CustomerService(CustomerMapper customerMapper,
                           CustomerAddressMapper addressMapper,
                           CustomerNoteMapper noteMapper,
                           WhatsappTemplateMapper whatsappTemplateMapper,
                           CustomerPricingAgreementMapper agreementMapper,
                           TripMapper tripMapper,
                           BookingMapper bookingMapper,
                           DriverMapper driverMapper,
                           VehicleMapper vehicleMapper,
                           AuditService auditService) {
        this.customerMapper = customerMapper;
        this.addressMapper = addressMapper;
        this.noteMapper = noteMapper;
        this.whatsappTemplateMapper = whatsappTemplateMapper;
        this.agreementMapper = agreementMapper;
        this.tripMapper = tripMapper;
        this.bookingMapper = bookingMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.auditService = auditService;
    }

    public List<CustomerDetailDto> listCustomers(String query) {
        SecurityUser current = SecurityUtils.currentUser();
        Set<String> allowedCustomerIds = null;

        if (current.isDriver()) {
            String driverId = SecurityUtils.requireDriverId();
            allowedCustomerIds = resolveDriverCustomerIds(driverId);
            if (allowedCustomerIds.isEmpty()) {
                return Collections.emptyList();
            }
        }

        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getIsActive, true)
                .orderByAsc(Customer::getName);
        if (allowedCustomerIds != null) {
            qw.in(Customer::getId, allowedCustomerIds);
        }
        if (query != null && !query.isBlank()) {
            String q = query.trim();
            qw.and(w -> w.like(Customer::getName, q)
                    .or().like(Customer::getPhone, q)
                    .or().like(Customer::getCompanyName, q));
        }

        List<Customer> customers = customerMapper.selectList(qw);
        return customers.stream()
                .map(c -> toDetailDto(c, current.isDriver()))
                .toList();
    }

    public CustomerDetailDto getCustomer(String id) {
        SecurityUser current = SecurityUtils.currentUser();
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw BusinessException.notFound("Pelanggan tidak ditemukan.");
        }
        if (current.isDriver()) {
            String driverId = SecurityUtils.requireDriverId();
            Set<String> allowedIds = resolveDriverCustomerIds(driverId);
            if (!allowedIds.contains(customer.getId())) {
                throw BusinessException.forbidden("Supir hanya dapat melihat data pelanggan pada perjalanan yang ditugaskan kepadanya.");
            }
        }
        return toDetailDto(customer, current.isDriver());
    }

    @Transactional
    public CustomerDetailDto createCustomer(CustomerRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        Customer c = new Customer();
        c.setId(IdGenerator.newId());
        c.setName(req.name().trim());
        c.setPhone(req.phone().trim());
        c.setWhatsappPhone(req.whatsappPhone() != null && !req.whatsappPhone().isBlank()
                ? req.whatsappPhone().trim() : req.phone().trim());
        c.setCompanyName(req.companyName());
        c.setPreferredVehicleType(req.preferredVehicleType());
        c.setIsActive(true);
        c.setCreatedBy(owner.userId());
        c.setUpdatedBy(owner.userId());
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        c.setVersion(0L);
        customerMapper.insert(c);
        auditService.log("CUSTOMER", c.getId(), "CREATE", "Membuat pelanggan baru: " + c.getName(), null, c);
        return toDetailDto(c, false);
    }

    @Transactional
    public CustomerDetailDto updateCustomer(String id, CustomerRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("Pelanggan tidak ditemukan.");
        }
        existing.setName(req.name().trim());
        existing.setPhone(req.phone().trim());
        existing.setWhatsappPhone(req.whatsappPhone() != null && !req.whatsappPhone().isBlank()
                ? req.whatsappPhone().trim() : req.phone().trim());
        existing.setCompanyName(req.companyName());
        existing.setPreferredVehicleType(req.preferredVehicleType());
        existing.setUpdatedBy(owner.userId());
        existing.setUpdatedAt(OffsetDateTime.now());
        customerMapper.updateById(existing);
        auditService.log("CUSTOMER", id, "UPDATE", "Memperbarui pelanggan: " + existing.getName(), null, existing);
        return toDetailDto(existing, false);
    }

    @Transactional
    public CustomerAddress addAddress(String customerId, CustomerAddressRequest req) {
        SecurityUtils.requireOwner();
        Customer existing = customerMapper.selectById(customerId);
        if (existing == null) {
            throw BusinessException.notFound("Pelanggan tidak ditemukan.");
        }
        if (Boolean.TRUE.equals(req.isDefault())) {
            List<CustomerAddress> currentAddrs = addressMapper.selectList(
                    new LambdaQueryWrapper<CustomerAddress>().eq(CustomerAddress::getCustomerId, customerId));
            for (CustomerAddress ca : currentAddrs) {
                if (Boolean.TRUE.equals(ca.getIsDefault())) {
                    ca.setIsDefault(false);
                    ca.setUpdatedAt(OffsetDateTime.now());
                    addressMapper.updateById(ca);
                }
            }
        }
        CustomerAddress addr = new CustomerAddress();
        addr.setId(IdGenerator.newId());
        addr.setCustomerId(customerId);
        addr.setLabel(req.label().trim());
        addr.setAddressText(req.addressText().trim());
        addr.setLatitude(req.latitude());
        addr.setLongitude(req.longitude());
        String mapsUrl = (req.googleMapsUrl() != null && !req.googleMapsUrl().isBlank())
                ? req.googleMapsUrl().trim()
                : GeoAndLinkUtils.buildGoogleMapsSearchUrl(req.latitude(), req.longitude(), req.addressText());
        addr.setGoogleMapsUrl(mapsUrl);
        addr.setIsDefault(Boolean.TRUE.equals(req.isDefault()));
        addr.setCreatedAt(OffsetDateTime.now());
        addr.setUpdatedAt(OffsetDateTime.now());
        addressMapper.insert(addr);
        auditService.log("CUSTOMER_ADDRESS", addr.getId(), "CREATE",
                "Menambah alamat pelanggan " + existing.getName() + " (" + addr.getLabel() + ")", null, addr);
        return addr;
    }

    @Transactional
    public CustomerNote addNote(String customerId, CustomerNoteRequest req) {
        SecurityUser owner = SecurityUtils.requireOwner();
        Customer existing = customerMapper.selectById(customerId);
        if (existing == null) {
            throw BusinessException.notFound("Pelanggan tidak ditemukan.");
        }
        CustomerNote note = new CustomerNote();
        note.setId(IdGenerator.newId());
        note.setCustomerId(customerId);
        note.setCategory(req.category() != null && !req.category().isBlank() ? req.category().trim().toUpperCase() : "OPERATIONAL");
        note.setNoteText(req.noteText().trim());
        note.setVisibleToDrivers(req.visibleToDrivers() == null || req.visibleToDrivers());
        note.setCreatedBy(owner.userId());
        note.setUpdatedBy(owner.userId());
        note.setCreatedAt(OffsetDateTime.now());
        note.setUpdatedAt(OffsetDateTime.now());
        noteMapper.insert(note);
        auditService.log("CUSTOMER_NOTE", note.getId(), "CREATE",
                "Menambah catatan operasional pelanggan " + existing.getName(), null, note);
        return note;
    }

    public List<WhatsappTemplate> listWhatsappTemplates() {
        return whatsappTemplateMapper.selectList(new LambdaQueryWrapper<WhatsappTemplate>()
                .eq(WhatsappTemplate::getIsActive, true)
                .orderByAsc(WhatsappTemplate::getCategory)
                .orderByAsc(WhatsappTemplate::getTitle));
    }

    public RenderWhatsappResponse renderWhatsappTemplate(RenderWhatsappRequest req) {
        SecurityUser current = SecurityUtils.currentUser();
        WhatsappTemplate tpl = whatsappTemplateMapper.selectOne(new LambdaQueryWrapper<WhatsappTemplate>()
                .eq(WhatsappTemplate::getCode, req.templateCode().trim().toUpperCase())
                .eq(WhatsappTemplate::getIsActive, true)
                .last("LIMIT 1"));
        if (tpl == null) {
            throw BusinessException.notFound("Template WhatsApp tidak ditemukan: " + req.templateCode());
        }

        Trip trip = null;
        Booking booking = null;
        if (req.tripId() != null && !req.tripId().isBlank()) {
            trip = tripMapper.selectById(req.tripId());
            if (trip == null) {
                throw BusinessException.notFound("Perjalanan tidak ditemukan.");
            }
            if (current.isDriver() && !SecurityUtils.requireDriverId().equals(trip.getDriverId())) {
                throw BusinessException.forbidden("Supir hanya dapat merender pesan untuk perjalanannya sendiri.");
            }
            booking = bookingMapper.selectById(trip.getBookingId());
        } else if (req.bookingId() != null && !req.bookingId().isBlank()) {
            booking = bookingMapper.selectById(req.bookingId());
            if (booking == null) {
                throw BusinessException.notFound("Booking tidak ditemukan.");
            }
            trip = tripMapper.selectOne(new LambdaQueryWrapper<Trip>()
                    .eq(Trip::getBookingId, booking.getId())
                    .orderByAsc(Trip::getLegNumber)
                    .last("LIMIT 1"));
            if (current.isDriver() && (trip == null || !SecurityUtils.requireDriverId().equals(trip.getDriverId()))) {
                throw BusinessException.forbidden("Supir tidak memiliki akses ke booking ini.");
            }
        }

        Customer customer = booking != null ? customerMapper.selectById(booking.getCustomerId()) : null;
        Driver driver = (trip != null && trip.getDriverId() != null) ? driverMapper.selectById(trip.getDriverId()) : null;
        Vehicle vehicle = (trip != null && trip.getVehicleId() != null) ? vehicleMapper.selectById(trip.getVehicleId()) : null;

        String customerName = customer != null ? customer.getName() : (booking != null ? booking.getContactName() : "Pelanggan");
        OffsetDateTime pickupDt = trip != null ? trip.getScheduledPickupAt() : (booking != null ? booking.getRequestedPickupAt() : null);
        String pickupTime = pickupDt != null ? pickupDt.atZoneSameInstant(IdGenerator.WIB).format(WA_TIME_FMT) + " WIB" : "-";
        String pickupAddr = trip != null ? trip.getPickupAddress() : (booking != null ? booking.getPickupAddress() : "-");
        String destAddr = trip != null ? trip.getDestinationAddress() : (booking != null ? booking.getDestinationAddress() : "-");
        String driverName = driver != null ? driver.getFullName() : "Tim Supir Mandiri Transport";
        String driverPhone = driver != null ? driver.getPhone() : "-";
        String vehiclePlate = vehicle != null ? vehicle.getRegistrationNumber() : "-";
        String vehicleModel = vehicle != null ? vehicle.getNameModel() : "-";
        BigDecimal fare = trip != null ? trip.getAgreedFareSnapshot() : (booking != null ? booking.getAgreedFare() : BigDecimal.ZERO);

        String rendered = tpl.getBodyTemplate()
                .replace("{customer_name}", safeStr(customerName))
                .replace("{pickup_time}", safeStr(pickupTime))
                .replace("{pickup_address}", safeStr(pickupAddr))
                .replace("{destination_address}", safeStr(destAddr))
                .replace("{driver_name}", safeStr(driverName))
                .replace("{driver_phone}", safeStr(driverPhone))
                .replace("{vehicle_plate}", safeStr(vehiclePlate))
                .replace("{vehicle_model}", safeStr(vehicleModel))
                .replace("{agreed_fare}", GeoAndLinkUtils.formatIdr(fare));

        String targetPhone = req.phone();
        if (targetPhone == null || targetPhone.isBlank()) {
            if (customer != null && customer.getWhatsappPhone() != null && !customer.getWhatsappPhone().isBlank()) {
                targetPhone = customer.getWhatsappPhone();
            } else if (booking != null && booking.getContactPhone() != null && !booking.getContactPhone().isBlank()) {
                targetPhone = booking.getContactPhone();
            } else if (customer != null) {
                targetPhone = customer.getPhone();
            }
        }

        String waUrl = GeoAndLinkUtils.buildWhatsappUrl(targetPhone, rendered);
        return new RenderWhatsappResponse(targetPhone, rendered, waUrl);
    }

    private Set<String> resolveDriverCustomerIds(String driverId) {
        List<Trip> driverTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getDriverId, driverId));
        if (driverTrips.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> bookingIds = driverTrips.stream()
                .map(Trip::getBookingId)
                .collect(Collectors.toSet());
        return bookingMapper.selectByIds(bookingIds).stream()
                .map(Booking::getCustomerId)
                .collect(Collectors.toSet());
    }

    private CustomerDetailDto toDetailDto(Customer c, boolean driverScoped) {
        List<CustomerAddress> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<CustomerAddress>()
                        .eq(CustomerAddress::getCustomerId, c.getId())
                        .orderByDesc(CustomerAddress::getIsDefault));

        LambdaQueryWrapper<CustomerNote> noteQw = new LambdaQueryWrapper<CustomerNote>()
                .eq(CustomerNote::getCustomerId, c.getId())
                .orderByDesc(CustomerNote::getCreatedAt);
        if (driverScoped) {
            noteQw.eq(CustomerNote::getVisibleToDrivers, true);
        }
        List<CustomerNote> notes = noteMapper.selectList(noteQw);

        List<CustomerPricingAgreement> agreements = driverScoped
                ? Collections.emptyList()
                : agreementMapper.selectList(new LambdaQueryWrapper<CustomerPricingAgreement>()
                .eq(CustomerPricingAgreement::getCustomerId, c.getId())
                .eq(CustomerPricingAgreement::getIsActive, true));

        String waPhone = (c.getWhatsappPhone() != null && !c.getWhatsappPhone().isBlank())
                ? c.getWhatsappPhone() : c.getPhone();

        return new CustomerDetailDto(
                c.getId(),
                c.getName(),
                c.getPhone(),
                c.getWhatsappPhone(),
                c.getCompanyName(),
                c.getPreferredVehicleType(),
                GeoAndLinkUtils.buildWhatsappUrl(waPhone, null),
                addresses,
                notes,
                agreements
        );
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }
}
