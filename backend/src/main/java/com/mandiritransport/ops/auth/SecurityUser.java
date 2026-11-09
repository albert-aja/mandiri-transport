package com.mandiritransport.ops.auth;

public record SecurityUser(
        String userId,
        String username,
        String fullName,
        String phone,
        String role,
        String driverId
) {
    public boolean isOwner() {
        return "OWNER".equalsIgnoreCase(role);
    }

    public boolean isDriver() {
        return "DRIVER".equalsIgnoreCase(role);
    }
}
