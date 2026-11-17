package com.candela.security;

import com.candela.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Protected route rejects request with no token (401)")
    void protectedRouteRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Protected route rejects forged X-User-Roles header with no token (401)")
    void rejectsForgedXUserRolesHeader() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sales")
                        .header("X-User-Roles", "SUPER_ADMIN,ADMIN")
                        .header("X-User-Id", "admin-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Protected route rejects tampered JWT signature (401)")
    void rejectsTamperedToken() throws Exception {
        String validToken = jwtTokenProvider.generateToken("user-1", "testuser", "test@candela.in", List.of("SUPER_ADMIN"));
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("ROLE_USER token cannot access admin routes (403)")
    void roleUserCannotAccessAdmin() throws Exception {
        String userToken = jwtTokenProvider.generateToken("user-2", "regularuser", "user@candela.in", List.of("USER"));

        mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Mutating cookie request from untrusted origin is rejected by CSRF filter (403)")
    void cookieAuthUntrustedOriginRejected() throws Exception {
        String adminToken = jwtTokenProvider.generateToken("admin-1", "admin", "admin@candela.in", List.of("ADMIN"));
        Cookie authCookie = new Cookie("candela_access_token", adminToken);

        var result = mockMvc.perform(post("/api/v1/enquiries")
                        .cookie(authCookie)
                        .header("Origin", "https://evil-attacker-site.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientName\":\"Test\",\"customerEmail\":\"test@test.com\",\"customerPhone\":\"1234567890\",\"notes\":\"Test enquiry\"}"))
                .andExpect(status().isForbidden())
                .andReturn();
        System.out.println("CSRF response content: " + result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Rate limiter enforces lockout after 5 consecutive failed login attempts (429)")
    void loginRateLimiterEnforcesLockout() throws Exception {
        String badPayload = "{\"usernameOrEmail\":\"rate-limit-victim\",\"password\":\"WrongPassword123!\"}";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "198.51.100.42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt must be blocked by rate limiter with 429 TOO_MANY_REQUESTS
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    @DisplayName("JwtTokenProvider fails securely on null, empty, or insecure (< 32 char) secrets")
    void jwtTokenProviderFailsSecurelyOnWeakSecrets() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtTokenProvider(null, 900000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret must not be empty");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtTokenProvider("", 900000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret must not be empty");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtTokenProvider("   ", 900000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret must not be empty");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtTokenProvider("short-secret-under-32", 900000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters long");

        // Valid secret with >= 32 characters succeeds
        String validStrongKey = EphemeralJwtKeyInitializer.getEphemeralSecret();
        JwtTokenProvider provider = new JwtTokenProvider(validStrongKey, 900000);
        org.assertj.core.api.Assertions.assertThat(provider).isNotNull();
    }
}
