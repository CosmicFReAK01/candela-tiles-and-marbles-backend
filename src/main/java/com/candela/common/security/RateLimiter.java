package com.candela.common.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    public record RateLimitResult(boolean allowed, int remaining, long resetInSeconds) {}

    private static class Entry {
        int count;
        long resetAt;

        Entry(long resetAt) {
            this.count = 0;
            this.resetAt = resetAt;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public RateLimitResult check(String key, int maxAttempts, long windowMs) {
        long now = System.currentTimeMillis();
        Entry entry = store.compute(key, (k, existing) -> {
            if (existing == null || now > existing.resetAt) {
                return new Entry(now + windowMs);
            }
            return existing;
        });

        int remaining = Math.max(0, maxAttempts - entry.count);
        long resetInSeconds = Math.max(1, (entry.resetAt - now) / 1000);
        return new RateLimitResult(entry.count < maxAttempts, remaining, resetInSeconds);
    }

    public RateLimitResult increment(String key, int maxAttempts, long windowMs) {
        long now = System.currentTimeMillis();
        Entry entry = store.compute(key, (k, existing) -> {
            if (existing == null || now > existing.resetAt) {
                Entry e = new Entry(now + windowMs);
                e.count = 1;
                return e;
            } else {
                existing.count += 1;
                return existing;
            }
        });

        int remaining = Math.max(0, maxAttempts - entry.count);
        long resetInSeconds = Math.max(1, (entry.resetAt - now) / 1000);
        return new RateLimitResult(entry.count <= maxAttempts, remaining, resetInSeconds);
    }

    public void reset(String key) {
        store.remove(key);
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> now > entry.getValue().resetAt);
    }
}
