package com.mandiritransport.ops.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("cash_handovers")
public class CashHandover {

    @TableId(type = IdType.INPUT)
    private String id;
    private String handoverCode;
    private String driverId;
    private String receivedByOwnerId;
    private BigDecimal declaredAmount;
    private BigDecimal confirmedAmount;
    private String status;
    private String driverNotes;
    private String ownerNotes;
    private OffsetDateTime handedOverAt;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHandoverCode() { return handoverCode; }
    public void setHandoverCode(String handoverCode) { this.handoverCode = handoverCode; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getReceivedByOwnerId() { return receivedByOwnerId; }
    public void setReceivedByOwnerId(String receivedByOwnerId) { this.receivedByOwnerId = receivedByOwnerId; }
    public BigDecimal getDeclaredAmount() { return declaredAmount; }
    public void setDeclaredAmount(BigDecimal declaredAmount) { this.declaredAmount = declaredAmount; }
    public BigDecimal getConfirmedAmount() { return confirmedAmount; }
    public void setConfirmedAmount(BigDecimal confirmedAmount) { this.confirmedAmount = confirmedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDriverNotes() { return driverNotes; }
    public void setDriverNotes(String driverNotes) { this.driverNotes = driverNotes; }
    public String getOwnerNotes() { return ownerNotes; }
    public void setOwnerNotes(String ownerNotes) { this.ownerNotes = ownerNotes; }
    public OffsetDateTime getHandedOverAt() { return handedOverAt; }
    public void setHandedOverAt(OffsetDateTime handedOverAt) { this.handedOverAt = handedOverAt; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
