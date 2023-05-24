package com.vprokopiv.ytbot;

import com.vprokopiv.ytbot.stats.HistoryService;
import com.vprokopiv.ytbot.yt.YouTubeService;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("!test")
public class AddToWlQueueWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(AddToWlQueueWatcher.class);
    static final String WL = "WL";

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
                    var update = queuesManager.takeAddToWlMessage().get();
                    
                    switch (update.updateType()) {
                        case WL_ADD:
                            youTubeService.addToWL(update.videoId());
                            historyService.setAddedToWl(update.videoId());
                            break;
                        case WL_REMOVE:
                            youTubeService.removeFromWL(update.videoId());
                            historyService.setRemovedFromWL(update.videoId());
                            break;
                        case LL_ADD:
                            youTubeService.addToLL(update.videoId());
                            historyService.setAddedToLl(update.videoId());
                            break;
                        case LL_REMOVE:
                            youTubeService.removeFromLL(update.videoId());
                            historyService.setRemovedFromLL(update.videoId());
                            break;
                        default:
                            LOG.warn("Unknown update type: {}", update.updateType());
                    }
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }, "AddToWlQueueWatcherThread"));
    }
}
