package com.vprokopiv.ytbot.util;

import com.vprokopiv.ytbot.yt.model.Channel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

public class Util {
    private Util() {}

    public static String getFixedSizeTitle(Channel channel) {
        if (channel.title() == null) {
            return "NULL";
        }
        return channel.title().length() < 15
                ? lpad(channel.title(), " ", 15)
                : (channel.title().substring(0, 12) + "...");
    }

    public static String lpad(String val, String pad, int toSize) {
        StringBuilder valBuilder = new StringBuilder(val);
        while (valBuilder.length() < toSize) {
            valBuilder.insert(0, pad);
        }
        return valBuilder.toString();
    }

    public static String stringStackTrace(Exception e) {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static String formatDuration(Long duration) {
        if (duration == null) {
            return "";
        }
        return formatDuration(Duration.ofSeconds(duration));
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "";
        }
        return (duration.toHoursPart() > 0 ? (duration.toHoursPart() + ":") : "")
                + "%02d:%02d".formatted(duration.toMinutesPart(), duration.toSecondsPart());
    }
}
