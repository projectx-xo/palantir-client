package com.perplexddev.palantir.color;

import com.perplexddev.palantir.config.PalantirConfig;

import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/** The nine editable colors in {@link PalantirConfig.AppearanceSettings}, in display order. */
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
    private final ToIntFunction<PalantirConfig.AppearanceSettings> getter;
    private final BiConsumer<PalantirConfig.AppearanceSettings, Integer> setter;

    AppearanceColorKey(String displayName,
                       ToIntFunction<PalantirConfig.AppearanceSettings> getter,
                       BiConsumer<PalantirConfig.AppearanceSettings, Integer> setter) {
        this.displayName = displayName;
        this.getter = getter;
        this.setter = setter;
    }

    public int get(PalantirConfig.AppearanceSettings appearance) {
        return getter.applyAsInt(appearance);
    }

    public void set(PalantirConfig.AppearanceSettings appearance, int argb) {
        setter.accept(appearance, argb);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
