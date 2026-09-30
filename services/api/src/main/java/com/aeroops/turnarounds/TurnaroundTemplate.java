package com.aeroops.turnarounds;

import java.util.List;

/**
 * One fixed, hardcoded template for this phase — a template editor is out of scope
 * for Phase 3 (see docs/AEROOPS_EXECUTION_PLAN.md Step 3.4).
 */
public final class TurnaroundTemplate {

    public static final String NAME = "STANDARD_TURNAROUND";

    public static final List<String> STEPS = List.of(
            "SCHEDULED_UPDATE", "INBOUND_UPDATE", "ON_BLOCK", "DISEMBARK", "SERVICE", "BOARDING", "OFF_BLOCK");

    private TurnaroundTemplate() {
    }
}
