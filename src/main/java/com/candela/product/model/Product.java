package com.candela.product.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @Column(nullable = false, length = 100)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "master_category")
    private String masterCategory;

    @Column(name = "subcategory")
    private String subcategory;

    @Column(name = "category")
    private String category;

    @Column(name = "collection_name")
    private String collectionName;

    @Column(name = "collection_slug")
    private String collectionSlug;

    @Column(name = "brand")
    private String brand;

    @Column(name = "material")
    private String material;

    @Column(name = "finish")
    private String finish;

    @Column(name = "colour")
    private String colour;

    @Column(name = "shape")
    private String shape;

    @Column(name = "origin")
    private String origin;

    @Column(name = "price_per_sqft", precision = 10, scale = 2)
    private BigDecimal pricePerSqft;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "stock_sqft_available", precision = 10, scale = 2)
    private BigDecimal stockSqftAvailable;

    @Column(name = "in_stock")
    @Builder.Default
    private Boolean inStock = true;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "main_image_url")
    private String mainImageUrl;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "published";

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
        if (status == null) status = "published";
        if (inStock == null) inStock = true;
        if (isFeatured == null) isFeatured = false;
        if (version == null) version = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
