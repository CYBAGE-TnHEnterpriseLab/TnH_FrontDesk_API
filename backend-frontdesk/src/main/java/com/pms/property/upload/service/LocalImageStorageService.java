package com.pms.property.upload.service;

import com.pms.property.upload.dto.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface LocalImageStorageService {

    UploadImageResponse store(MultipartFile file);

    void deleteByPublicUrl(String publicUrlOrFileName);
}
