package com.aeroops.risk;

/**
 * One independently-explainable contributing signal behind a {@link RiskAssessment}.
 * Never an autonomous recommendation — always a field, the threshold it breached, the
 * actual observed value, and a human-readable explanation, per the roadmap's
 * "show reasons and contributing fields; no autonomous action" rule for F08.
 */
public record RiskReason(
        String field,
        String thresholdBreached,
        String actualValue,
        String explanation
) {
}
