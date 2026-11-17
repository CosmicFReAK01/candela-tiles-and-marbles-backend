package com.candela.auth.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private UUID id;
    private String name;
    private String description;
    @Builder.Default
    private Instant createdAt = Instant.now();
}
