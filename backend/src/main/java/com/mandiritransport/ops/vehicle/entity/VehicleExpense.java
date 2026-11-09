package com.mandiritransport.ops.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@TableName("vehicle_expenses")
public class VehicleExpense {

    @TableId(type = IdType.INPUT)
    private String id;
    private String vehicleId;
    private String maintenanceLogId;
    private String category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
    private String receiptPath;
    private String recordedBy;
    private Boolean isVoided;
    private String voidReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getMaintenanceLogId() { return maintenanceLogId; }
    public void setMaintenanceLogId(String maintenanceLogId) { this.maintenanceLogId = maintenanceLogId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public Boolean getIsVoided() { return isVoided; }
    public void setIsVoided(Boolean isVoided) { this.isVoided = isVoided; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
