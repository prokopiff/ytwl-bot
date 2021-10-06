package com.vprokopiv.ytbot.yt.model;

public record Vid(String id, String title, String channel) {
    public String getUrl() {
        return "https://www.youtube.com/watch?v=" + id;
    }

    @Override
    public String toString() {
        return "%s [%s] %s".formatted(getUrl(), channel, title);
    }
}
