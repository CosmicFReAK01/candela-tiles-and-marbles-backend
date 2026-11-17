package com.candela.enquiry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class EnquiryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EnquiryCreateRequest {
        @JsonAlias({"clientName", "customerName", "name"})
        private String clientName;

        @JsonAlias({"phoneWhatsApp", "customerPhone", "phone"})
        private String customerPhone;

        @JsonAlias({"email", "customerEmail"})
        private String customerEmail;

        private String productName;
        private String quantityArea;

        @JsonAlias({"firmOrProjectName", "projectType"})
        private String projectType;

        @JsonAlias({"notes", "message"})
        private String notes;

        // Bot Honeypot fields
        private String website;
        private String companyUrl;

        // Idempotency key
        private String idempotencyKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnquiryCreateResponse {
        private String referenceCode;
        private String trackingToken;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EnquiryTrackDto {
        private String reference;
        private String status;
        private Instant statusUpdatedAt;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnquiryAdminDto {
        private String id;
        private String referenceCode;
        private String clientName;
        private String customerPhone;
        private String customerEmail;
        private String productName;
        private String projectType;
        private String message;
        private String status;
        private String notes;
        private Integer version;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
