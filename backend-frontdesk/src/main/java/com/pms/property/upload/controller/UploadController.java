package com.pms.property.upload.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.upload.dto.UploadImageResponse;
import com.pms.property.upload.service.LocalImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final LocalImageStorageService localImageStorageService;

    public UploadController(LocalImageStorageService localImageStorageService) {
        this.localImageStorageService = localImageStorageService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadImageResponse>> uploadImage(@RequestParam("file") MultipartFile file) {
        UploadImageResponse uploaded = localImageStorageService.store(file);
        return ResponseEntity.ok(ApiResponse.ok(uploaded, "Image uploaded"));
    }
}

