package com.prospectai.document.dto;

import com.prospectai.document.DocumentStatus;
import com.prospectai.document.UploadedDocument;

import java.time.Instant;

public record DocumentResponse(
    String id,
    String originalFilename,
    String companyName,
    DocumentStatus status,
    Integer pageCount,
    Long fileSize,
    String contentType,
    Instant createdAt,
    Instant updatedAt,
    String failureReason
) {
    public static DocumentResponse fromEntity(UploadedDocument doc) {
        return new DocumentResponse(
            doc.getId(),
            doc.getOriginalFilename(),
            doc.getCompanyName(),
            doc.getStatus(),
            doc.getPageCount(),
            doc.getFileSize(),
            doc.getContentType(),
            doc.getCreatedAt(),
            doc.getUpdatedAt(),
            doc.getFailureReason()
        );
    }
}
