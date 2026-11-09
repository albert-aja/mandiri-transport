package com.mandiritransport.ops.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("trip_payments")
public class TripPayment {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tripId;
    private String clientMutationId;
    private BigDecimal amount;
    private String paymentMethod;
    private Boolean isRefund;
    private String collectedByUserId;
    private String collectedByRole;
    private String heldByDriverId;
    private BigDecimal handedOverAmount;
    private Boolean custodySettled;
    private OffsetDateTime paidAt;
    private String referenceNumber;
    private String receiptPath;
    private String notes;
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Boolean getIsRefund() { return isRefund; }
    public void setIsRefund(Boolean isRefund) { this.isRefund = isRefund; }
    public String getCollectedByUserId() { return collectedByUserId; }
    public void setCollectedByUserId(String collectedByUserId) { this.collectedByUserId = collectedByUserId; }
    public String getCollectedByRole() { return collectedByRole; }
    public void setCollectedByRole(String collectedByRole) { this.collectedByRole = collectedByRole; }
    public String getHeldByDriverId() { return heldByDriverId; }
    public void setHeldByDriverId(String heldByDriverId) { this.heldByDriverId = heldByDriverId; }
    public BigDecimal getHandedOverAmount() { return handedOverAmount; }
    public void setHandedOverAmount(BigDecimal handedOverAmount) { this.handedOverAmount = handedOverAmount; }
    public Boolean getCustodySettled() { return custodySettled; }
    public void setCustodySettled(Boolean custodySettled) { this.custodySettled = custodySettled; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
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
