package com.candela.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ContentDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionDto {
        private String sectionKey;
        private String title;
        private String subtitle;
        private String description;
        private String imageUrl;
        private String ctaText;
        private String ctaUrl;
        private Integer version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerDto {
        private String id;
        private String title;
        private String subtitle;
        private String imageUrl;
        private String ctaText;
        private String ctaUrl;
        private Integer displayOrder;
        private Boolean isActive;
    }
}
