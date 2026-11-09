package com.mandiritransport.ops.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("fare_override_audits")
public class FareOverrideAudit {

    @TableId(type = IdType.INPUT)
    private String id;
    private String bookingId;
    private String tripId;
    private BigDecimal suggestedFare;
    private String pricingTierUsed;
    private BigDecimal finalAgreedFare;
    private String overrideReason;
    private String authorizedBy;
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public BigDecimal getSuggestedFare() { return suggestedFare; }
    public void setSuggestedFare(BigDecimal suggestedFare) { this.suggestedFare = suggestedFare; }
    public String getPricingTierUsed() { return pricingTierUsed; }
    public void setPricingTierUsed(String pricingTierUsed) { this.pricingTierUsed = pricingTierUsed; }
    public BigDecimal getFinalAgreedFare() { return finalAgreedFare; }
    public void setFinalAgreedFare(BigDecimal finalAgreedFare) { this.finalAgreedFare = finalAgreedFare; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
    public String getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
