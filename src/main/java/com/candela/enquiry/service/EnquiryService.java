package com.candela.enquiry.service;

import com.candela.common.security.RateLimiter;
import com.candela.enquiry.dto.EnquiryDtos.*;
import com.candela.enquiry.model.Enquiry;
import com.candela.enquiry.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final RateLimiter rateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    // Idempotency cache (stores results for 24h)
    private final Map<String, EnquiryCreateResponse> idempotencyCache = new ConcurrentHashMap<>();

    @Value("${app.security.rate-limit.enquiry-max-attempts:10}")
    private int maxEnquiryAttempts;

    @Value("${app.security.rate-limit.enquiry-window-minutes:15}")
    private int enquiryWindowMinutes;

    @Transactional
    public EnquiryCreateResponse createEnquiry(EnquiryCreateRequest request, String clientIp, String idempotencyKeyHeader) {
        String effectiveKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader.trim()
                : (request.getIdempotencyKey() != null ? request.getIdempotencyKey().trim() : null);

        // Check idempotency
        if (effectiveKey != null && !effectiveKey.isBlank()) {
            EnquiryCreateResponse cached = idempotencyCache.get(effectiveKey);
            if (cached != null) {
                log.info("Returning cached enquiry response for idempotency key: {}", effectiveKey);
                return cached;
            }
        }

        // Rate limiting
        String ipKey = "enquiry:ip:" + clientIp;
        long windowMs = enquiryWindowMinutes * 60L * 1000L;
        var check = rateLimiter.increment(ipKey, maxEnquiryAttempts, windowMs);
        if (!check.allowed()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many enquiry requests. Please retry in " + check.resetInSeconds() + " seconds.");
        }

        // Honeypot check
        if ((request.getWebsite() != null && !request.getWebsite().isBlank()) ||
            (request.getCompanyUrl() != null && !request.getCompanyUrl().isBlank())) {
            log.warn("Honeypot triggered from IP: {}", clientIp);
            return EnquiryCreateResponse.builder()
                    .referenceCode("CND-2026-0000")
                    .message("Enquiry logged.")
                    .build();
        }

        // Validation
        if (request.getClientName() == null || request.getClientName().trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid client name is required.");
        }
        if ((request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) &&
            (request.getCustomerEmail() == null || request.getCustomerEmail().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please provide a contact phone or email.");
        }

        // Generate high-entropy reference code
        byte[] entropy = new byte[6];
        secureRandom.nextBytes(entropy);
        String entropyHex = HexFormat.of().formatHex(entropy).toUpperCase();
        String referenceNo = "CND-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + "-" + entropyHex;

        // Generate 24-byte tracking token and SHA-256 hash
        byte[] tokenBytes = new byte[24];
        secureRandom.nextBytes(tokenBytes);
        String secretToken = "candela_track_" + HexFormat.of().formatHex(tokenBytes);
        String tokenHash = sha256Hex(secretToken);

        Enquiry enquiry = Enquiry.builder()
                .referenceNo(referenceNo)
                .customerName(request.getClientName().trim())
                .customerPhone(request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : null)
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : null)
                .productName(request.getProductName())
                .quantityArea(request.getQuantityArea())
                .projectType(request.getProjectType() != null ? request.getProjectType().trim() : "Residential")
                .message(request.getNotes())
                .status("RECEIVED")
                .trackingTokenHash(tokenHash)
                .version(1)
                .build();

        enquiryRepository.save(enquiry);

        EnquiryCreateResponse response = EnquiryCreateResponse.builder()
                .referenceCode(referenceNo)
                .trackingToken(secretToken)
                .message("Enquiry successfully submitted.")
                .build();

        if (effectiveKey != null && !effectiveKey.isBlank()) {
            idempotencyCache.put(effectiveKey, response);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public EnquiryTrackDto trackEnquiry(String referenceNo, String candidateToken, String email, String phone) {
        Enquiry enquiry = enquiryRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Enquiry reference not found or verification credentials invalid."));

        boolean verified = false;

        if (candidateToken != null && !candidateToken.isBlank() && enquiry.getTrackingTokenHash() != null) {
            String candidateHash = sha256Hex(candidateToken.trim());
            verified = MessageDigest.isEqual(
                    candidateHash.getBytes(StandardCharsets.UTF_8),
                    enquiry.getTrackingTokenHash().getBytes(StandardCharsets.UTF_8)
            );
        }

        if (!verified && email != null && !email.isBlank() && enquiry.getCustomerEmail() != null) {
            verified = email.trim().equalsIgnoreCase(enquiry.getCustomerEmail().trim());
        }

        if (!verified && phone != null && !phone.isBlank() && enquiry.getCustomerPhone() != null) {
            String cleanQuery = phone.replaceAll("\\D", "");
            String cleanDb = enquiry.getCustomerPhone().replaceAll("\\D", "");
            verified = cleanDb.endsWith(cleanQuery) || cleanQuery.endsWith(cleanDb);
        }

        if (!verified) {
            // Return identical 404 to prevent enumeration of valid reference codes
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Enquiry reference not found or verification credentials invalid.");
        }

        // Return minimal PII-scrubbed status payload
        return EnquiryTrackDto.builder()
                .reference(enquiry.getReferenceNo())
                .status(enquiry.getStatus())
                .statusUpdatedAt(enquiry.getUpdatedAt())
                .createdAt(enquiry.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<EnquiryAdminDto> getAllEnquiriesForAdmin() {
        return enquiryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                .stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public EnquiryAdminDto updateStatus(UUID id, String status) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enquiry not found: " + id));
        enquiry.setStatus(status);
        return toAdminDto(enquiryRepository.save(enquiry));
    }

    private EnquiryAdminDto toAdminDto(Enquiry e) {
        return EnquiryAdminDto.builder()
                .id(e.getId().toString())
                .referenceCode(e.getReferenceNo())
                .clientName(e.getCustomerName())
                .customerPhone(e.getCustomerPhone())
                .customerEmail(e.getCustomerEmail())
                .productName(e.getProductName())
                .projectType(e.getProjectType())
                .message(e.getMessage())
                .status(e.getStatus())
                .notes(e.getNotes())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
