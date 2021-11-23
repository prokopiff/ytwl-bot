package com.vprokopiv.ytbot;

import com.vprokopiv.ytbot.stats.HistoryService;
import com.vprokopiv.ytbot.yt.YouTubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("!test")
public class AddToWlQueueWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(AddToWlQueueWatcher.class);

    private final QueuesManager queuesManager;
    private final YouTubeService youTubeService;
    private final HistoryService historyService;
    private final ExecutorService executorService;

    public AddToWlQueueWatcher(QueuesManager queuesManager,
                               YouTubeService youTubeService,
                               HistoryService historyService) {
        this.queuesManager = queuesManager;
        this.youTubeService = youTubeService;
        this.historyService = historyService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    public void start() {
        LOG.info("Starting");
        executorService.submit(new Thread(() -> {
            while (true) {
                try {
                    var message = queuesManager.takeAddToWlMessage().get();
                    var id = message.substring(2);
                    if (message.startsWith("WL")) {
                        youTubeService.addToWL(id);
                        historyService.setAddedToWl(id);
                    } else {
                        youTubeService.addToLL(id);
                        historyService.setAddedToLl(id);
                    }
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }, "AddToWlQueueWatcherThread"));
    }
}
