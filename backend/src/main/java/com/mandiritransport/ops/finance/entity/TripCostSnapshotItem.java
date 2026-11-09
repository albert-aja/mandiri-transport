package com.mandiritransport.ops.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("trip_cost_snapshot_items")
public class TripCostSnapshotItem {

    @TableId(type = IdType.INPUT)
    private String id;
    private String snapshotId;
    private String componentCode;
    private String componentName;
    private String reconciliationPolicy;
    private BigDecimal estimatedAmount;
    private BigDecimal actualExpenseAmount;
    private BigDecimal effectiveCostAmount;
    private String calculationDetail;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String snapshotId) { this.snapshotId = snapshotId; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getReconciliationPolicy() { return reconciliationPolicy; }
    public void setReconciliationPolicy(String reconciliationPolicy) { this.reconciliationPolicy = reconciliationPolicy; }
    public BigDecimal getEstimatedAmount() { return estimatedAmount; }
    public void setEstimatedAmount(BigDecimal estimatedAmount) { this.estimatedAmount = estimatedAmount; }
    public BigDecimal getActualExpenseAmount() { return actualExpenseAmount; }
    public void setActualExpenseAmount(BigDecimal actualExpenseAmount) { this.actualExpenseAmount = actualExpenseAmount; }
    public BigDecimal getEffectiveCostAmount() { return effectiveCostAmount; }
    public void setEffectiveCostAmount(BigDecimal effectiveCostAmount) { this.effectiveCostAmount = effectiveCostAmount; }
    public String getCalculationDetail() { return calculationDetail; }
    public void setCalculationDetail(String calculationDetail) { this.calculationDetail = calculationDetail; }
}
