package com.aeroops.turnarounds;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRevisionRepository extends JpaRepository<TaskRevision, UUID> {

    List<TaskRevision> findByTaskIdAndTenantIdOrderByRecordedAtAsc(UUID taskId, String tenantId);
}
