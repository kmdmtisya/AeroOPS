package com.aeroops.risk;

import java.util.List;

/**
 * API projection of a RiskAssessment. Matches the record + static from(...) convention
 * used by com.aeroops.flights.FlightView and the other view DTOs in this codebase.
 */
public record RiskView(
        RiskLevel level,
        List<RiskReason> reasons,
        boolean lowConfidence
) {

    public static RiskView from(RiskAssessment assessment) {
        return new RiskView(assessment.level(), assessment.reasons(), assessment.lowConfidence());
    }
}
