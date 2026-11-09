package com.mandiritransport.ops.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@TableName("vehicle_maintenance_schedules")
public class VehicleMaintenanceSchedule {

    @TableId(type = IdType.INPUT)
    private String id;
    private String vehicleId;
    private String maintenanceType;
    private BigDecimal intervalKm;
    private Integer intervalMonths;
    private BigDecimal lastDoneOdometerKm;
    private LocalDate lastDoneDate;
    private BigDecimal nextDueOdometerKm;
    private LocalDate nextDueDate;
    private BigDecimal estimatedCost;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }
    public BigDecimal getIntervalKm() { return intervalKm; }
    public void setIntervalKm(BigDecimal intervalKm) { this.intervalKm = intervalKm; }
    public Integer getIntervalMonths() { return intervalMonths; }
    public void setIntervalMonths(Integer intervalMonths) { this.intervalMonths = intervalMonths; }
    public BigDecimal getLastDoneOdometerKm() { return lastDoneOdometerKm; }
    public void setLastDoneOdometerKm(BigDecimal lastDoneOdometerKm) { this.lastDoneOdometerKm = lastDoneOdometerKm; }
    public LocalDate getLastDoneDate() { return lastDoneDate; }
    public void setLastDoneDate(LocalDate lastDoneDate) { this.lastDoneDate = lastDoneDate; }
    public BigDecimal getNextDueOdometerKm() { return nextDueOdometerKm; }
    public void setNextDueOdometerKm(BigDecimal nextDueOdometerKm) { this.nextDueOdometerKm = nextDueOdometerKm; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
