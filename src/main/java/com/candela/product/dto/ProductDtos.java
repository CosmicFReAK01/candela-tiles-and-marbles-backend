package com.candela.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class ProductDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {
        private String id;
        private String sku;
        private String name;
        private String slug;
        private String category;
        private String masterCategory;
        private String subcategory;
        private String collectionName;
        private String collectionSlug;
        private String brand;
        private String material;
        private String finish;
        private String colour;
        private String shape;
        private String origin;
        private BigDecimal pricePerSqft;
        private BigDecimal salePrice;
        private BigDecimal stockSqftAvailable;
        private Boolean inStock;
        private Boolean isFeatured;
        private String mainImageUrl;
        private String status;
        private String shortDescription;
        private String description;
        private Integer version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionDto {
        private String id;
        private String name;
        private String slug;
        private String description;
        private String coverImageUrl;
        private Integer displayOrder;
        private Boolean isPublished;
        @JsonProperty("product_count")
        private Long productCount;
        private Integer version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private String id;
        private String name;
        private String slug;
        private String description;
        private String imageUrl;
        private Integer displayOrder;
        private Boolean isActive;
        private Integer version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResponse {
        private String query;
        private List<ProductDto> products;
        private List<CollectionDto> collections;
        private int count;
        @Builder.Default
        private String source = "Spring Boot Modular Monolith";
        private String timestamp;
    }
}
