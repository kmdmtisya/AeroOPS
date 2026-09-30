package com.aeroops.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable thresholds for the rule-based delay-risk indicators (roadmap Phase 5, F08).
 * Follows the same plain-getter/setter @ConfigurationProperties shape as
 * {@link com.aeroops.feed.SimulatedFeedProperties}.
 */
@ConfigurationProperties(prefix = "aeroops.risk-thresholds")
public class RiskThresholdsProperties {

    private long onBlockDelayWarningMinutes = 15;
    private long onBlockDelayCriticalMinutes = 45;
    private long taskOverdueMinutes = 10;
    private boolean incidentSignalEnabled = true;
    private boolean standOverrideSignalEnabled = true;

    public long getOnBlockDelayWarningMinutes() {
        return onBlockDelayWarningMinutes;
    }

    public void setOnBlockDelayWarningMinutes(long onBlockDelayWarningMinutes) {
        this.onBlockDelayWarningMinutes = onBlockDelayWarningMinutes;
    }

    public long getOnBlockDelayCriticalMinutes() {
        return onBlockDelayCriticalMinutes;
    }

    public void setOnBlockDelayCriticalMinutes(long onBlockDelayCriticalMinutes) {
        this.onBlockDelayCriticalMinutes = onBlockDelayCriticalMinutes;
    }

    public long getTaskOverdueMinutes() {
        return taskOverdueMinutes;
    }

    public void setTaskOverdueMinutes(long taskOverdueMinutes) {
        this.taskOverdueMinutes = taskOverdueMinutes;
    }

    public boolean isIncidentSignalEnabled() {
        return incidentSignalEnabled;
    }

    public void setIncidentSignalEnabled(boolean incidentSignalEnabled) {
        this.incidentSignalEnabled = incidentSignalEnabled;
    }

    public boolean isStandOverrideSignalEnabled() {
        return standOverrideSignalEnabled;
    }

    public void setStandOverrideSignalEnabled(boolean standOverrideSignalEnabled) {
        this.standOverrideSignalEnabled = standOverrideSignalEnabled;
    }
}
