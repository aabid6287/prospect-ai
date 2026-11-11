package com.prospectai.analysis;

import com.prospectai.analysis.dto.FinancialAnalysisResponse;
import com.prospectai.analysis.dto.RhpOverviewResponse;
import com.prospectai.analysis.dto.RiskAnalysisResponse;
import com.prospectai.common.ApiResponse;
import com.prospectai.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final RhpAnalysisService analysisService;

    public AnalysisController(RhpAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/{documentId}/overview")
    public ResponseEntity<ApiResponse<RhpOverviewResponse>> getOverview(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId
    ) {
        RhpOverviewResponse response = analysisService.getOverview(user, documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{documentId}/risks")
    public ResponseEntity<ApiResponse<RiskAnalysisResponse>> getRisks(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId
    ) {
        RiskAnalysisResponse response = analysisService.getRisks(user, documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{documentId}/financials")
    public ResponseEntity<ApiResponse<FinancialAnalysisResponse>> getFinancials(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId
    ) {
        FinancialAnalysisResponse response = analysisService.getFinancials(user, documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
