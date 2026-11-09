package com.mandiritransport.ops.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("trip_expenses")
public class TripExpense {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tripId;
    private String clientMutationId;
    private String category;
    private BigDecimal amount;
    private String paidByUserId;
    private String paidByRole;
    private BigDecimal fuelLiters;
    private String receiptPath;
    private String notes;
    private OffsetDateTime incurredAt;
    private Boolean isVoided;
    private String voidedBy;
    private String voidReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getClientMutationId() { return clientMutationId; }
    public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaidByUserId() { return paidByUserId; }
    public void setPaidByUserId(String paidByUserId) { this.paidByUserId = paidByUserId; }
    public String getPaidByRole() { return paidByRole; }
    public void setPaidByRole(String paidByRole) { this.paidByRole = paidByRole; }
    public BigDecimal getFuelLiters() { return fuelLiters; }
    public void setFuelLiters(BigDecimal fuelLiters) { this.fuelLiters = fuelLiters; }
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getIncurredAt() { return incurredAt; }
    public void setIncurredAt(OffsetDateTime incurredAt) { this.incurredAt = incurredAt; }
    public Boolean getIsVoided() { return isVoided; }
    public void setIsVoided(Boolean isVoided) { this.isVoided = isVoided; }
    public String getVoidedBy() { return voidedBy; }
    public void setVoidedBy(String voidedBy) { this.voidedBy = voidedBy; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
