package com.candela.auth.service;

import com.candela.auth.dto.AuthDtos.*;
import com.candela.auth.model.RefreshToken;
import com.candela.auth.model.Role;
import com.candela.auth.model.User;
import com.candela.auth.repository.RefreshTokenRepository;
import com.candela.auth.repository.RoleRepository;
import com.candela.auth.repository.UserRepository;
import com.candela.common.security.JwtTokenProvider;
import com.candela.common.security.RateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimiter rateLimiter;

    @Value("${app.security.rate-limit.login-max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.rate-limit.login-window-minutes:15}")
    private int loginWindowMinutes;

    @Value("${app.security.jwt.expiration-ms:900000}")
    private long jwtExpirationMs;

    @Value("${app.security.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_EMAIL:admin@candela.in}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @PostConstruct
    @Transactional
    public void init() {
        // Ensure default roles exist
        List<String> defaultRoles = List.of("SUPER_ADMIN", "ADMIN", "INVENTORY_MANAGER", "SHOWROOM_ADVISOR", "USER");
        for (String roleName : defaultRoles) {
            roleRepository.findByName(roleName).orElseGet(() ->
                    roleRepository.save(Role.builder()
                            .name(roleName)
                            .description("Default system role " + roleName)
                            .build())
            );
        }

        // Bootstrap admin if table is empty
        if (userRepository.count() == 0 && adminPassword != null && !adminPassword.isBlank()) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN").orElseThrow();

            User bootstrapAdmin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .firstName("Candela")
                    .lastName("Administrator")
                    .active(true)
                    .roles(Set.of(adminRole, superAdminRole))
                    .build();

            userRepository.save(bootstrapAdmin);
            log.info("Initialized bootstrap administrator account: {}", adminUsername);
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        String identifier = request.getUsernameOrEmail().trim().toLowerCase();
        String ipKey = "login:ip:" + clientIp;
        String userKey = "login:user:" + identifier;
        long windowMs = loginWindowMinutes * 60L * 1000L;

        // 1. Check Rate Limit
        var ipCheck = rateLimiter.check(ipKey, maxLoginAttempts, windowMs);
        var userCheck = rateLimiter.check(userKey, maxLoginAttempts, windowMs);

        if (!ipCheck.allowed() || !userCheck.allowed()) {
            long retryAfter = Math.max(ipCheck.resetInSeconds(), userCheck.resetInSeconds());
            log.warn("Login brute-force rate limit exceeded for IP: {} / Account: {}", clientIp, identifier);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please retry in " + retryAfter + " seconds.");
        }

        // 2. Lookup user
        User user = userRepository.findByUsernameOrEmail(identifier, identifier).orElse(null);

        // 3. Verify password
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimiter.increment(ipKey, maxLoginAttempts, windowMs);
            rateLimiter.increment(userKey, maxLoginAttempts, windowMs);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled.");
        }

        // 4. Successful login: reset rate limit counters
        rateLimiter.reset(ipKey);
        rateLimiter.reset(userKey);

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtTokenProvider.generateToken(user.getId().toString(), user.getUsername(), user.getEmail(), roleNames);
        String refreshToken = UUID.randomUUID().toString().replace("-", "");

        RefreshToken tokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(tokenEntity);

        UserDto userDto = UserDto.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .user(userDto)
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token."));

        if (token.isRevoked() || token.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked.");
        }

        User user = token.getUser();
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String newAccessToken = jwtTokenProvider.generateToken(user.getId().toString(), user.getUsername(), user.getEmail(), roleNames);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .user(UserDto.builder()
                        .id(user.getId().toString())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .roles(roleNames)
                        .build())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            });
        }
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserDto.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }
}
