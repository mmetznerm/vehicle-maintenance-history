package com.mmetzner.vmh.consistency.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertOutboxRepository extends JpaRepository<AlertOutboxEntity, Long> {

    @Query(value = """
            SELECT * FROM alert_outbox_events
            WHERE published_at IS NULL AND attempts < :maxAttempts
            ORDER BY sequence_id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AlertOutboxEntity> lockPendingBatch(
            @Param("batchSize") int batchSize,
            @Param("maxAttempts") int maxAttempts
    );
}
