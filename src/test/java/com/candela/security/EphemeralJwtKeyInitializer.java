package com.candela.security;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Test application context initializer that dynamically generates a high-entropy ephemeral
 * HMAC-SHA signing key (512 bits) at test runtime.
 *
 * Guarantees:
 * 1. Zero static secrets stored in source control or test configuration files.
 * 2. Key entropy satisfies production-grade HMAC-SHA requirements (64 random bytes / 512 bits).
 * 3. Consistent signing key across all test contexts within a JVM run.
 * 4. Injected into Spring's Environment at the highest precedence before any beans or @Value resolve.
 */
public class EphemeralJwtKeyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String EPHEMERAL_TEST_SECRET;

    static {
        byte[] randomBytes = new byte[64]; // 512-bit key
        new SecureRandom().nextBytes(randomBytes);
        EPHEMERAL_TEST_SECRET = Base64.getEncoder().encodeToString(randomBytes);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, Object> ephemeralProperties = Map.of(
                "app.security.jwt.secret", EPHEMERAL_TEST_SECRET,
                "JWT_SECRET", EPHEMERAL_TEST_SECRET
        );
        applicationContext.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("ephemeralJwtProperties", ephemeralProperties)
        );
    }

    public static String getEphemeralSecret() {
        return EPHEMERAL_TEST_SECRET;
    }
}
