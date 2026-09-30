package com.aeroops.ingest;

/** Outcome of one ingestion attempt, returned to the caller as the /v1/ingest/events response body. */
public record IngestResult(String status, String reason, java.util.UUID sourceEventId) {

    public static IngestResult accepted(java.util.UUID sourceEventId) {
        return new IngestResult(SourceEvent.STATUS_ACCEPTED, null, sourceEventId);
    }

    public static IngestResult duplicate(java.util.UUID sourceEventId) {
        return new IngestResult(SourceEvent.STATUS_DUPLICATE, null, sourceEventId);
    }

    public static IngestResult rejected(String reason, java.util.UUID sourceEventId) {
        return new IngestResult(SourceEvent.STATUS_REJECTED, reason, sourceEventId);
    }
}
