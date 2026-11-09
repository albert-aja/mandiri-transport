package com.mandiritransport.ops.pricing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("standard_pricing_configs")
public class StandardPricingConfig {

    @TableId(type = IdType.INPUT)
    private String id;
    private String vehicleType;
    private BigDecimal baseFare;
    private BigDecimal perKmRate;
    private BigDecimal hourlyCharterRate;
    private BigDecimal fullDayCharterRate;
    private BigDecimal minimumFare;
    private Boolean isActive;
    private OffsetDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }
    public BigDecimal getPerKmRate() { return perKmRate; }
    public void setPerKmRate(BigDecimal perKmRate) { this.perKmRate = perKmRate; }
    public BigDecimal getHourlyCharterRate() { return hourlyCharterRate; }
    public void setHourlyCharterRate(BigDecimal hourlyCharterRate) { this.hourlyCharterRate = hourlyCharterRate; }
    public BigDecimal getFullDayCharterRate() { return fullDayCharterRate; }
    public void setFullDayCharterRate(BigDecimal fullDayCharterRate) { this.fullDayCharterRate = fullDayCharterRate; }
    public BigDecimal getMinimumFare() { return minimumFare; }
    public void setMinimumFare(BigDecimal minimumFare) { this.minimumFare = minimumFare; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
