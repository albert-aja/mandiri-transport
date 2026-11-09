package com.mandiritransport.ops.auth.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record DevLoginRequest(@NotBlank(message = "username wajib diisi") String username) {
    }

    public record FcmTokenRequest(
            @NotBlank(message = "fcmToken wajib diisi") String fcmToken,
            String platform,
            String deviceLabel
    ) {
    }

    public record UnregisterFcmTokenRequest(
            @NotBlank(message = "fcmToken wajib diisi") String fcmToken
    ) {
    }

    public record UserAccountDto(
            String id,
            String username,
            String fullName,
            String phone,
            String role,
            String driverId
    ) {
    }

    public record LoginResponseDto(
            String token,
            UserAccountDto user
    ) {
    }
}
