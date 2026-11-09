package com.mandiritransport.ops.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("notification_settings")
public class NotificationSetting {

    @TableId(type = IdType.INPUT)
    private String id;
    private String reminderType;
    private Boolean enabled;
    private Integer leadTimeMinutes;
    private String recipientScope;
    private Boolean acknowledgementEnabled;
    private OffsetDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getLeadTimeMinutes() { return leadTimeMinutes; }
    public void setLeadTimeMinutes(Integer leadTimeMinutes) { this.leadTimeMinutes = leadTimeMinutes; }
    public String getRecipientScope() { return recipientScope; }
    public void setRecipientScope(String recipientScope) { this.recipientScope = recipientScope; }
    public Boolean getAcknowledgementEnabled() { return acknowledgementEnabled; }
    public void setAcknowledgementEnabled(Boolean acknowledgementEnabled) { this.acknowledgementEnabled = acknowledgementEnabled; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
