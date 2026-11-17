package com.candela.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class MediaDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaDto {
        private String id;
        private String filename;
        private String originalFilename;
        private String fileUrl;
        private String fileType;
        private String mimeType;
        private Long fileSizeBytes;
        private String altText;
        private Instant createdAt;
    }
}
