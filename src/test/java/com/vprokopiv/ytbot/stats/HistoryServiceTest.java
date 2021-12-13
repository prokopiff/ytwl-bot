package com.vprokopiv.ytbot.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class HistoryServiceTest {
    @Autowired
    private HistoryService historyService;

    @BeforeEach
    void clean() {
        historyService.deleteAll();
    }

    @Test
    void testSaveEntries() {
        var entry = getTestEntry();
        historyService.save(entry);
        assertEquals(entry, historyService.findById(entry.getId()).get());
    }

    @Test
    void testSaveDuplicateEntries() {
        historyService.save(getTestEntry());
        assertThrows(IllegalStateException.class, () -> historyService.save(getTestEntry()));
    }

    @Test
    void testUpdate() {
        var entry = getTestEntry();
        historyService.save(entry);
        entry.setAddedToWl(1L);
        historyService.update(entry);
        assertEquals(1L, historyService.findById(entry.getId()).get().getAddedToWl());
    }

    private HistoryEntry getTestEntry() {
        return new HistoryEntry(
                "id1",
                "title",
                60L,
                "Description",
                "channelName-id",
                "Channel Name",
                null,
                null
        );
    }
}
