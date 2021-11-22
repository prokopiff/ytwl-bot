package com.vprokopiv.ytbot.util;

import com.vprokopiv.ytbot.yt.model.Channel;

import java.io.PrintWriter;
import java.io.StringWriter;

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
}
