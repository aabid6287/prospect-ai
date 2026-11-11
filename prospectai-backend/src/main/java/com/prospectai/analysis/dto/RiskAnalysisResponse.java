package com.prospectai.analysis.dto;

import com.prospectai.chat.dto.CitationDto;

import java.util.List;

public record RiskAnalysisResponse(
        String documentId,
        String companyName,
        String internalRisksSummary,
        List<String> keyInternalRisks,
        String externalRisksSummary,
        List<String> keyExternalRisks,
        String litigationSummary,
        List<CitationDto> citations,
        String disclaimer
) {
}
