package com.candela.auth.repository;

import com.candela.auth.model.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {
    private final Map<UUID, User> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byUsername = new ConcurrentHashMap<>();
    private final Map<String, UUID> byEmail = new ConcurrentHashMap<>();

    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        UUID id = byUsername.get(username.toLowerCase());
        return id != null ? Optional.ofNullable(byId.get(id)) : Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        UUID id = byEmail.get(email.toLowerCase());
        return id != null ? Optional.ofNullable(byId.get(id)) : Optional.empty();
    }

    public Optional<User> findByUsernameOrEmail(String username, String email) {
        Optional<User> u = findByUsername(username);
        return u.isPresent() ? u : findByEmail(email);
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        byId.put(user.getId(), user);
        if (user.getUsername() != null) byUsername.put(user.getUsername().toLowerCase(), user.getId());
        if (user.getEmail() != null) byEmail.put(user.getEmail().toLowerCase(), user.getId());
        return user;
    }

    public long count() {
        return byId.size();
    }
}
