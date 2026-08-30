package com.pms.property.upload.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.upload.dto.UploadImageResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreImageAndReturnPublicUrl() throws Exception {
        LocalImageStorageService service = new LocalImageStorageService(tempDir.toString(), 1024 * 1024, true);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "hero.png",
            "image/png",
            new byte[] {1, 2, 3, 4, 5}
        );

        UploadImageResponse response = service.store(file);

        assertTrue(response.url().startsWith("/uploads/"));
        assertTrue(Files.exists(tempDir.resolve(response.fileName())));
    }

    @Test
    void shouldRejectNonImageUpload() {
        LocalImageStorageService service = new LocalImageStorageService(tempDir.toString(), 1024 * 1024, true);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "notes.txt",
            "text/plain",
            "hello".getBytes()
        );

        assertThrows(BadRequestException.class, () -> service.store(file));
    }
}

