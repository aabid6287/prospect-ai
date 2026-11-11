package com.prospectai.analysis.dto;

import com.prospectai.chat.dto.CitationDto;

import java.util.List;

public record FinancialAnalysisResponse(
        String documentId,
        String companyName,
        String revenueTrend,
        String profitabilityAndEbitda,
        String capitalStructureAndDebt,
        List<FinancialMetricDto> keyMetrics,
        List<CitationDto> citations,
        String disclaimer
) {
}
