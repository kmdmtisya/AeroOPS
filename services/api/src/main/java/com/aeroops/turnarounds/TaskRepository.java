package com.aeroops.turnarounds;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByTurnaroundIdAndTenantIdOrderBySequenceAsc(UUID turnaroundId, String tenantId);

    Optional<Task> findByIdAndTurnaroundIdAndTenantId(UUID id, UUID turnaroundId, String tenantId);
}
