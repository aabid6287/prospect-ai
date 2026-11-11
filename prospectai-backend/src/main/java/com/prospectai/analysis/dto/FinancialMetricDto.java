package com.prospectai.analysis.dto;

public record FinancialMetricDto(
        String metricName,
        String period,
        String value,
        String unit,
        String sourceNote
) {
    public FinancialMetricDto(String metricName, String period, String value, String unit) {
        this(metricName, period, value, unit, null);
    }
}
