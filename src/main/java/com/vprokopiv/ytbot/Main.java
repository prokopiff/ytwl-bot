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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private static final ZoneOffset ZONE_OFFSET = OffsetDateTime.now().getOffset();

    public static final String LAST_RUN_FILE = "last-run.txt";
    public static final Comparator<Activity> ACTIVITY_COMPARATOR = Comparator.comparing(a -> a.getSnippet()
            .getPublishedAt().getValue());
    public static final long TWENTY_FOUR_HRS = Duration.ofHours(24L).toMillis();

    public static void main(String[] args)
            throws GeneralSecurityException, IOException, InterruptedException {

        LOG.info("Running with time zone offset {}", ZONE_OFFSET);

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
                long lastRunTs = lastRun();

                if (notDaytime() || lastRunTs > System.currentTimeMillis() - Duration.ofHours(2).toMillis()) {
                    // Save some YT API credits
                    LOG.info("Not active hours, skipping");
                    continue;
                }

                DateTime after = getCheckTime(lastRunTs);
                LOG.info("Will get activities after {}", after);

                List<Channel> subs = yt.getSubscriptions();
                LOG.info("Got {} subscriptions", subs.size());

                Stream<Activity> activities = subs.parallelStream().flatMap(channel -> {
                    List<Activity> channelActivities = yt.getActivities(channel.id(), after);
                    var titleLog = getFixedSizeTitle(channel);
                    LOG.info("Checking {}\t{} - {} updates", channel.id(), titleLog, channelActivities.size());
                    return channelActivities.stream();
                });

                var runTs = System.currentTimeMillis();

                List<Vid> vids = activities
                        .sorted(ACTIVITY_COMPARATOR)
                        .filter(a -> "upload".equals(a.getSnippet().getType()))
                        .map(activity -> new Vid(
                                activity.getContentDetails().getUpload().getVideoId(),
                                activity.getSnippet().getTitle(),
                                activity.getSnippet().getChannelTitle(),
                                ""))
                        .toList();

                LOG.info("Got {} vids", vids.size());
                try {
                    var ids = vids.stream().map(Vid::id).toList();
                    Map<String, Duration> durations = yt.getDurations(ids);
                    vids = vids.stream()
                            .map(vid -> {
                                Duration duration = durations.getOrDefault(vid.id(), null);
                                var durationStr = formatDuration(duration);

                                return new Vid(vid, durationStr);
                            })
                            .toList();
                } finally {
                    tg.sendVideos(vids);
                }

                String doneMsg = "Done. Run was from %s to %s".formatted(
                        LocalDateTime.ofEpochSecond(lastRunTs / 1000, 0, ZONE_OFFSET),
                        LocalDateTime.ofEpochSecond(runTs / 1000, 0, ZONE_OFFSET)
                );
                LOG.info(doneMsg);

                saveLastRunTime(runTs);

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                tgMessagesQueue.put(TG.sendMessageOf(stringStackTrace(e)));
            } finally {
                sleep();
            }
        }
    }

    static String formatDuration(Duration duration) {
        if (duration == null) {
            return "";
        }
        return (duration.toHoursPart() > 0 ? (duration.toHoursPart() + ":") : "")
                + "%02d:%02d".formatted(duration.toMinutesPart(), duration.toSecondsPart());
    }

    @NotNull
    private static String getFixedSizeTitle(Channel channel) {
        if (channel.title() == null) {
            return "NULL";
        }
        return channel.title().length() < 15
                ? lpad(channel.title(), " ", 15)
                : (channel.title().substring(0, 12) + "...");
    }

    private static String lpad(String val, String pad, int toSize) {
        StringBuilder valBuilder = new StringBuilder(val);
        while (valBuilder.length() < toSize) {
            valBuilder.insert(0, pad);
        }
        return valBuilder.toString();
    }

    @NotNull
    private static String stringStackTrace(Exception e) {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private static void saveLastRunTime(long runTs) throws IOException {
        String localDir = System.getProperty("user.home") + "/" + Config.getRequiredProperty("local-dir");
        File lastCheck = new File(localDir, LAST_RUN_FILE);
        try (FileWriter fw = new FileWriter(lastCheck)) {
            fw.write(String.valueOf(runTs));
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
            return System.currentTimeMillis() - TWENTY_FOUR_HRS;
        }

        try (Stream<String> lines = Files.lines(lastCheck.toPath())) {
            var content = lines.collect(Collectors.joining());
            long ts = Long.parseLong(content.trim());
            LOG.info("Got last run time {}", ts);
            return ts;
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            LOG.info("Error reading last check file. Returning 24hrs ago");
            return System.currentTimeMillis() - TWENTY_FOUR_HRS;
        }
    }

    @NotNull
    static DateTime getCheckTime(long lastRunTs) {
        return DateTime.parseRfc3339(
                ZonedDateTime.of(LocalDateTime.ofEpochSecond(lastRunTs / 1000, 0, ZONE_OFFSET), ZONE_OFFSET)
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
                    LOG.debug("Got a message: {}", id);
                    if (id.startsWith("WL")) {
                        yt.addToWL(id.substring(2));
                    } else {
                        yt.addToLL(id.substring(2));
                    }
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
