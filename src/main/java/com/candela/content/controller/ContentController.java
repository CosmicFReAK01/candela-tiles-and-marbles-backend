package com.candela.content.controller;

import com.candela.common.dto.ApiResponse;
import com.candela.content.dto.ContentDtos.*;
import com.candela.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/content", "/api/content", "/api/v1/admin/cms"})
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/homepage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomepage() {
        return ResponseEntity.ok(ApiResponse.ok(contentService.getHomepageBlocks()));
    }

    @PutMapping("/homepage/{sectionKey}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SectionDto>> updateSection(
            @PathVariable String sectionKey,
            @RequestBody SectionDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Section updated", contentService.updateSection(sectionKey, dto)));
    }

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<BannerDto>>> getBanners() {
        return ResponseEntity.ok(ApiResponse.ok(contentService.getActiveBanners()));
    }
}
