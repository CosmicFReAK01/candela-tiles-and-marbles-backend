package com.candela.media.service;

import com.candela.media.dto.MediaDtos.MediaDto;
import com.candela.media.model.Media;
import com.candela.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final RestTemplate restTemplate;

    @Value("${app.storage.type:supabase}")
    private String storageType;

    @Value("${app.storage.supabase.url:}")
    private String supabaseUrl;

    @Value("${app.storage.supabase.service-key:}")
    private String supabaseServiceKey;

    @Value("${app.storage.supabase.bucket:candela-media}")
    private String supabaseBucket;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @Transactional
    public MediaDto upload(MultipartFile file, String altText) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds maximum size of 10MB.");
        }

        byte[] bytes = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "application/octet-stream";

        // Validate raster format & reject SVG / XML payloads
        validateRasterImage(bytes, contentType);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
        String extension = getExtension(originalFilename);
        String uniqueFilename = "media_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        String uploadedUrl = null;

        if ("supabase".equalsIgnoreCase(storageType) && !supabaseUrl.isBlank() && !supabaseServiceKey.isBlank()) {
            uploadedUrl = uploadToSupabase(uniqueFilename, bytes, contentType);
        } else {
            uploadedUrl = "/uploads/" + uniqueFilename;
        }

        try {
            Media media = Media.builder()
                    .id("med-" + UUID.randomUUID().toString().substring(0, 12))
                    .filename(uniqueFilename)
                    .originalFilename(originalFilename)
                    .fileUrl(uploadedUrl)
                    .fileType("IMAGE")
                    .mimeType(contentType)
                    .fileSizeBytes(file.getSize())
                    .altText(altText != null ? altText : originalFilename)
                    .build();

            return toDto(mediaRepository.save(media));
        } catch (Exception e) {
            // Compensatory Rollback: delete orphaned storage object if database insert fails
            if ("supabase".equalsIgnoreCase(storageType) && !supabaseUrl.isBlank() && !supabaseServiceKey.isBlank()) {
                deleteFromSupabase(uniqueFilename);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to record media in database", e);
        }
    }

    @Transactional(readOnly = true)
    public List<MediaDto> getAllMedia() {
        return mediaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public void deleteMedia(String id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found: " + id));

        if ("supabase".equalsIgnoreCase(storageType) && !supabaseUrl.isBlank() && !supabaseServiceKey.isBlank()) {
            deleteFromSupabase(media.getFilename());
        }

        mediaRepository.delete(media);
    }

    private void validateRasterImage(byte[] bytes, String contentType) {
        if (bytes.length < 4) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid image data.");
        }

        // SVG / XML / HTML detection
        int checkLen = Math.min(bytes.length, 512);
        String header = new String(bytes, 0, checkLen, StandardCharsets.UTF_8).toLowerCase();
        if (header.contains("<svg") || header.contains("<?xml") || header.contains("<script") || header.contains("<!doctype")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file format. SVG uploads are disallowed; only JPEG, PNG, WebP, AVIF, and GIF raster images are permitted.");
        }

        boolean validMagicBytes = false;
        // JPEG: FF D8 FF
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            validMagicBytes = true;
        }
        // PNG: 89 50 4E 47
        else if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            validMagicBytes = true;
        }
        // GIF: GIF8
        else if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            validMagicBytes = true;
        }
        // WebP: RIFF....WEBP
        else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                 bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            validMagicBytes = true;
        }

        if (!validMagicBytes) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file format. Only validated raster images (JPEG, PNG, WebP, GIF) are allowed.");
        }
    }

    private String uploadToSupabase(String filename, byte[] bytes, String contentType) {
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + filename;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> entity = new HttpEntity<>(bytes, headers);
        restTemplate.postForEntity(uploadUrl, entity, String.class);

        return supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + filename;
    }

    private void deleteFromSupabase(String filename) {
        try {
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + filename;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseServiceKey);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        } catch (Exception e) {
            log.warn("Failed to delete storage object from Supabase: {}", e.getMessage());
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : ".jpg";
    }

    private MediaDto toDto(Media m) {
        return MediaDto.builder()
                .id(m.getId())
                .filename(m.getFilename())
                .originalFilename(m.getOriginalFilename())
                .fileUrl(m.getFileUrl())
                .fileType(m.getFileType())
                .mimeType(m.getMimeType())
                .fileSizeBytes(m.getFileSizeBytes())
                .altText(m.getAltText())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
