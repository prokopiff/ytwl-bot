package com.vprokopiv.ytbot.stats;

import org.springframework.context.annotation.Lazy;
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
}
