package com.mandiritransport.ops.auth;

import com.mandiritransport.ops.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static SecurityUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser su) {
            return su;
        }
        return null;
    }

    public static SecurityUser currentUser() {
        SecurityUser su = currentUserOrNull();
        if (su == null) {
            throw BusinessException.unauthorized("Autentikasi diperlukan.");
        }
        return su;
    }

    public static SecurityUser requireOwner() {
        SecurityUser su = currentUser();
        if (!su.isOwner()) {
            throw BusinessException.forbidden("Hanya OWNER yang diizinkan melakukan tindakan ini.");
        }
        return su;
    }

    public static String requireDriverId() {
        SecurityUser su = currentUser();
        if (su.driverId() == null || su.driverId().isBlank()) {
            throw BusinessException.forbidden("Akun ini tidak terhubung dengan profil supir aktif.");
        }
        return su.driverId();
    }
}
