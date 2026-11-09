package com.mandiritransport.ops.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("routes")
public class Route {

    @TableId(type = IdType.INPUT)
    private String id;
    private String code;
    private String name;
    private String originName;
    private String destinationName;
    private BigDecimal typicalDistanceKm;
    private Integer defaultDurationMin;
    private Integer defaultPrepBufferMin;
    private Integer defaultArrivalBufferMin;
    private Integer defaultRepositionBufferMin;
    private Boolean isLongDistance;
    private Integer requiredRestAfterMin;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOriginName() { return originName; }
    public void setOriginName(String originName) { this.originName = originName; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public BigDecimal getTypicalDistanceKm() { return typicalDistanceKm; }
    public void setTypicalDistanceKm(BigDecimal typicalDistanceKm) { this.typicalDistanceKm = typicalDistanceKm; }
    public Integer getDefaultDurationMin() { return defaultDurationMin; }
    public void setDefaultDurationMin(Integer defaultDurationMin) { this.defaultDurationMin = defaultDurationMin; }
    public Integer getDefaultPrepBufferMin() { return defaultPrepBufferMin; }
    public void setDefaultPrepBufferMin(Integer defaultPrepBufferMin) { this.defaultPrepBufferMin = defaultPrepBufferMin; }
    public Integer getDefaultArrivalBufferMin() { return defaultArrivalBufferMin; }
    public void setDefaultArrivalBufferMin(Integer defaultArrivalBufferMin) { this.defaultArrivalBufferMin = defaultArrivalBufferMin; }
    public Integer getDefaultRepositionBufferMin() { return defaultRepositionBufferMin; }
    public void setDefaultRepositionBufferMin(Integer defaultRepositionBufferMin) { this.defaultRepositionBufferMin = defaultRepositionBufferMin; }
    public Boolean getIsLongDistance() { return isLongDistance; }
    public void setIsLongDistance(Boolean isLongDistance) { this.isLongDistance = isLongDistance; }
    public Integer getRequiredRestAfterMin() { return requiredRestAfterMin; }
    public void setRequiredRestAfterMin(Integer requiredRestAfterMin) { this.requiredRestAfterMin = requiredRestAfterMin; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
