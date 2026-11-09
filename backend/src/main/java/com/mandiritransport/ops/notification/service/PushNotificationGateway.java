package com.mandiritransport.ops.notification.service;

public interface PushNotificationGateway {

    record PushDeliveryResult(
            boolean delivered,
            boolean liveGatewayActive,
            boolean tokenUnregistered,
            String messageId,
            String errorMessage
    ) {
        public static PushDeliveryResult sent(String messageId) {
            return new PushDeliveryResult(true, true, false, messageId, null);
        }

        public static PushDeliveryResult inAppOnly(String reason) {
            return new PushDeliveryResult(false, false, false, null, reason);
        }

        public static PushDeliveryResult failed(boolean tokenUnregistered, String error) {
            return new PushDeliveryResult(false, true, tokenUnregistered, null, error);
        }
    }

    record GatewayDiagnostics(
            boolean fcmEnabled,
            boolean liveCredentialsReady,
            String credentialSource,
            String projectId,
            String clientEmailMasked,
            String androidChannelId,
            String statusSummary
    ) {
    }

    PushDeliveryResult sendPush(String fcmToken,
                                String notificationId,
                                String reminderType,
                                String title,
                                String body,
                                String referenceType,
                                String referenceId);

    GatewayDiagnostics getDiagnostics();
}
