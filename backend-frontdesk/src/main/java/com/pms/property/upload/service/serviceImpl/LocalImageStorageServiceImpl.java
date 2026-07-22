package com.pms.property.upload.service.serviceImpl;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.upload.dto.UploadImageResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.pms.property.upload.service.LocalImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalImageStorageServiceImpl implements LocalImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final Path uploadRoot;
    private final long maxUploadBytes;

    @Autowired
    public LocalImageStorageServiceImpl(
        @Value("${app.upload.base-dir:uploads}") String uploadBaseDir,
        @Value("${app.upload.max-bytes:5242880}") long maxUploadBytes
    ) {
        this.uploadRoot = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        this.maxUploadBytes = maxUploadBytes;
    }

    public LocalImageStorageServiceImpl(String uploadBaseDir, long maxUploadBytes, boolean ignored) {
        this.uploadRoot = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    public UploadImageResponse store(MultipartFile file) {
        validate(file);

        String extension = resolveExtension(file);
        String storedFileName = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new UploadImageResponse(storedFileName, "/uploads/" + storedFileName, file.getSize(), file.getContentType());
        } catch (IOException ex) {
            throw new BadRequestException("Could not store uploaded image");
        }
    }

    @Override
    public void deleteByPublicUrl(String publicUrlOrFileName) {
        String fileName = extractFileName(publicUrlOrFileName);
        if (fileName.isBlank()) {
            return;
        }

        Path target = uploadRoot.resolve(fileName).normalize();
        if (!target.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Best-effort cleanup should not block business operations.
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (file.getSize() > maxUploadBytes) {
            throw new BadRequestException("Image size exceeds configured upload limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ENGLISH).startsWith("image/")) {
            throw new BadRequestException("Only image uploads are allowed");
        }

        String extension = resolveExtension(file);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported image type. Allowed: jpg, jpeg, png, webp, gif");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
                return originalName.substring(dotIndex).toLowerCase(Locale.ENGLISH);
            }
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return "";
        }
        return switch (contentType.toLowerCase(Locale.ENGLISH)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }

    private String extractFileName(String publicUrlOrFileName) {
        if (publicUrlOrFileName == null) {
            return "";
        }
        String value = publicUrlOrFileName.trim();
        if (value.isEmpty()) {
            return "";
        }
        int slash = value.lastIndexOf('/');
        String fileName = slash >= 0 ? value.substring(slash + 1) : value;
        if (fileName.contains("..") || fileName.contains("\\") || fileName.contains("/")) {
            return "";
        }
        return fileName;
    }
}


