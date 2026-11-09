package com.mandiritransport.ops.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("bookings")
public class Booking {

    @TableId(type = IdType.INPUT)
    private String id;
    private String bookingCode;
    private String customerId;
    private String contactName;
    private String contactPhone;
    private String bookingSource;
    private String tripType;
    private String routeId;
    private OffsetDateTime requestedPickupAt;
    private OffsetDateTime returnPickupAt;
    private Integer charterHours;
    private String pickupAddress;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private String destinationAddress;
    private BigDecimal destinationLat;
    private BigDecimal destinationLng;
    private Integer passengerCount;
    private Integer luggageCount;
    private String luggageNotes;
    private String flightNumber;
    private OffsetDateTime flightScheduledAt;
    private BigDecimal suggestedFare;
    private String pricingTierUsed;
    private String pricingExplanation;
    private BigDecimal agreedFare;
    private String specialInstructions;
    private String status;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;
    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getBookingSource() { return bookingSource; }
    public void setBookingSource(String bookingSource) { this.bookingSource = bookingSource; }
    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public OffsetDateTime getRequestedPickupAt() { return requestedPickupAt; }
    public void setRequestedPickupAt(OffsetDateTime requestedPickupAt) { this.requestedPickupAt = requestedPickupAt; }
    public OffsetDateTime getReturnPickupAt() { return returnPickupAt; }
    public void setReturnPickupAt(OffsetDateTime returnPickupAt) { this.returnPickupAt = returnPickupAt; }
    public Integer getCharterHours() { return charterHours; }
    public void setCharterHours(Integer charterHours) { this.charterHours = charterHours; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public BigDecimal getPickupLat() { return pickupLat; }
    public void setPickupLat(BigDecimal pickupLat) { this.pickupLat = pickupLat; }
    public BigDecimal getPickupLng() { return pickupLng; }
    public void setPickupLng(BigDecimal pickupLng) { this.pickupLng = pickupLng; }
    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
    public BigDecimal getDestinationLat() { return destinationLat; }
    public void setDestinationLat(BigDecimal destinationLat) { this.destinationLat = destinationLat; }
    public BigDecimal getDestinationLng() { return destinationLng; }
    public void setDestinationLng(BigDecimal destinationLng) { this.destinationLng = destinationLng; }
    public Integer getPassengerCount() { return passengerCount; }
    public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }
    public Integer getLuggageCount() { return luggageCount; }
    public void setLuggageCount(Integer luggageCount) { this.luggageCount = luggageCount; }
    public String getLuggageNotes() { return luggageNotes; }
    public void setLuggageNotes(String luggageNotes) { this.luggageNotes = luggageNotes; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public OffsetDateTime getFlightScheduledAt() { return flightScheduledAt; }
    public void setFlightScheduledAt(OffsetDateTime flightScheduledAt) { this.flightScheduledAt = flightScheduledAt; }
    public BigDecimal getSuggestedFare() { return suggestedFare; }
    public void setSuggestedFare(BigDecimal suggestedFare) { this.suggestedFare = suggestedFare; }
    public String getPricingTierUsed() { return pricingTierUsed; }
    public void setPricingTierUsed(String pricingTierUsed) { this.pricingTierUsed = pricingTierUsed; }
    public String getPricingExplanation() { return pricingExplanation; }
    public void setPricingExplanation(String pricingExplanation) { this.pricingExplanation = pricingExplanation; }
    public BigDecimal getAgreedFare() { return agreedFare; }
    public void setAgreedFare(BigDecimal agreedFare) { this.agreedFare = agreedFare; }
    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
