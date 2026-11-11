package com.prospectai.common;

import com.prospectai.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoint for ProspectAI.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> healthData = Map.of(
            "status", "UP",
            "application", "ProspectAI",
            "version", "0.0.1-SNAPSHOT"
        );
        return ResponseEntity.ok(ApiResponse.success(healthData));
    }
}
