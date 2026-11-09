package com.mandiritransport.ops.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("trip_stops")
public class TripStop {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tripId;
    private Integer stopOrder;
    private String stopAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime plannedArrivalAt;
    private String notes;
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public Integer getStopOrder() { return stopOrder; }
    public void setStopOrder(Integer stopOrder) { this.stopOrder = stopOrder; }
    public String getStopAddress() { return stopAddress; }
    public void setStopAddress(String stopAddress) { this.stopAddress = stopAddress; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public OffsetDateTime getPlannedArrivalAt() { return plannedArrivalAt; }
    public void setPlannedArrivalAt(OffsetDateTime plannedArrivalAt) { this.plannedArrivalAt = plannedArrivalAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
