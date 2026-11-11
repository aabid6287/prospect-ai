package com.prospectai.document.dto;

import com.prospectai.document.DocumentStatus;

public record UploadDocumentResponse(
    String id,
    String originalFilename,
    String companyName,
    DocumentStatus status,
    String message
) {}
