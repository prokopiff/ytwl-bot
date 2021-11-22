package com.vprokopiv.ytbot.stats;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
@Lazy
public interface HistoryRepository extends PagingAndSortingRepository<HistoryEntry, String> {

}
