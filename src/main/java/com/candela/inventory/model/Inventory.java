package com.candela.inventory.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @Column(name = "current_stock")
    @Builder.Default
    private Integer currentStock = 0;

    @Column(name = "reserved_stock")
    @Builder.Default
    private Integer reservedStock = 0;

    @Column(name = "minimum_stock_level")
    @Builder.Default
    private Integer minimumStockLevel = 10;

    @Column(name = "stock_unit", length = 50)
    @Builder.Default
    private String stockUnit = "sqft";

    @Column(name = "stock_status", length = 50)
    @Builder.Default
    private String stockStatus = "IN_STOCK";

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (currentStock == null) currentStock = 0;
        if (reservedStock == null) reservedStock = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
