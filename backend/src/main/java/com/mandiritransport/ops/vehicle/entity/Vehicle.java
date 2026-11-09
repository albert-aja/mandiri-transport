package com.mandiritransport.ops.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("vehicles")
public class Vehicle {

    @TableId(type = IdType.INPUT)
    private String id;
    private String registrationNumber;
    private String nameModel;
    private String brand;
    private Integer manufactureYear;
    private String vehicleType;
    private Integer passengerCapacity;
    private Integer luggageCapacityBags;
    private String status;
    private BigDecimal currentOdometerKm;
    private String fuelType;
    private String suitabilityTags;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;
    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getNameModel() { return nameModel; }
    public void setNameModel(String nameModel) { this.nameModel = nameModel; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Integer getManufactureYear() { return manufactureYear; }
    public void setManufactureYear(Integer manufactureYear) { this.manufactureYear = manufactureYear; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public Integer getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(Integer passengerCapacity) { this.passengerCapacity = passengerCapacity; }
    public Integer getLuggageCapacityBags() { return luggageCapacityBags; }
    public void setLuggageCapacityBags(Integer luggageCapacityBags) { this.luggageCapacityBags = luggageCapacityBags; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getCurrentOdometerKm() { return currentOdometerKm; }
    public void setCurrentOdometerKm(BigDecimal currentOdometerKm) { this.currentOdometerKm = currentOdometerKm; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public String getSuitabilityTags() { return suitabilityTags; }
    public void setSuitabilityTags(String suitabilityTags) { this.suitabilityTags = suitabilityTags; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
