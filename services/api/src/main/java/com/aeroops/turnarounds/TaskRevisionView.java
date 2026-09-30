package com.aeroops.turnarounds;

import java.time.Instant;

public record TaskRevisionView(String actor, Instant actualAt, Instant recordedAt) {

    public static TaskRevisionView from(TaskRevision revision) {
        return new TaskRevisionView(revision.getActor(), revision.getActualAt(), revision.getRecordedAt());
    }
}
