package com.candela.enquiry.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 100)
    private String referenceNo;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "quantity_area", length = 100)
    private String quantityArea;

    @Column(name = "project_type", length = 100)
    private String projectType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 50)
    @Builder.Default
    private String status = "RECEIVED";

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tracking_token_hash", length = 64)
    private String trackingTokenHash;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = "RECEIVED";
        if (version == null) version = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
