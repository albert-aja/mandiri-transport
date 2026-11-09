package com.mandiritransport.ops.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("trip_cost_snapshots")
public class TripCostSnapshot {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tripId;
    private String formulaVersionId;
    private Integer formulaVersionNumber;
    private BigDecimal distanceKmUsed;
    private String distanceSource;
    private BigDecimal durationHoursUsed;
    private BigDecimal agreedFareSnapshot;
    private BigDecimal estimatedOperatingCost;
    private BigDecimal actualExpensesTotal;
    private BigDecimal allocatedOperatingCost;
    private BigDecimal totalEffectiveCost;
    private BigDecimal estimatedProfit;
    private BigDecimal actualProfit;
    private OffsetDateTime calculatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getFormulaVersionId() { return formulaVersionId; }
    public void setFormulaVersionId(String formulaVersionId) { this.formulaVersionId = formulaVersionId; }
    public Integer getFormulaVersionNumber() { return formulaVersionNumber; }
    public void setFormulaVersionNumber(Integer formulaVersionNumber) { this.formulaVersionNumber = formulaVersionNumber; }
    public BigDecimal getDistanceKmUsed() { return distanceKmUsed; }
    public void setDistanceKmUsed(BigDecimal distanceKmUsed) { this.distanceKmUsed = distanceKmUsed; }
    public String getDistanceSource() { return distanceSource; }
    public void setDistanceSource(String distanceSource) { this.distanceSource = distanceSource; }
    public BigDecimal getDurationHoursUsed() { return durationHoursUsed; }
    public void setDurationHoursUsed(BigDecimal durationHoursUsed) { this.durationHoursUsed = durationHoursUsed; }
    public BigDecimal getAgreedFareSnapshot() { return agreedFareSnapshot; }
    public void setAgreedFareSnapshot(BigDecimal agreedFareSnapshot) { this.agreedFareSnapshot = agreedFareSnapshot; }
    public BigDecimal getEstimatedOperatingCost() { return estimatedOperatingCost; }
    public void setEstimatedOperatingCost(BigDecimal estimatedOperatingCost) { this.estimatedOperatingCost = estimatedOperatingCost; }
    public BigDecimal getActualExpensesTotal() { return actualExpensesTotal; }
    public void setActualExpensesTotal(BigDecimal actualExpensesTotal) { this.actualExpensesTotal = actualExpensesTotal; }
    public BigDecimal getAllocatedOperatingCost() { return allocatedOperatingCost; }
    public void setAllocatedOperatingCost(BigDecimal allocatedOperatingCost) { this.allocatedOperatingCost = allocatedOperatingCost; }
    public BigDecimal getTotalEffectiveCost() { return totalEffectiveCost; }
    public void setTotalEffectiveCost(BigDecimal totalEffectiveCost) { this.totalEffectiveCost = totalEffectiveCost; }
    public BigDecimal getEstimatedProfit() { return estimatedProfit; }
    public void setEstimatedProfit(BigDecimal estimatedProfit) { this.estimatedProfit = estimatedProfit; }
    public BigDecimal getActualProfit() { return actualProfit; }
    public void setActualProfit(BigDecimal actualProfit) { this.actualProfit = actualProfit; }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
