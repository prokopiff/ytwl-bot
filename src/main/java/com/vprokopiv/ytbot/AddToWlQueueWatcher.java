package com.vprokopiv.ytbot;

import com.vprokopiv.ytbot.yt.YouTubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AddToWlQueueWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(AddToWlQueueWatcher.class);

    private final QueuesManager queuesManager;
    private final YouTubeService youTubeService;
    private final ExecutorService executorService;

    public AddToWlQueueWatcher(QueuesManager queuesManager, YouTubeService youTubeService) {
        this.queuesManager = queuesManager;
        this.youTubeService = youTubeService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    public void start() {
        LOG.info("Starting");
        executorService.submit(new Thread(() -> {
            while (true) {
                try {
                    var message = queuesManager.takeAddToWlMessage().get();
                    if (message.startsWith("WL")) {
                        youTubeService.addToWL(message.substring(2));
                    } else {
                        youTubeService.addToLL(message.substring(2));
                    }
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }, "AddToWlQueueWatcherThread"));
    }
}
