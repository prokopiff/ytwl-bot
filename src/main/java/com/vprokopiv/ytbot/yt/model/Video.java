package com.vprokopiv.ytbot.yt.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.vprokopiv.ytbot.util.Util.formatDuration;

public record Video(String id, String title, String description, Channel channel, Long duration, Long timestamp) {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("LLL dd HH:mm:ss");

    private static final ZoneOffset ZONE_OFFSET = OffsetDateTime.now().getOffset();

    public Video(Video video, Long duration) {
        this(video.id, video.title, video.description, video.channel, duration, video.timestamp);
    }

    public String getUrl() {
        return "https://www.youtube.com/watch?v=" + id;
    }

    @Override
    public String toString() {
        return "%s [%s] %s".formatted(getUrl(), channel.title(), title);
    }

    public String toMessageString() {
        return "*%s*\n%s\n%s\n%s\n\n%s".formatted(
                this.title(),
                this.channel().title(),
                formatDuration(this.duration()),
                getDateString(),
                this.getUrl());
    }

    private String getDateString() {
        return LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZONE_OFFSET)
                .format(DATE_TIME_FORMATTER);
    }
}
