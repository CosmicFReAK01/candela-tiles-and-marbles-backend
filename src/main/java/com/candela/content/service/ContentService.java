package com.candela.content.service;

import com.candela.content.dto.ContentDtos.*;
import com.candela.content.model.HomepageContent;
import com.candela.content.repository.BannerRepository;
import com.candela.content.repository.HomepageContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final HomepageContentRepository homepageContentRepository;
    private final BannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getHomepageBlocks() {
        Map<String, Object> result = new HashMap<>();
        List<HomepageContent> list = homepageContentRepository.findAll();
        for (HomepageContent hc : list) {
            result.put(hc.getSectionKey(), toSectionDto(hc));
        }
        return result;
    }

    @Transactional
    public SectionDto updateSection(String sectionKey, SectionDto dto) {
        HomepageContent existing = homepageContentRepository.findBySectionKey(sectionKey)
                .orElseGet(() -> HomepageContent.builder().sectionKey(sectionKey).build());

        if (dto.getVersion() != null && existing.getVersion() != null && !dto.getVersion().equals(existing.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflict: Version mismatch. Expected " + existing.getVersion() + ", got " + dto.getVersion());
        }

        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getSubtitle() != null) existing.setSubtitle(dto.getSubtitle());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) existing.setImageUrl(dto.getImageUrl());
        if (dto.getCtaText() != null) existing.setCtaText(dto.getCtaText());
        if (dto.getCtaUrl() != null) existing.setCtaUrl(dto.getCtaUrl());

        return toSectionDto(homepageContentRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public List<BannerDto> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(b -> BannerDto.builder()
                        .id(b.getId().toString())
                        .title(b.getTitle())
                        .subtitle(b.getSubtitle())
                        .imageUrl(b.getImageUrl())
                        .ctaText(b.getCtaText())
                        .ctaUrl(b.getCtaUrl())
                        .displayOrder(b.getDisplayOrder())
                        .isActive(b.getIsActive())
                        .build()).toList();
    }

    private SectionDto toSectionDto(HomepageContent hc) {
        return SectionDto.builder()
                .sectionKey(hc.getSectionKey())
                .title(hc.getTitle())
                .subtitle(hc.getSubtitle())
                .description(hc.getDescription())
                .imageUrl(hc.getImageUrl())
                .ctaText(hc.getCtaText())
                .ctaUrl(hc.getCtaUrl())
                .version(hc.getVersion())
                .build();
    }
}
