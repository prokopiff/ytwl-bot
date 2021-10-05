package com.vprokopiv.ytbot.yt.model;

public record Vid(String id, String title) {
    public String getUrl() {
        return "https://www.youtube.com/watch?v=" + id;
    }

    @Override
    public String toString() {
        return getUrl() + " " + title;
    }
}
