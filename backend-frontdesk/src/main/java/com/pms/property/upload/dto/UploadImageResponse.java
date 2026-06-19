package com.pms.property.upload.dto;

public record UploadImageResponse(
    String fileName,
    String url,
    long size,
    String contentType
) {
}

