package com.aeroops.stands;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StandRepository extends JpaRepository<Stand, UUID> {

    List<Stand> findByTenantId(String tenantId);
}
