package com.candela.enquiry.controller;

import com.candela.common.dto.ApiResponse;
import com.candela.enquiry.dto.EnquiryDtos.*;
import com.candela.enquiry.service.EnquiryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/enquiries", "/api/enquiries"})
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping
    public ResponseEntity<java.util.Map<String, Object>> createEnquiry(
            @RequestBody EnquiryCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);
        EnquiryCreateResponse response = enquiryService.createEnquiry(request, clientIp, idempotencyKey);
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("success", true);
        map.put("message", "Enquiry submitted successfully.");
        map.put("referenceCode", response.getReferenceCode());
        map.put("trackingToken", response.getTrackingToken());
        map.put("data", response);
        map.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(map);
    }

    public record TrackVerificationRequest(String trackingToken, String email, String phone) {}

    @GetMapping("/track/{code}")
    public ResponseEntity<ApiResponse<EnquiryTrackDto>> trackEnquiry(
            @PathVariable String code,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestHeader(value = "X-Tracking-Token", required = false) String headerToken,
            @RequestHeader(value = "X-Customer-Email", required = false) String headerEmail,
            @RequestHeader(value = "X-Customer-Phone", required = false) String headerPhone) {

        String effectiveToken = headerToken != null && !headerToken.isBlank() ? headerToken : token;
        String effectiveEmail = headerEmail != null && !headerEmail.isBlank() ? headerEmail : email;
        String effectivePhone = headerPhone != null && !headerPhone.isBlank() ? headerPhone : phone;

        EnquiryTrackDto trackDto = enquiryService.trackEnquiry(code, effectiveToken, effectiveEmail, effectivePhone);
        return ResponseEntity.ok(ApiResponse.ok(trackDto));
    }

    @PostMapping("/track/{code}")
    public ResponseEntity<ApiResponse<EnquiryTrackDto>> trackEnquiryPost(
            @PathVariable String code,
            @RequestBody(required = false) TrackVerificationRequest body,
            @RequestHeader(value = "X-Tracking-Token", required = false) String headerToken,
            @RequestHeader(value = "X-Customer-Email", required = false) String headerEmail,
            @RequestHeader(value = "X-Customer-Phone", required = false) String headerPhone) {

        String token = body != null && body.trackingToken() != null ? body.trackingToken() : headerToken;
        String email = body != null && body.email() != null ? body.email() : headerEmail;
        String phone = body != null && body.phone() != null ? body.phone() : headerPhone;

        EnquiryTrackDto trackDto = enquiryService.trackEnquiry(code, token, email, phone);
        return ResponseEntity.ok(ApiResponse.ok(trackDto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SHOWROOM_ADVISOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<EnquiryAdminDto>>> getAllEnquiries() {
        return ResponseEntity.ok(ApiResponse.ok(enquiryService.getAllEnquiriesForAdmin()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SHOWROOM_ADVISOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EnquiryAdminDto>> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok(enquiryService.updateStatus(id, status)));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
