package com.vprokopiv.ytbot;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Activity;
import com.vprokopiv.ytbot.config.Config;
import com.vprokopiv.ytbot.tg.Telegram;
import com.vprokopiv.ytbot.yt.YouTubeService;
import com.vprokopiv.ytbot.yt.model.Channel;
import com.vprokopiv.ytbot.yt.model.Vid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vprokopiv.ytbot.util.Util.getFixedSizeTitle;
import static com.vprokopiv.ytbot.util.Util.stringStackTrace;

@Component
public class PeriodicJob  {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicJob.class);

    private static final ZoneOffset ZONE_OFFSET = OffsetDateTime.now().getOffset();
    public static final String LAST_RUN_FILE = "last-run.txt";
    public static final Comparator<Activity> ACTIVITY_COMPARATOR = Comparator.comparing(a -> a.getSnippet()
            .getPublishedAt().getValue());
    public static final long TWENTY_FOUR_HRS = Duration.ofHours(24L).toMillis();

    private final Telegram telegram;
    private final YouTubeService youTubeService;
    private final QueuesManager queuesManager;

    private final String localDir;

    public PeriodicJob(Config config,
                       Telegram telegram,
                       YouTubeService youTubeService,
                       QueuesManager queuesManager) {
        this.localDir = config.getLocalDir();
        this.telegram = telegram;
        this.youTubeService = youTubeService;
        this.queuesManager = queuesManager;
    }

    @PostConstruct
    public void onStartup() {
        scheduled();
    }

    @Scheduled(cron = "0 0 0,9-23/2 * * *")
    public void scheduled() {
        try {
            LOG.info("Updating...");
            long lastRunTs = lastRun();

            if (lastRunTs > System.currentTimeMillis() - Duration.ofHours(2).toMillis()) {
                LOG.info("Last run was recently, skipping");
                return;
            }

            DateTime after = getCheckTime(lastRunTs);
            LOG.info("Will get activities after {}", after);

            List<Channel> subs = youTubeService.getSubscriptions();
            LOG.info("Got {} subscriptions", subs.size());

            Stream<Activity> activities = subs.parallelStream().flatMap(channel -> {
                List<Activity> channelActivities = youTubeService.getActivities(channel.id(), after);
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

            LOG.info("Got {} videos", vids.size());
            try {
                var ids = vids.stream().map(Vid::id).toList();
                Map<String, Duration> durations = youTubeService.getDurations(ids);
                vids = vids.stream()
                        .map(vid -> {
                            var duration = durations.get(vid.id());
                            var durationStr = formatDuration(duration);

                            return new Vid(vid, durationStr);
                        })
                        .toList();
            } finally {
                telegram.sendVideos(vids);
            }

            String doneMsg = "Done. Run was from %s to %s".formatted(
                    LocalDateTime.ofEpochSecond(lastRunTs / 1000, 0, ZONE_OFFSET),
                    LocalDateTime.ofEpochSecond(runTs / 1000, 0, ZONE_OFFSET)
            );
            LOG.info(doneMsg);

            saveLastRunTime(runTs);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            queuesManager.putTgMessage(telegram.sendMessageOf(stringStackTrace(e)));
        }
    }

    static String formatDuration(Duration duration) {
        if (duration == null) {
            return "";
        }
        return (duration.toHoursPart() > 0 ? (duration.toHoursPart() + ":") : "")
                + "%02d:%02d".formatted(duration.toMinutesPart(), duration.toSecondsPart());
    }



    private void saveLastRunTime(long runTs) throws IOException {
        String localDir = System.getProperty("user.home") + "/" + this.localDir;
        var lastCheck = new File(localDir, LAST_RUN_FILE);
        try (var fw = new FileWriter(lastCheck)) {
            fw.write(String.valueOf(runTs));
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            throw e;
        }
    }

    private long lastRun() {
        LOG.info("Getting last run time");
        String localDir = System.getProperty("user.home") + "/" + this.localDir;
        var lastCheck = new File(localDir, LAST_RUN_FILE);
        if (!lastCheck.exists()) {
            LOG.info("No last check file. Returning 24hrs ago");
            return System.currentTimeMillis() - TWENTY_FOUR_HRS;
        }

        try (Stream<String> lines = Files.lines(lastCheck.toPath())) {
            var content = lines.collect(Collectors.joining());
            var ts = Long.parseLong(content.trim());
            LOG.info("Got last run time {}", ts);
            return ts;
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            LOG.info("Error reading last check file. Returning 24hrs ago");
            return System.currentTimeMillis() - TWENTY_FOUR_HRS;
        }
    }

    static DateTime getCheckTime(long lastRunTs) {
        return DateTime.parseRfc3339(
                ZonedDateTime.of(LocalDateTime.ofEpochSecond(lastRunTs / 1000, 0, ZONE_OFFSET), ZONE_OFFSET)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSXXX"))
        );
    }
}
