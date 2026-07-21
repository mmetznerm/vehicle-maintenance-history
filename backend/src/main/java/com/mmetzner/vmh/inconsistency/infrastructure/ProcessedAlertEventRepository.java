package com.mmetzner.vmh.inconsistency.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedAlertEventRepository extends JpaRepository<ProcessedAlertEventEntity, ProcessedAlertEventId> {
}
