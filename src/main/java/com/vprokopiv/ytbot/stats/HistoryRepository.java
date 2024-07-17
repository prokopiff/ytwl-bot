package com.vprokopiv.ytbot.stats;

import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
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
            sum(if(added_to_wl IS NOT NULL OR added_to_ll IS NOT NULL, 1, 0)) AS added,
            count(*) AS total
          FROM
            history
          GROUP BY
            channel_id
        )
    
        SELECT
          channel_id,
          added * 1.0 / total
        FROM
          counts
        WHERE
          total > 0
    """, nativeQuery = true)
    Map<String, Float> getChannelsWatchPct();
}
