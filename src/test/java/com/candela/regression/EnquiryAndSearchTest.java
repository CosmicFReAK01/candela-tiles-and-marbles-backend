package com.candela.regression;

import com.candela.enquiry.dto.EnquiryDtos.EnquiryCreateRequest;
import com.candela.enquiry.repository.EnquiryRepository;
import com.candela.enquiry.service.EnquiryService;
import com.candela.product.model.Product;
import com.candela.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class EnquiryAndSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private EnquiryService enquiryService;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Public enquiry submission and PII-scrubbed token tracking")
    void enquirySubmissionAndTracking() throws Exception {
        EnquiryCreateRequest req = EnquiryCreateRequest.builder()
                .clientName("Rahul Sharma")
                .customerEmail("rahul@example.com")
                .customerPhone("+91 98765 43210")
                .productName("Italian Statuario Marble")
                .quantityArea("500 sq ft")
                .notes("Need sample delivery")
                .build();

        var created = enquiryService.createEnquiry(req, "127.0.0.1", null);
        assertThat(created.getReferenceCode()).isNotNull();
        assertThat(created.getTrackingToken()).isNotNull();

        // 1. Unverified tracking attempt without credentials returns 404 (identical to nonexistent code to prevent enumeration)
        mockMvc.perform(get("/api/v1/enquiries/track/" + created.getReferenceCode()))
                .andExpect(status().isNotFound());

        // 1b. Nonexistent reference code returns the exact same 404 response
        mockMvc.perform(get("/api/v1/enquiries/track/CND-NONEXISTENT-CODE"))
                .andExpect(status().isNotFound());

        // 2. Verified tracking with secret token via HTTP header returns status, NO PII
        mockMvc.perform(get("/api/v1/enquiries/track/" + created.getReferenceCode())
                        .header("X-Tracking-Token", created.getTrackingToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").value(created.getReferenceCode()))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                // Verify PII is NOT present in the returned JSON
                .andExpect(jsonPath("$.data.customerName").doesNotExist())
                .andExpect(jsonPath("$.data.customerEmail").doesNotExist())
                .andExpect(jsonPath("$.data.customerPhone").doesNotExist())
                .andExpect(jsonPath("$.data.message").doesNotExist());

        // 3. Verified tracking via POST body (out of query string) returns 200 OK
        mockMvc.perform(post("/api/v1/enquiries/track/" + created.getReferenceCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingToken\":\"" + created.getTrackingToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reference").value(created.getReferenceCode()));
    }

    @Test
    @DisplayName("Idempotency key prevents duplicate enquiry persistence")
    void idempotentEnquirySubmissionPreventsDuplicates() throws Exception {
        long countBefore = enquiryRepository.count();

        EnquiryCreateRequest req = EnquiryCreateRequest.builder()
                .clientName("Suresh Raina")
                .customerEmail("suresh@example.com")
                .productName("Botticino Classic")
                .build();

        String idempotencyKey = "idemp-key-" + System.currentTimeMillis();

        // First submission
        var firstResult = mockMvc.perform(post("/api/v1/enquiries")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String ref1 = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("referenceCode").asText();

        // Second submission with identical idempotency key
        var secondResult = mockMvc.perform(post("/api/v1/enquiries")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String ref2 = objectMapper.readTree(secondResult.getResponse().getContentAsString()).get("referenceCode").asText();

        // Responses must match, and database count must have increased by exactly 1
        assertThat(ref1).isEqualTo(ref2);
        long countAfter = enquiryRepository.count();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("Honeypot field stops bot enquiry without persisting")
    void honeypotRejectsBot() throws Exception {
        long countBefore = enquiryRepository.count();

        EnquiryCreateRequest botReq = EnquiryCreateRequest.builder()
                .clientName("Spam Bot")
                .customerEmail("bot@spam.com")
                .customerPhone("1234567890")
                .website("http://spam-link.com") // Honeypot filled
                .build();

        mockMvc.perform(post("/api/v1/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(botReq)))
                .andExpect(status().isCreated());

        long countAfter = enquiryRepository.count();
        // Database count must not have incremented
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("Product search returns matching products by query")
    void productSearch() throws Exception {
        productRepository.save(Product.builder()
                .id("test-search-tile-1")
                .sku("SKU-TEST-CALACATTA")
                .name("Calacatta Gold Luxury")
                .slug("calacatta-gold-luxury")
                .material("Marble")
                .finish("Polished")
                .status("published")
                .isFeatured(true)
                .version(1)
                .build());

        mockMvc.perform(get("/api/v1/search").param("q", "Calacatta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].name").value("Calacatta Gold Luxury"));
    }
}
