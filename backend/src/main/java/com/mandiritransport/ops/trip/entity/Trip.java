package com.mandiritransport.ops.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("trips")
public class Trip {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tripCode;
    private String bookingId;
    private Integer legNumber;
    private String legLabel;
    private String routeId;
    private String tripType;
    private String driverId;
    private String vehicleId;
    private OffsetDateTime scheduledPickupAt;
    private Integer estimatedDurationMin;
    private Integer prepBufferMin;
    private Integer arrivalBufferMin;
    private Integer repositionBufferMin;
    private OffsetDateTime reservedStartAt;
    private OffsetDateTime reservedEndAt;
    private String pickupAddress;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private String destinationAddress;
    private BigDecimal destinationLat;
    private BigDecimal destinationLng;
    private Integer passengerCount;
    private Integer luggageCount;
    private String flightNumber;
    private BigDecimal agreedFareSnapshot;
    private String status;
    private String cancelOrOutcomeReason;
    private String conflictOverrideNote;
    private Boolean trackingActive;
    private OffsetDateTime trackingStartedAt;
    private OffsetDateTime trackingEndedAt;
    private OffsetDateTime lastGpsRecordedAt;
    private BigDecimal lastGpsLat;
    private BigDecimal lastGpsLng;
    private BigDecimal gpsDistanceKm;
    private BigDecimal odometerStartKm;
    private BigDecimal odometerEndKm;
    private OffsetDateTime actualStartedAt;
    private OffsetDateTime actualCompletedAt;
    private Integer actualDurationMin;
    private String completionNotes;
    private String paymentStatus;
    private BigDecimal totalPaidAmount;
    private String cashCustodyStatus;
    private BigDecimal driverCashHeldAmount;
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
    public String getTripCode() { return tripCode; }
    public void setTripCode(String tripCode) { this.tripCode = tripCode; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public Integer getLegNumber() { return legNumber; }
    public void setLegNumber(Integer legNumber) { this.legNumber = legNumber; }
    public String getLegLabel() { return legLabel; }
    public void setLegLabel(String legLabel) { this.legLabel = legLabel; }
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public OffsetDateTime getScheduledPickupAt() { return scheduledPickupAt; }
    public void setScheduledPickupAt(OffsetDateTime scheduledPickupAt) { this.scheduledPickupAt = scheduledPickupAt; }
    public Integer getEstimatedDurationMin() { return estimatedDurationMin; }
    public void setEstimatedDurationMin(Integer estimatedDurationMin) { this.estimatedDurationMin = estimatedDurationMin; }
    public Integer getPrepBufferMin() { return prepBufferMin; }
    public void setPrepBufferMin(Integer prepBufferMin) { this.prepBufferMin = prepBufferMin; }
    public Integer getArrivalBufferMin() { return arrivalBufferMin; }
    public void setArrivalBufferMin(Integer arrivalBufferMin) { this.arrivalBufferMin = arrivalBufferMin; }
    public Integer getRepositionBufferMin() { return repositionBufferMin; }
    public void setRepositionBufferMin(Integer repositionBufferMin) { this.repositionBufferMin = repositionBufferMin; }
    public OffsetDateTime getReservedStartAt() { return reservedStartAt; }
    public void setReservedStartAt(OffsetDateTime reservedStartAt) { this.reservedStartAt = reservedStartAt; }
    public OffsetDateTime getReservedEndAt() { return reservedEndAt; }
    public void setReservedEndAt(OffsetDateTime reservedEndAt) { this.reservedEndAt = reservedEndAt; }
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
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public BigDecimal getAgreedFareSnapshot() { return agreedFareSnapshot; }
    public void setAgreedFareSnapshot(BigDecimal agreedFareSnapshot) { this.agreedFareSnapshot = agreedFareSnapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCancelOrOutcomeReason() { return cancelOrOutcomeReason; }
    public void setCancelOrOutcomeReason(String cancelOrOutcomeReason) { this.cancelOrOutcomeReason = cancelOrOutcomeReason; }
    public String getConflictOverrideNote() { return conflictOverrideNote; }
    public void setConflictOverrideNote(String conflictOverrideNote) { this.conflictOverrideNote = conflictOverrideNote; }
    public Boolean getTrackingActive() { return trackingActive; }
    public void setTrackingActive(Boolean trackingActive) { this.trackingActive = trackingActive; }
    public OffsetDateTime getTrackingStartedAt() { return trackingStartedAt; }
    public void setTrackingStartedAt(OffsetDateTime trackingStartedAt) { this.trackingStartedAt = trackingStartedAt; }
    public OffsetDateTime getTrackingEndedAt() { return trackingEndedAt; }
    public void setTrackingEndedAt(OffsetDateTime trackingEndedAt) { this.trackingEndedAt = trackingEndedAt; }
    public OffsetDateTime getLastGpsRecordedAt() { return lastGpsRecordedAt; }
    public void setLastGpsRecordedAt(OffsetDateTime lastGpsRecordedAt) { this.lastGpsRecordedAt = lastGpsRecordedAt; }
    public BigDecimal getLastGpsLat() { return lastGpsLat; }
    public void setLastGpsLat(BigDecimal lastGpsLat) { this.lastGpsLat = lastGpsLat; }
    public BigDecimal getLastGpsLng() { return lastGpsLng; }
    public void setLastGpsLng(BigDecimal lastGpsLng) { this.lastGpsLng = lastGpsLng; }
    public BigDecimal getGpsDistanceKm() { return gpsDistanceKm; }
    public void setGpsDistanceKm(BigDecimal gpsDistanceKm) { this.gpsDistanceKm = gpsDistanceKm; }
    public BigDecimal getOdometerStartKm() { return odometerStartKm; }
    public void setOdometerStartKm(BigDecimal odometerStartKm) { this.odometerStartKm = odometerStartKm; }
    public BigDecimal getOdometerEndKm() { return odometerEndKm; }
    public void setOdometerEndKm(BigDecimal odometerEndKm) { this.odometerEndKm = odometerEndKm; }
    public OffsetDateTime getActualStartedAt() { return actualStartedAt; }
    public void setActualStartedAt(OffsetDateTime actualStartedAt) { this.actualStartedAt = actualStartedAt; }
    public OffsetDateTime getActualCompletedAt() { return actualCompletedAt; }
    public void setActualCompletedAt(OffsetDateTime actualCompletedAt) { this.actualCompletedAt = actualCompletedAt; }
    public Integer getActualDurationMin() { return actualDurationMin; }
    public void setActualDurationMin(Integer actualDurationMin) { this.actualDurationMin = actualDurationMin; }
    public String getCompletionNotes() { return completionNotes; }
    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
    public void setTotalPaidAmount(BigDecimal totalPaidAmount) { this.totalPaidAmount = totalPaidAmount; }
    public String getCashCustodyStatus() { return cashCustodyStatus; }
    public void setCashCustodyStatus(String cashCustodyStatus) { this.cashCustodyStatus = cashCustodyStatus; }
    public BigDecimal getDriverCashHeldAmount() { return driverCashHeldAmount; }
    public void setDriverCashHeldAmount(BigDecimal driverCashHeldAmount) { this.driverCashHeldAmount = driverCashHeldAmount; }
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
