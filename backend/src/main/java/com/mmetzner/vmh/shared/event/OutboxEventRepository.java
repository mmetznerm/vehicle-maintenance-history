package com.mmetzner.vmh.shared.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE published_at IS NULL
              AND attempts < :maxAttempts
            ORDER BY sequence_id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> lockPendingBatch(
            @Param("batchSize") int batchSize,
            @Param("maxAttempts") int maxAttempts
    );
}
