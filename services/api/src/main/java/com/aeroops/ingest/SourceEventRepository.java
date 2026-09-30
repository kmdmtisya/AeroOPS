package com.aeroops.ingest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SourceEventRepository extends JpaRepository<SourceEvent, UUID> {

    Optional<SourceEvent> findByTenantIdAndSourceAndExternalEventId(
            String tenantId, String source, String externalEventId);
}
