package com.mandiritransport.ops.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.JwtService;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.auth.dto.AuthDtos.DevLoginRequest;
import com.mandiritransport.ops.auth.dto.AuthDtos.FcmTokenRequest;
import com.mandiritransport.ops.auth.dto.AuthDtos.LoginResponseDto;
import com.mandiritransport.ops.auth.dto.AuthDtos.UnregisterFcmTokenRequest;
import com.mandiritransport.ops.auth.dto.AuthDtos.UserAccountDto;
import com.mandiritransport.ops.auth.entity.AppUser;
import com.mandiritransport.ops.auth.mapper.AppUserMapper;
import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import com.mandiritransport.ops.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppUserMapper appUserMapper;
    private final DriverMapper driverMapper;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final boolean devAuthEnabled;

    public AuthController(AppUserMapper appUserMapper,
                          DriverMapper driverMapper,
                          JwtService jwtService,
                          NotificationService notificationService,
                          @Value("${app.security.dev-auth-enabled:false}") boolean devAuthEnabled) {
        this.appUserMapper = appUserMapper;
        this.driverMapper = driverMapper;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.devAuthEnabled = devAuthEnabled;
    }

    @PostMapping("/dev-login")
    public ApiResponse<LoginResponseDto> devLogin(@Valid @RequestBody DevLoginRequest req) {
        if (!devAuthEnabled) {
            throw BusinessException.forbidden("Login mode pengembangan dinonaktifkan di lingkungan ini.");
        }
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, req.username().trim().toLowerCase())
                .eq(AppUser::getIsActive, true)
                .last("LIMIT 1"));
        if (user == null) {
            throw BusinessException.notFound("Akun pengguna tidak ditemukan: " + req.username());
        }
        Driver drv = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getUserId, user.getId())
                .last("LIMIT 1"));
        String driverId = drv != null ? drv.getId() : null;
        String token = jwtService.generateToken(user, driverId);
        UserAccountDto userDto = new UserAccountDto(
                user.getId(), user.getUsername(), user.getFullName(), user.getPhone(), user.getRole(), driverId
        );
        return ApiResponse.ok(new LoginResponseDto(token, userDto));
    }

    @GetMapping("/accounts")
    public ApiResponse<List<UserAccountDto>> listAccounts() {
        if (!devAuthEnabled) {
            throw BusinessException.forbidden("Daftar akun cepat hanya tersedia di mode pengembangan.");
        }
        List<AppUser> users = appUserMapper.selectList(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getIsActive, true)
                .orderByAsc(AppUser::getRole)
                .orderByAsc(AppUser::getFullName));
        Map<String, String> driverByUserId = driverMapper.selectList(null).stream()
                .filter(d -> d.getUserId() != null)
                .collect(Collectors.toMap(Driver::getUserId, Driver::getId, (a, b) -> a));

        List<UserAccountDto> dtos = users.stream()
                .map(u -> new UserAccountDto(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getPhone(),
                        u.getRole(),
                        driverByUserId.get(u.getId())
                ))
                .toList();
        return ApiResponse.ok(dtos);
    }

    @GetMapping("/me")
    public ApiResponse<UserAccountDto> me() {
        SecurityUser su = SecurityUtils.currentUser();
        return ApiResponse.ok(new UserAccountDto(
                su.userId(), su.username(), su.fullName(), su.phone(), su.role(), su.driverId()
        ));
    }

    @PostMapping("/fcm-token")
    public ApiResponse<Map<String, Object>> updateFcmToken(@Valid @RequestBody FcmTokenRequest req) {
        SecurityUser su = SecurityUtils.currentUser();
        String deviceId = notificationService.registerDeviceToken(
                su.userId(),
                req.fcmToken(),
                req.platform(),
                req.deviceLabel()
        );
        return ApiResponse.ok(Map.of("updated", true, "deviceId", deviceId));
    }

    @PostMapping("/fcm-token/unregister")
    public ApiResponse<Map<String, Object>> unregisterFcmToken(@Valid @RequestBody UnregisterFcmTokenRequest req) {
        SecurityUser su = SecurityUtils.currentUser();
        boolean removed = notificationService.unregisterDeviceToken(su.userId(), req.fcmToken());
        return ApiResponse.ok(Map.of("unregistered", removed));
    }
}
