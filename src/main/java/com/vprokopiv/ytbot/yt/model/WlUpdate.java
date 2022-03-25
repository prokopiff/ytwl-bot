package com.vprokopiv.ytbot.yt.model;

public record WlUpdate(UpdateType updateType, String videoId) {
    public enum UpdateType {
        WL_ADD("WL"),
        WL_REMOVE("DWL"),
        LL_ADD("LL"),
        LL_REMOVE("DLL");

        private final String prefix;

        UpdateType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public static WlUpdate of(String value) {
        if (value.startsWith(UpdateType.WL_ADD.getPrefix())) {
            return new WlUpdate(UpdateType.WL_ADD, value.substring(2));
        } else if (value.startsWith(UpdateType.LL_ADD.getPrefix())) {
            return new WlUpdate(UpdateType.LL_ADD, value.substring(2));
        } else if (value.startsWith(UpdateType.WL_REMOVE.getPrefix())) {
            return new WlUpdate(UpdateType.WL_REMOVE, value.substring(3));
        } else if (value.startsWith(UpdateType.LL_REMOVE.getPrefix())) {
            return new WlUpdate(UpdateType.LL_REMOVE, value.substring(3));
        } else {
            throw new IllegalArgumentException("Unknown update type: " + value);
        }
    }

    public static WlUpdate of(UpdateType updateType, String videoId) {
        return new WlUpdate(updateType, videoId);
    }

    @Override
    public String toString() {
        return updateType.getPrefix() + videoId;
    }
}
