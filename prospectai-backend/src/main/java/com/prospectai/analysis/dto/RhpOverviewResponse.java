package com.prospectai.analysis.dto;

import com.prospectai.chat.dto.CitationDto;

import java.util.List;

public record RhpOverviewResponse(
        String documentId,
        String companyName,
        String industry,
        String businessModel,
        String objectsOfIssue,
        String promotersAndShareholding,
        List<CitationDto> citations,
        String disclaimer
) {
}
