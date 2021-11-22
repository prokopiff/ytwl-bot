package com.vprokopiv.ytbot.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Lazy
public class HistoryService {
    private static final Logger LOG = LoggerFactory.getLogger(HistoryService.class);
    private final HistoryRepository historyRepository;

    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public HistoryEntry save(HistoryEntry entry) {
        if (historyRepository.existsById(entry.getId())) {
            throw new IllegalStateException(entry.getId() + " already exists");
        }
        return historyRepository.save(entry);
    }

    public HistoryEntry update(HistoryEntry entry) {
        return historyRepository.save(entry);
    }

    public boolean exists(String id) {
        return historyRepository.existsById(id);
    }

    public void setAddedToWl(String id) {
        Optional<HistoryEntry> entry = historyRepository.findById(id);
        entry.ifPresentOrElse(e -> {
            e.setAddedToWl(System.currentTimeMillis());
            historyRepository.save(e);
        }, () -> LOG.warn("Can't add to WL a video [{}] that doesn't exist", id));
    }
    public void setAddedToLl(String id) {
        Optional<HistoryEntry> entry = historyRepository.findById(id);
        entry.ifPresentOrElse(e -> {
            e.setAddedToLl(System.currentTimeMillis());
            historyRepository.save(e);
        }, () -> LOG.warn("Can't add to LL a video [{}] that doesn't exist", id));
    }

    public Optional<HistoryEntry> findById(String id) {
        return historyRepository.findById(id);
    }

    public void deleteAll() {
        historyRepository.deleteAll();
    }

    public long count() {
        return historyRepository.count();
    }

    public Iterable<HistoryEntry> getAll() {
        return historyRepository.findAll();
    }
}
