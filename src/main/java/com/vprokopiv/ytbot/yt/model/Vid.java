package com.vprokopiv.ytbot.yt.model;

public record Vid(String id, String title, String channel, String duration) {
    public Vid(Vid vid, String duration) {
        this(vid.id, vid.title, vid.channel, duration);
    }

    public String getUrl() {
        return "https://www.youtube.com/watch?v=" + id;
    }

    @Override
    public String toString() {
        return "%s [%s] %s".formatted(getUrl(), channel, title);
    }
}
