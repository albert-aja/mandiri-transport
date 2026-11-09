package com.mandiritransport.ops.notification.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.notification.dto.NotificationDtos.FcmStatusDto;
import com.mandiritransport.ops.notification.dto.NotificationDtos.NotificationListDto;
import com.mandiritransport.ops.notification.dto.NotificationDtos.TestPushRequest;
import com.mandiritransport.ops.notification.dto.NotificationDtos.TestPushResponse;
import com.mandiritransport.ops.notification.dto.NotificationDtos.UpdateNotificationSettingRequest;
import com.mandiritransport.ops.notification.entity.Notification;
import com.mandiritransport.ops.notification.entity.NotificationSetting;
import com.mandiritransport.ops.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<NotificationListDto> listMyNotifications() {
        return ApiResponse.ok(notificationService.listMyNotifications());
    }

    @PostMapping("/{id}/acknowledge")
    public ApiResponse<Notification> acknowledge(@PathVariable String id) {
        return ApiResponse.ok(notificationService.acknowledge(id));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<List<NotificationSetting>> listSettings() {
        return ApiResponse.ok(notificationService.listSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<List<NotificationSetting>> updateSettings(
            @Valid @RequestBody List<UpdateNotificationSettingRequest> updates) {
        return ApiResponse.ok(notificationService.updateSettings(updates));
    }

    @GetMapping("/fcm-status")
    public ApiResponse<FcmStatusDto> getFcmStatus() {
        return ApiResponse.ok(notificationService.getFcmStatus());
    }

    @PostMapping("/test-push")
    public ApiResponse<TestPushResponse> sendTestPush(@RequestBody(required = false) TestPushRequest req) {
        return ApiResponse.ok(notificationService.sendTestPush(req));
    }

    @PostMapping("/scan-reminders")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<Map<String, Object>> triggerReminderScan() {
        int generated = notificationService.scanOperationalReminders();
        return ApiResponse.ok(Map.of("generatedCount", generated));
    }
}
