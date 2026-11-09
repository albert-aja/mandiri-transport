package com.mandiritransport.ops.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("cash_handover_items")
public class CashHandoverItem {

    @TableId(type = IdType.INPUT)
    private String id;
    private String handoverId;
    private String tripPaymentId;
    private String tripId;
    private BigDecimal amountApplied;
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHandoverId() { return handoverId; }
    public void setHandoverId(String handoverId) { this.handoverId = handoverId; }
    public String getTripPaymentId() { return tripPaymentId; }
    public void setTripPaymentId(String tripPaymentId) { this.tripPaymentId = tripPaymentId; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public BigDecimal getAmountApplied() { return amountApplied; }
    public void setAmountApplied(BigDecimal amountApplied) { this.amountApplied = amountApplied; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
