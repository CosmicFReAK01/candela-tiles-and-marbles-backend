package com.candela.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:900000}") long expirationMs) {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException("CRITICAL: app.security.jwt.secret must not be empty and must be at least 32 characters long for secure HMAC-SHA signing");
        }
        this.key = parseSecretKey(secret.trim());
        this.expirationMs = expirationMs;
    }

    private SecretKey parseSecretKey(String secret) {
        try {
            if (secret.length() % 4 == 0 && secret.matches("^[A-Za-z0-9+/=]+$")) {
                byte[] decoded = Base64.getDecoder().decode(secret);
                if (decoded.length >= 32) {
                    return Keys.hmacShaKeyFor(decoded);
                }
            }
        } catch (Exception ignored) {}
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("CRITICAL: JWT secret key must provide at least 256 bits of entropy (32 bytes)");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String userId, String username, String email, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object rolesObj = getClaims(token).get("roles");
        if (rolesObj instanceof List<?> list) {
            return (List<String>) list;
        } else if (rolesObj instanceof String str) {
            return List.of(str.split(","));
        }
        return Collections.emptyList();
    }

    public String getUsername(String token) {
        Object val = getClaims(token).get("username");
        return val != null ? val.toString() : null;
    }
}
