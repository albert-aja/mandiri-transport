package com.mandiritransport.ops.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("vehicle_inspections")
public class VehicleInspection {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tripId;
    private String vehicleId;
    private String driverId;
    private String clientMutationId;
    private String stage;
    private Boolean allPassed;
    private String flaggedItemsJson;
    private String photoPathsJson;
    private String notes;
    private OffsetDateTime inspectedAt;
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getClientMutationId() { return clientMutationId; }
    public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public Boolean getAllPassed() { return allPassed; }
    public void setAllPassed(Boolean allPassed) { this.allPassed = allPassed; }
    public String getFlaggedItemsJson() { return flaggedItemsJson; }
    public void setFlaggedItemsJson(String flaggedItemsJson) { this.flaggedItemsJson = flaggedItemsJson; }
    public String getPhotoPathsJson() { return photoPathsJson; }
    public void setPhotoPathsJson(String photoPathsJson) { this.photoPathsJson = photoPathsJson; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(OffsetDateTime inspectedAt) { this.inspectedAt = inspectedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
