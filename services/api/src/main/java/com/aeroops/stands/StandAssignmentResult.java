package com.aeroops.stands;

import java.util.List;

/** Outcome of one assignment attempt — either a conflict (no row created) or a saved assignment. */
public record StandAssignmentResult(boolean conflict, List<StandAssignment> conflicting, StandAssignment assignment) {

    public static StandAssignmentResult conflict(List<StandAssignment> conflicting) {
        return new StandAssignmentResult(true, conflicting, null);
    }

    public static StandAssignmentResult created(StandAssignment assignment) {
        return new StandAssignmentResult(false, List.of(), assignment);
    }
}
