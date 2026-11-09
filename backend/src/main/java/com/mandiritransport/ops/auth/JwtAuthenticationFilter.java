package com.mandiritransport.ops.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mandiritransport.ops.auth.entity.AppUser;
import com.mandiritransport.ops.auth.mapper.AppUserMapper;
import com.mandiritransport.ops.driver.entity.Driver;
import com.mandiritransport.ops.driver.mapper.DriverMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserMapper appUserMapper;
    private final DriverMapper driverMapper;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserMapper appUserMapper, DriverMapper driverMapper) {
        this.jwtService = jwtService;
        this.appUserMapper = appUserMapper;
        this.driverMapper = driverMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String rawToken = authHeader.substring(7).trim();
            if (!rawToken.isEmpty()) {
                try {
                    Jwt jwt = jwtService.decodeToken(rawToken);
                    String subject = jwt.getSubject();
                    AppUser user = resolveAppUser(subject);
                    if (user != null && Boolean.TRUE.equals(user.getIsActive())) {
                        String driverId = resolveDriverId(user.getId(), jwt.getClaimAsString("driverId"));
                        SecurityUser principal = new SecurityUser(
                                user.getId(),
                                user.getUsername(),
                                user.getFullName(),
                                user.getPhone(),
                                user.getRole(),
                                driverId
                        );
                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())
                        );
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principal, rawToken, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (JwtException ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private AppUser resolveAppUser(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        AppUser byId = appUserMapper.selectById(subject);
        if (byId != null) {
            return byId;
        }
        return appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getSupabaseUid, subject)
                .last("LIMIT 1"));
    }

    private String resolveDriverId(String userId, String fallbackClaim) {
        Driver drv = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getUserId, userId)
                .last("LIMIT 1"));
        return drv != null ? drv.getId() : fallbackClaim;
    }
}
