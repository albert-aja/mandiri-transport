package com.mandiritransport.ops.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("user_fcm_devices")
public class UserFcmDevice {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String fcmToken;
    private String platform;
    private String deviceLabel;
    private Boolean isActive;
    private OffsetDateTime lastRegisteredAt;
    private OffsetDateTime lastPushAt;
    private String lastError;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public OffsetDateTime getLastRegisteredAt() { return lastRegisteredAt; }
    public void setLastRegisteredAt(OffsetDateTime lastRegisteredAt) { this.lastRegisteredAt = lastRegisteredAt; }
    public OffsetDateTime getLastPushAt() { return lastPushAt; }
    public void setLastPushAt(OffsetDateTime lastPushAt) { this.lastPushAt = lastPushAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

