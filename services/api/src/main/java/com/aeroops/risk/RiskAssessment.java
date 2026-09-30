package com.aeroops.risk;

import java.util.List;

/**
 * {@code lowConfidence} is set when the underlying flight data itself is stale — the
 * roadmap's "suppress confident recommendations when stale" rule. The level and reasons
 * are still reported (never hidden), just flagged as based on stale data.
 */
public record RiskAssessment(
        RiskLevel level,
        List<RiskReason> reasons,
        boolean lowConfidence
) {
}
