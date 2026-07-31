package com.perplexddev.shardtracker.color;

import com.perplexddev.shardtracker.config.ShardTrackerConfig;

import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/** The nine editable colors in {@link ShardTrackerConfig.AppearanceSettings}, in display order. */
public enum AppearanceColorKey {
    HUD_BACKGROUND("HUD Background",
            a -> a.hudBackgroundColor, (a, v) -> a.hudBackgroundColor = v),
    HUD_BORDER("HUD Border",
            a -> a.hudBorderColor, (a, v) -> a.hudBorderColor = v),
    HUD_TITLE("HUD Title",
            a -> a.hudTitleColor, (a, v) -> a.hudTitleColor = v),
    NORMAL_PLAYER("Normal Player",
            a -> a.normalPlayerColor, (a, v) -> a.normalPlayerColor = v),
    TRACKED_PLAYER("Tracked Player",
            a -> a.trackedPlayerColor, (a, v) -> a.trackedPlayerColor = v),
    NOTIFICATION_BACKGROUND("Notification Background",
            a -> a.notificationBackgroundColor, (a, v) -> a.notificationBackgroundColor = v),
    NOTIFICATION_BORDER("Notification Border",
            a -> a.notificationBorderColor, (a, v) -> a.notificationBorderColor = v),
    NOTIFICATION_TITLE("Notification Title",
            a -> a.notificationTitleColor, (a, v) -> a.notificationTitleColor = v),
    NOTIFICATION_BODY("Notification Body",
            a -> a.notificationBodyColor, (a, v) -> a.notificationBodyColor = v);

    private final String displayName;
    private final ToIntFunction<ShardTrackerConfig.AppearanceSettings> getter;
    private final BiConsumer<ShardTrackerConfig.AppearanceSettings, Integer> setter;

    AppearanceColorKey(String displayName,
                       ToIntFunction<ShardTrackerConfig.AppearanceSettings> getter,
                       BiConsumer<ShardTrackerConfig.AppearanceSettings, Integer> setter) {
        this.displayName = displayName;
        this.getter = getter;
        this.setter = setter;
    }

    public int get(ShardTrackerConfig.AppearanceSettings appearance) {
        return getter.applyAsInt(appearance);
    }

    public void set(ShardTrackerConfig.AppearanceSettings appearance, int argb) {
        setter.accept(appearance, argb);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
