package com.vprokopiv.ytbot.stats;

import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Lazy
public interface HistoryRepository extends PagingAndSortingRepository<HistoryEntry, String> {

    boolean existsById(String id);

    HistoryEntry save(HistoryEntry entry);

    Optional<HistoryEntry> findById(String id);

    void deleteAll();

    long count();

    Iterable<HistoryEntry> findAll();

    @Query(value = """
        WITH counts AS (
          SELECT
            channel_id,
            sum(
              CASE
                WHEN added_to_wl IS NOT NULL OR added_to_ll IS NOT NULL
                  THEN 1
                ELSE 0
              END
            ) AS added,
            count(*) AS total
          FROM
            history
          GROUP BY
            channel_id
        )
    
        SELECT
          channel_id
        FROM
          counts
        WHERE
          total > 0
          AND (added * 1.0 / total) < (:maxPct / 100.)
    """, nativeQuery = true)
    Set<String> getRarelyWatchedChannels(@Param("maxPct") int maxPct);
}
