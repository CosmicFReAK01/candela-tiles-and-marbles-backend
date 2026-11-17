package com.candela.auth.model;

import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "roles")
@ToString(exclude = "roles")
public class User {
    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    @Builder.Default
    private boolean active = true;
    private Instant lastLogin;
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    @Builder.Default
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
}
