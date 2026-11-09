package com.mandiritransport.ops.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("booking_templates")
public class BookingTemplate {

    @TableId(type = IdType.INPUT)
    private String id;
    private String name;
    private String customerId;
    private String routeId;
    private String tripType;
    private String bookingSource;
    private String pickupAddress;
    private String destinationAddress;
    private Integer defaultPassengerCount;
    private Integer defaultLuggageCount;
    private String luggageNotes;
    private String preferredVehicleType;
    private String notes;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }
    public String getBookingSource() { return bookingSource; }
    public void setBookingSource(String bookingSource) { this.bookingSource = bookingSource; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
    public Integer getDefaultPassengerCount() { return defaultPassengerCount; }
    public void setDefaultPassengerCount(Integer defaultPassengerCount) { this.defaultPassengerCount = defaultPassengerCount; }
    public Integer getDefaultLuggageCount() { return defaultLuggageCount; }
    public void setDefaultLuggageCount(Integer defaultLuggageCount) { this.defaultLuggageCount = defaultLuggageCount; }
    public String getLuggageNotes() { return luggageNotes; }
    public void setLuggageNotes(String luggageNotes) { this.luggageNotes = luggageNotes; }
    public String getPreferredVehicleType() { return preferredVehicleType; }
    public void setPreferredVehicleType(String preferredVehicleType) { this.preferredVehicleType = preferredVehicleType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
