package com.vprokopiv.ytbot;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Activity;
import com.vprokopiv.ytbot.config.Config;
import com.vprokopiv.ytbot.state.StateManager;
import com.vprokopiv.ytbot.stats.HistoryEntry;
import com.vprokopiv.ytbot.stats.HistoryService;
import com.vprokopiv.ytbot.tg.Telegram;
import com.vprokopiv.ytbot.yt.YouTubeService;
import com.vprokopiv.ytbot.yt.model.Channel;
import com.vprokopiv.ytbot.yt.model.Video;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vprokopiv.ytbot.util.Util.getFixedSizeTitle;
import static com.vprokopiv.ytbot.util.Util.stringStackTrace;
import static java.util.stream.Collectors.toMap;

@Profile("!test")
@Component
public class PeriodicJob  {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicJob.class);

    private static final ZoneOffset ZONE_OFFSET = OffsetDateTime.now().getOffset();
    public static final Comparator<Activity> ACTIVITY_COMPARATOR = Comparator.comparing(a ->
            a.getSnippet().getChannelTitle() + "|" + a.getSnippet().getPublishedAt().getValue()
    );
    public static final long TWENTY_FOUR_HRS = Duration.ofHours(24L).toMillis();
    static final String LAST_RUN_TS = "last_run_ts";

    private final Telegram telegram;
    private final YouTubeService youTubeService;
    private final QueuesManager queuesManager;
    private final HistoryService historyService;
    private final StateManager stateManager;
    private final Config config;

    public PeriodicJob(Telegram telegram,
                       YouTubeService youTubeService,
                       QueuesManager queuesManager,
                       HistoryService historyService,
                       StateManager stateManager, Config config) {
        this.telegram = telegram;
        this.youTubeService = youTubeService;
        this.queuesManager = queuesManager;
        this.historyService = historyService;
        this.stateManager = stateManager;
        this.config = config;
    }

    @PostConstruct
    public void onStartup() {
        scheduled();
    }

    @Scheduled(cron = "0 12 0,9-23/2 * * *")
    public void scheduled() {
        try {
            LOG.info("Updating...");
            Map<String, HistoryEntry> history = new HashMap<>();
            long lastRunTs = lastRun();

            if (lastRunTs > System.currentTimeMillis() - Duration.ofMinutes(115).toMillis()) {
                LOG.info("Last run was recently, skipping");
                return;
            }

            DateTime after = getCheckTime(lastRunTs);
            LOG.info("Will get activities after {}", after);

            List<Channel> subs = youTubeService.getSubscriptions();
            LOG.info("Got {} subscriptions", subs.size());

            Map<String, String> channelIdToName = subs.stream()
                    .collect(toMap(Channel::id, Channel::title));

            Stream<Activity> activities = subs.parallelStream().flatMap(channel -> {
                List<Activity> channelActivities = youTubeService.getActivities(channel.id(), after);
                var titleLog = getFixedSizeTitle(channel);
                LOG.info("Checking {}\t{} - {} updates", channel.id(), titleLog, channelActivities.size());
                return channelActivities.stream();
            });

            var runTs = System.currentTimeMillis();

            Set<String> rarelyWatchedChannels = historyService.getRarelyWatchedChannels(20);

            List<Video> videos = activities
                    .sorted(ACTIVITY_COMPARATOR)
                    .filter(a -> "upload".equals(a.getSnippet().getType()))
                    .map(activity -> new Video(
                            activity.getContentDetails().getUpload().getVideoId(),
                            activity.getSnippet().getTitle(),
                            activity.getSnippet().getDescription(),
                            new Channel(
                                    activity.getSnippet().getChannelId(),
                                    channelIdToName.get(activity.getSnippet().getChannelId())),
                            null,
                            activity.getSnippet().getPublishedAt().getValue(),
                            rarelyWatchedChannels.contains(activity.getSnippet().getChannelId())))
                    .toList();

            LOG.info("Got {} videos", videos.size());

            try {
                var ids = videos.stream().map(Video::id).toList();
                Map<String, Duration> durations = youTubeService.getDurations(ids);
                videos = videos.stream()
                        .map(vid -> {
                            var duration = Optional.ofNullable(durations.get(vid.id()));
                            return new Video(vid, duration.map(Duration::toSeconds).orElse(null));
                        })
                        .filter(v -> !config.isDisableShorts() || v.duration() == null || v.duration() > 61)
                        .toList();
                videos.forEach(video -> history.put(video.id(), new HistoryEntry(video)));
            } finally {
                telegram.sendVideos(videos);
            }

            String doneMsg = "Done. Run was from %s to %s".formatted(
                    LocalDateTime.ofEpochSecond(lastRunTs / 1000, 0, ZONE_OFFSET),
                    LocalDateTime.ofEpochSecond(runTs / 1000, 0, ZONE_OFFSET)
            );
            LOG.info(doneMsg);

            saveLastRunTime(runTs);
            historyService.saveAll(history.values());

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            queuesManager.putTgMessage(telegram.sendMessageOf(stringStackTrace(e)));
        }
    }

    private void saveLastRunTime(long runTs) {
        stateManager.setState(LAST_RUN_TS, String.valueOf(runTs));
    }

    private long lastRun() {
        var lastCheck = stateManager.getState(LAST_RUN_TS);
        if (lastCheck.isEmpty()) {
            LOG.info("No last check timestamp. Returning 24hrs ago");
            return System.currentTimeMillis() - TWENTY_FOUR_HRS;
        }
        return Long.parseLong(lastCheck.get());
    }

    static DateTime getCheckTime(long lastRunTs) {
        return DateTime.parseRfc3339(
                ZonedDateTime.of(LocalDateTime.ofEpochSecond(lastRunTs / 1000, 0, ZONE_OFFSET), ZONE_OFFSET)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSXXX"))
        );
    }
}
