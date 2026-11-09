package com.mandiritransport.ops.intelligence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("anomaly_rules")
public class AnomalyRule {

    @TableId(type = IdType.INPUT)
    private String id;
    private String ruleCode;
    private String name;
    private Boolean enabled;
    private BigDecimal thresholdMultiplier;
    private BigDecimal thresholdAbsolute;
    private Integer minSampleSize;
    private String description;
    private OffsetDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public BigDecimal getThresholdMultiplier() { return thresholdMultiplier; }
    public void setThresholdMultiplier(BigDecimal thresholdMultiplier) { this.thresholdMultiplier = thresholdMultiplier; }
    public BigDecimal getThresholdAbsolute() { return thresholdAbsolute; }
    public void setThresholdAbsolute(BigDecimal thresholdAbsolute) { this.thresholdAbsolute = thresholdAbsolute; }
    public Integer getMinSampleSize() { return minSampleSize; }
    public void setMinSampleSize(Integer minSampleSize) { this.minSampleSize = minSampleSize; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
