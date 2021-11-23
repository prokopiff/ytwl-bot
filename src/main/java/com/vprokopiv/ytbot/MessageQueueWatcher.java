package com.vprokopiv.ytbot;

import com.vprokopiv.ytbot.tg.Telegram;
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
public class MessageQueueWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(MessageQueueWatcher.class);

    private final QueuesManager queuesManager;
    private final Telegram telegram;
    private final ExecutorService executorService;

    public MessageQueueWatcher(QueuesManager queuesManager, Telegram telegram) {
        this.queuesManager = queuesManager;
        this.telegram = telegram;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    public void start() {
        LOG.info("Starting");
        executorService.submit(new Thread(() -> {
            while (true) {
                try {
                    var message = queuesManager.takeTgMessage().get();
                    telegram.sendMessage(message);
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }, "MessageQueueWatcherThread"));
    }
}
