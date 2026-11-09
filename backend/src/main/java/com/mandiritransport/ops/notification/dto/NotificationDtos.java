package com.mandiritransport.ops.notification.dto;

import com.mandiritransport.ops.notification.entity.Notification;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationListDto(
            long unreadCount,
            List<Notification> notifications
    ) {
    }

    public record UpdateNotificationSettingRequest(
            @NotBlank(message = "reminderType wajib diisi") String reminderType,
            Boolean enabled,
            Integer leadTimeMinutes,
            String recipientScope,
            Boolean acknowledgementEnabled
    ) {
    }

    public record FcmDeviceItemDto(
            String id,
            String platform,
            String deviceLabel,
            String tokenPreview,
            Boolean isActive,
            String lastRegisteredAt,
            String lastPushAt
    ) {
    }

    public record FcmStatusDto(
            boolean fcmEnabled,
            boolean liveCredentialsReady,
            String credentialSource,
            String projectId,
            String clientEmailMasked,
            String androidChannelId,
            int myActiveDevicesCount,
            int fleetActiveDevicesCount,
            List<FcmDeviceItemDto> myDevices,
            String statusSummary
    ) {
    }

    public record TestPushRequest(
            String targetUserId,
            String title,
            String body
    ) {
    }

    public record TestPushResponse(
            String notificationId,
            String fcmDeliveryStatus,
            int devicesTargeted,
            int devicesDelivered,
            String fcmMessageId,
            String diagnosticMessage
    ) {
    }
}
