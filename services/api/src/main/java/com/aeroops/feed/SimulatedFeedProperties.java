package com.aeroops.feed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dev-only configuration for the simulated partner feed (see application.yml's dev profile).
 * Stands in for "connect one real feed" (roadmap Phase 4) until a real partner is approved.
 */
@ConfigurationProperties(prefix = "aeroops.simulated-feed")
public class SimulatedFeedProperties {

    private boolean enabled = false;
    private String tenantId = "demo-airport";
    private String source = "SIMULATED_AOCC";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
