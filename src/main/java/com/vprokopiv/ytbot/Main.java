package com.vprokopiv.ytbot;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Activity;
import com.pengrad.telegrambot.request.SendMessage;
import com.vprokopiv.ytbot.tg.TG;
import com.vprokopiv.ytbot.yt.model.Channel;
import com.vprokopiv.ytbot.yt.model.Vid;
import com.vprokopiv.ytbot.yt.YT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("InfiniteLoopStatement")
public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    private static final BlockingQueue<SendMessage> tgMessagesQueue = new ArrayBlockingQueue<>(20);
    private static final BlockingQueue<String> addToWlQueue = new ArrayBlockingQueue<>(20);

    private static final int CHECK_INTERVAL_HRS = Config.getProperty("check-interval-hrs")
            .map(Integer::parseInt)
            .orElse(2);

    public static final String LAST_RUN_FILE = "last-run.txt";

    public static void main(String[] args)
            throws GeneralSecurityException, IOException, InterruptedException {

        var tg = TG.getInstance(Main::handleAddToWL);
        ExecutorService messageQueueWatcher = Executors.newFixedThreadPool(2);
        messageQueueWatcher.submit(new Thread(tgMessageQueueWatcher(tg), "TgQueueWatcher"));

        var yt = YT.getInstance(Main::handleMessage);
        messageQueueWatcher.submit(new Thread(addToWlQueueWatcher(yt), "AddToWlQueueWatcher"));

        mainLoop(tg, yt);
    }

    private static void mainLoop(TG tg, YT yt) throws InterruptedException {
        while (true) {
            try {
                LOG.info("Updating...");

                if (notDaytime()) { // Save some YT API credits
                    LOG.info("Not active hours, skipping");
                    continue;
                }

                DateTime after = getCheckTime(lastRun());
                LOG.info("Will get activities after {}", after);

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
                                activity.getSnippet().getTitle(),
                                activity.getSnippet().getChannelTitle()))
                        .toList();

                LOG.info("Got {} vids", vids.size());

                tg.sendVideos(vids);

                LOG.info("Done.");

                saveLastRunTime();

            } catch (Exception e) {
                tgMessagesQueue.put(TG.sendMessageOf(stringStackTrace(e)));
            } finally {
                sleep();
            }
        }
    }

    @NotNull
    private static String stringStackTrace(Exception e) {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private static void saveLastRunTime() throws IOException {
        String localDir = System.getProperty("user.home") + "/" + Config.getRequiredProperty("local-dir");
        File lastCheck = new File(localDir, LAST_RUN_FILE);
        try (FileWriter fw = new FileWriter(lastCheck)) {
            fw.write(String.valueOf(System.currentTimeMillis()));
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            throw e;
        }
    }

    private static long lastRun() {
        LOG.info("Getting last run time");
        String localDir = System.getProperty("user.home") + "/" + Config.getRequiredProperty("local-dir");
        var lastCheck = new File(localDir, LAST_RUN_FILE);
        if (!lastCheck.exists()) {
            LOG.info("No last check file. Returning 24hrs ago");
            return System.currentTimeMillis() - Duration.ofHours(24L).toMillis();
        }

        try (Stream<String> lines = Files.lines(lastCheck.toPath())) {
            var content = lines.collect(Collectors.joining());
            long ts = Long.parseLong(content.trim());
            LOG.info("Got last run time {}", ts);
            return ts;
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            LOG.info("Error reading last check file. Returning 24hrs ago");
            return System.currentTimeMillis() - Duration.ofHours(24L).toMillis();
        }
    }

    @NotNull
    private static DateTime getCheckTime(long lastRunTs) {
        return DateTime.parseRfc3339(
                ZonedDateTime.of(LocalDateTime.ofEpochSecond(lastRunTs / 1000, 0, ZoneOffset.UTC), ZoneOffset.UTC)
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
        LOG.info("Sleeping in main loop");
        Thread.sleep(Duration.ofHours(2).toMillis());
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
            tgMessagesQueue.put(TG.sendMessageOf(message));
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
