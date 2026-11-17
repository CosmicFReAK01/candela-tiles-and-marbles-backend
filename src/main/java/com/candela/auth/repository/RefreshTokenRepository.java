package com.candela.auth.repository;

import com.candela.auth.model.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RefreshTokenRepository {
    private final Map<UUID, RefreshToken> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byToken = new ConcurrentHashMap<>();

    public Optional<RefreshToken> findByToken(String token) {
        if (token == null) return Optional.empty();
        UUID id = byToken.get(token);
        return id != null ? Optional.ofNullable(byId.get(id)) : Optional.empty();
    }

    public RefreshToken save(RefreshToken entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        byId.put(entity.getId(), entity);
        if (entity.getToken() != null) {
            byToken.put(entity.getToken(), entity.getId());
        }
        return entity;
    }

    public void deleteByToken(String token) {
        if (token != null) {
            UUID id = byToken.remove(token);
            if (id != null) {
                byId.remove(id);
            }
        }
    }
}
