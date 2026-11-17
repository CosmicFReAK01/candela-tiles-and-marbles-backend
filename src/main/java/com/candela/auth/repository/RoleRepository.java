package com.candela.auth.repository;

import com.candela.auth.model.Role;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RoleRepository {
    private final Map<UUID, Role> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();

    public Optional<Role> findByName(String name) {
        if (name == null) return Optional.empty();
        UUID id = byName.get(name.toUpperCase());
        return id != null ? Optional.ofNullable(byId.get(id)) : Optional.empty();
    }

    public Role save(Role role) {
        if (role.getId() == null) {
            role.setId(UUID.randomUUID());
        }
        byId.put(role.getId(), role);
        if (role.getName() != null) {
            byName.put(role.getName().toUpperCase(), role.getId());
        }
        return role;
    }
}
