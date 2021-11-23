package com.vprokopiv.ytbot.yt.model;

public record Video(String id, String title, String description, Channel channel, Long duration) {
    public Video(Video video, Long duration) {
        this(video.id, video.title, video.description, video.channel, duration);
    }

    public String getUrl() {
        return "https://www.youtube.com/watch?v=" + id;
    }

    @Override
    public String toString() {
        return "%s [%s] %s".formatted(getUrl(), channel.title(), title);
    }
}
