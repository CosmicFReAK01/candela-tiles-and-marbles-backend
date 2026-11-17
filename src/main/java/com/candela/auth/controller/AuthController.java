package com.candela.auth.controller;

import com.candela.auth.dto.AuthDtos.*;
import com.candela.auth.service.AuthService;
import com.candela.common.dto.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.login(request, clientIp);

        // Set access token cookie matching Next.js specifications
        Cookie cookie = new Cookie("candela_access_token", response.getAccessToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(httpRequest.isSecure() || "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto")));
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 24 hours
        httpResponse.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthResponse response = authService.refreshToken(request.getRefreshToken());

        Cookie cookie = new Cookie("candela_access_token", response.getAccessToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(httpRequest.isSecure() || "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto")));
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        httpResponse.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) TokenRefreshRequest request,
            HttpServletResponse httpResponse) {

        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }

        // Expire cookie
        Cookie cookie = new Cookie("candela_access_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        httpResponse.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(@AuthenticationPrincipal String username) {
        UserDto user = authService.getCurrentUser(username);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
