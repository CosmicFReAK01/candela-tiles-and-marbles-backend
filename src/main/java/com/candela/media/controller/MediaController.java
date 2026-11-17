package com.candela.media.controller;

import com.candela.common.dto.ApiResponse;
import com.candela.media.dto.MediaDtos.MediaDto;
import com.candela.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/media", "/api/media", "/api/v1/admin/media"})
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MediaDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText) throws IOException {

        MediaDto uploaded = mediaService.upload(file, altText);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("File uploaded successfully", uploaded));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<MediaDto>>> getAllMedia() {
        return ResponseEntity.ok(ApiResponse.ok(mediaService.getAllMedia()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable String id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok(ApiResponse.ok("Media deleted", null));
    }
}
