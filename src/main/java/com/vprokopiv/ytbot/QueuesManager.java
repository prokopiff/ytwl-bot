package com.vprokopiv.ytbot;

import com.pengrad.telegrambot.request.SendMessage;
import com.vprokopiv.ytbot.yt.model.WlUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

@Component
@Lazy
public class QueuesManager {
    private static final Logger LOG = LoggerFactory.getLogger(QueuesManager.class);

    private final BlockingQueue<SendMessage> tgMessagesQueue = new ArrayBlockingQueue<>(20);
    private final BlockingQueue<WlUpdate> addToWlQueue = new ArrayBlockingQueue<>(20);

    public void putTgMessage(SendMessage message) {
        try {
            tgMessagesQueue.put(message);
        } catch (InterruptedException e) {
            LOG.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<SendMessage> takeTgMessage() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return tgMessagesQueue.take();
            } catch (InterruptedException e) {
                LOG.warn(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    public WlUpdate putWlUpdateMessage(String message) {
        try {
            WlUpdate update = WlUpdate.of(message);
            addToWlQueue.put(update);
            return update;
        } catch (InterruptedException e) {
            LOG.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<WlUpdate> takeAddToWlMessage() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return addToWlQueue.take();
            } catch (InterruptedException e) {
                LOG.warn(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
}
