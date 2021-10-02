package com.vprokopiv.ytbot;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.*;
import com.pengrad.telegrambot.request.SendMessage;
import com.vprokopiv.ytbot.tg.TG;
import com.vprokopiv.ytbot.yt.Channel;
import com.vprokopiv.ytbot.yt.Vid;
import com.vprokopiv.ytbot.yt.YT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("InfiniteLoopStatement")
public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static final BlockingQueue<SendMessage> tgMessagesQueue = new ArrayBlockingQueue<>(100);
    public static final BlockingQueue<String> addToWlQueue = new ArrayBlockingQueue<>(100);

    private static final int CHECK_INTERVAL_HRS = Config.getProperty("check-interval-hrs")
            .map(Integer::parseInt)
            .orElse(2);

    private static final Duration DURATION = Duration.ofHours(CHECK_INTERVAL_HRS);
    public static final String LAST_RUN_FILE = "last-run.txt";

    public static void main(String[] args)
            throws GeneralSecurityException, IOException, InterruptedException {

        TG tg = TG.getInstance(Main::handleAddToWL);

        YT yt = YT.getInstance(Main::handleMessage);

        ExecutorService messageQueueWatcher = Executors.newFixedThreadPool(2);
        messageQueueWatcher.submit(new Thread(tgMessageQueueWatcher(tg), "TgQueueWatcher"));
        messageQueueWatcher.submit(new Thread(addToWlQueueWatcher(yt), "AddToWlQueueWatcher"));

        mainLoop(tg, yt);
    }

    private static void mainLoop(TG tg, YT yt) throws InterruptedException, IOException {
        while (true) {
            LOG.info("Updating...");

            if (notDaytime() || !runRequired()) { // Save some YT API credits
                LOG.info("Not active hours or last run was recently, skipping");
                sleep();
                continue;
            }

            DateTime after = getCheckTime();

            List<Channel> subs = yt.getSubscriptions();
            LOG.info("Got {} subscriptions", subs.size());

            Stream<Activity> activities = subs.parallelStream().flatMap(channel -> {
                LOG.info("Checking {}\t{}", channel.id(), channel.title());
                List<Activity> channelActivities = yt.getActivities(channel.id(), after);
                return channelActivities.stream();
            });

            Comparator<Activity> activityComparator = Comparator.comparing(a -> a.getSnippet()
                    .getPublishedAt().getValue());

            List<Vid> vids = activities
                    .sorted(activityComparator)
                    .filter(a -> "upload".equals(a.getSnippet().getType()))
                    .map(activity -> new Vid(
                            activity.getContentDetails().getUpload().getVideoId(),
                            activity.getSnippet().getTitle()))
                    .toList();

            LOG.info("Got {} vids", vids.size());

            tg.sendVideos(vids);

            LOG.info("Done.");

            saveLastRunTime();

            sleep();
        }
    }

    private static void saveLastRunTime() {
        String localDir = System.getProperty("user.home") + "/" + Config.getRequiredProperty("local-dir");
        File lastCheck = new File(localDir, LAST_RUN_FILE);
        try (FileWriter fw = new FileWriter(lastCheck)) {
            fw.write(String.valueOf(System.currentTimeMillis()));
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private static boolean runRequired() {
        String localDir = System.getProperty("user.home") + "/" + Config.getRequiredProperty("local-dir");
        File lastCheck = new File(localDir, LAST_RUN_FILE);
        if (!lastCheck.exists()) {
            return true;
        }

        try {
            var content = Files.lines(lastCheck.toPath()).collect(Collectors.joining());
            var ts = Long.parseLong(content.trim());
            return ts < System.currentTimeMillis() - DURATION.toMillis();
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            return true;
        }
    }

    @NotNull
    private static DateTime getCheckTime() {
        return DateTime.parseRfc3339(
                ZonedDateTime.now().minus(DURATION)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSXXX"))
        );
    }

    @NotNull
    private static Runnable addToWlQueueWatcher(YT yt) {
        return () -> {
            LOG.info("Starting add to WL queue checker");
            while (true) {
                try {
                    String id = addToWlQueue.take();
                    LOG.debug("Got a message");
                    yt.addToWL(id);
                } catch (InterruptedException | IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
    }

    @NotNull
    private static Runnable tgMessageQueueWatcher(TG tg) {
        return () -> {
            LOG.info("Starting TG message queue checker");
            while (true) {
                try {
                    SendMessage msg = tgMessagesQueue.take();
                    LOG.debug("Got a message");
                    tg.sendMessage(msg);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
    }

    private static boolean notDaytime() {
        var hour = LocalDateTime.now().getHour();
        return hour < 9;
    }

    private static void sleep() throws InterruptedException {
        Thread.sleep(DURATION.toMillis());
    }

    private static void handleAddToWL(String id) {
        try {
            addToWlQueue.put(id);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static void handleMessage(String message) {
        try {
            tgMessagesQueue.put(new SendMessage(TG.CHAT_ID, message));
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}


