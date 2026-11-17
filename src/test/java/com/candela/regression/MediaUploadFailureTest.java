package com.candela.regression;

import com.candela.media.repository.MediaRepository;
import com.candela.media.service.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MediaUploadFailureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaService mediaService;

    @Test
    @DisplayName("Disallowed SVG upload is rejected and creates zero database records")
    @WithMockUser(roles = "ADMIN")
    void svgUploadRejectedNoDbRecord() throws Exception {
        long countBefore = mediaRepository.count();

        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "malicious.svg",
                "image/svg+xml",
                "<svg onload='alert(1)'></svg>".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/media/upload").file(svgFile))
                .andExpect(status().isUnsupportedMediaType());

        long countAfter = mediaRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("Interrupted / invalid magic bytes upload cleans up and creates zero orphan records")
    void invalidMagicBytesRejectsCleanly() {
        long countBefore = mediaRepository.count();

        // Spoofed mime-type claiming image/jpeg but header bytes are plain text/executable
        MockMultipartFile fakeJpg = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                "MZ-executable-bytes-not-a-real-jpeg".getBytes()
        );

        assertThatThrownBy(() -> mediaService.upload(fakeJpg, "Test Alt"))
                .isInstanceOf(ResponseStatusException.class);

        long countAfter = mediaRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }
}
