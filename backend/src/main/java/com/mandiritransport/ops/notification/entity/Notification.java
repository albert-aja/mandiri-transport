package com.mandiritransport.ops.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("notifications")
public class Notification {

    @TableId(type = IdType.INPUT)
    private String id;
    private String recipientUserId;
    private String recipientRole;
    private String reminderType;
    private String referenceType;
    private String referenceId;
    private String title;
    private String body;
    private OffsetDateTime scheduledFor;
    private OffsetDateTime dispatchedAt;
    private String status;
    private Boolean isRead;
    private OffsetDateTime acknowledgedAt;
    private String fcmDeliveryStatus;
    private String fcmMessageId;
    private String fcmError;
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(String recipientUserId) { this.recipientUserId = recipientUserId; }
    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public OffsetDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(OffsetDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    public OffsetDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(OffsetDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public OffsetDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public String getFcmDeliveryStatus() { return fcmDeliveryStatus; }
    public void setFcmDeliveryStatus(String fcmDeliveryStatus) { this.fcmDeliveryStatus = fcmDeliveryStatus; }
    public String getFcmMessageId() { return fcmMessageId; }
    public void setFcmMessageId(String fcmMessageId) { this.fcmMessageId = fcmMessageId; }
    public String getFcmError() { return fcmError; }
    public void setFcmError(String fcmError) { this.fcmError = fcmError; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
