package com.perplexddev.palantir.config;

import com.perplexddev.palantir.hud.HudAnchor;
import com.perplexddev.palantir.notification.NotificationCorner;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Serialized mod configuration.
 *
 * <p>This type is plain data: it is read by {@link ConfigHolder} once per config change and turned
 * into the immutable option objects the tracker, HUD and notification systems actually consume.
 * Gameplay code never reads these fields directly.
 */
@Config(name = "palantir")
public class PalantirConfig implements ConfigData {

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    public GeneralSettings general = new GeneralSettings();

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.TransitiveObject
    public HudSettings hud = new HudSettings();

    @ConfigEntry.Category("notifications")
    @ConfigEntry.Gui.TransitiveObject
    public NotificationSettings notifications = new NotificationSettings();

    @ConfigEntry.Category("tracked_players")
    @ConfigEntry.Gui.TransitiveObject
    public TrackedPlayerSettings trackedPlayers = new TrackedPlayerSettings();

    @ConfigEntry.Category("ignored_players")
    @ConfigEntry.Gui.TransitiveObject
    public IgnoredPlayerSettings ignoredPlayers = new IgnoredPlayerSettings();

    @ConfigEntry.Category("appearance")
    @ConfigEntry.Gui.TransitiveObject
    public AppearanceSettings appearance = new AppearanceSettings();

    @ConfigEntry.Category("advanced")
    @ConfigEntry.Gui.TransitiveObject
    public AdvancedSettings advanced = new AdvancedSettings();

    /**
     * Clamps hand-edited values back into range. Returning normally (rather than throwing) keeps a
     * partially invalid file usable instead of silently resetting every other setting.
     */
    @Override
    public void validatePostLoad() {
        hud.offsetX = clamp(hud.offsetX, Bounds.MIN_OFFSET, Bounds.MAX_OFFSET);
        hud.offsetY = clamp(hud.offsetY, Bounds.MIN_OFFSET, Bounds.MAX_OFFSET);
        hud.scaleXPercent = clamp(hud.scaleXPercent, Bounds.MIN_SCALE_PERCENT, Bounds.MAX_SCALE_PERCENT);
        hud.scaleYPercent = clamp(hud.scaleYPercent, Bounds.MIN_SCALE_PERCENT, Bounds.MAX_SCALE_PERCENT);
        hud.maxVisiblePlayers = clamp(hud.maxVisiblePlayers, Bounds.MIN_VISIBLE_PLAYERS, Bounds.MAX_VISIBLE_PLAYERS);
        hud.backgroundOpacityPercent = clamp(hud.backgroundOpacityPercent, Bounds.MIN_PERCENT, Bounds.MAX_PERCENT);

        notifications.offsetX = clamp(notifications.offsetX, Bounds.MIN_OFFSET, Bounds.MAX_OFFSET);
        notifications.offsetY = clamp(notifications.offsetY, Bounds.MIN_OFFSET, Bounds.MAX_OFFSET);
        notifications.durationMs = clamp(notifications.durationMs, Bounds.MIN_DURATION_MS, Bounds.MAX_DURATION_MS);
        notifications.fadeInMs = clamp(notifications.fadeInMs, Bounds.MIN_FADE_MS, Bounds.MAX_FADE_MS);
        notifications.fadeOutMs = clamp(notifications.fadeOutMs, Bounds.MIN_FADE_MS, Bounds.MAX_FADE_MS);
        notifications.soundVolumePercent = clamp(notifications.soundVolumePercent, Bounds.MIN_PERCENT, Bounds.MAX_PERCENT);
        notifications.scalePercent = clamp(notifications.scalePercent, Bounds.MIN_SCALE_PERCENT, Bounds.MAX_SCALE_PERCENT);
        notifications.maxSimultaneous = clamp(notifications.maxSimultaneous, Bounds.MIN_SIMULTANEOUS, Bounds.MAX_SIMULTANEOUS);

        advanced.stabilityDelayMs = clamp(advanced.stabilityDelayMs, Bounds.MIN_STABILITY_MS, Bounds.MAX_STABILITY_MS);

        if (trackedPlayers.names == null) {
            trackedPlayers.names = new ArrayList<>();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Shared numeric bounds, referenced by both the annotations and {@link #validatePostLoad()}. */
    public static final class Bounds {
        public static final int MIN_OFFSET = -1000;
        public static final int MAX_OFFSET = 1000;
        public static final int MIN_PERCENT = 0;
        public static final int MAX_PERCENT = 100;
        public static final int MIN_SCALE_PERCENT = 25;
        public static final int MAX_SCALE_PERCENT = 300;
        /** 0 means unlimited: the panel grows to fit every detected player. */
        public static final int MIN_VISIBLE_PLAYERS = 0;
        public static final int MAX_VISIBLE_PLAYERS = 500;
        public static final int MIN_DURATION_MS = 500;
        public static final int MAX_DURATION_MS = 30000;
        public static final int MIN_FADE_MS = 0;
        public static final int MAX_FADE_MS = 5000;
        public static final int MIN_SIMULTANEOUS = 1;
        public static final int MAX_SIMULTANEOUS = 10;
        public static final int MIN_STABILITY_MS = 0;
        public static final int MAX_STABILITY_MS = 1000;

        private Bounds() {
        }
    }

    public static class GeneralSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean trackLocalPlayer = false;

        @ConfigEntry.Gui.Tooltip
        public boolean notifyForPlayersAlreadyPresent = false;
    }

    public static class HudSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public HudAnchor anchor = HudAnchor.TOP_LEFT;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_OFFSET, max = Bounds.MAX_OFFSET)
        public int offsetX = 8;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_OFFSET, max = Bounds.MAX_OFFSET)
        public int offsetY = 8;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_SCALE_PERCENT, max = Bounds.MAX_SCALE_PERCENT)
        @ConfigEntry.Gui.Tooltip
        public int scaleXPercent = 100;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_SCALE_PERCENT, max = Bounds.MAX_SCALE_PERCENT)
        @ConfigEntry.Gui.Tooltip
        public int scaleYPercent = 100;

        public boolean showTitle = true;

        public boolean showTotalCount = true;

        public boolean showTrackedCount = true;

        @ConfigEntry.Gui.Tooltip
        public boolean showPlayerList = true;

        @ConfigEntry.Gui.Tooltip
        public boolean showTrackedFirst = true;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_VISIBLE_PLAYERS, max = Bounds.MAX_VISIBLE_PLAYERS)
        @ConfigEntry.Gui.Tooltip
        public int maxVisiblePlayers = 0;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_PERCENT, max = Bounds.MAX_PERCENT)
        public int backgroundOpacityPercent = 70;

        @ConfigEntry.Gui.Tooltip
        public boolean hideWhenEmpty = false;

        @ConfigEntry.Gui.Tooltip
        public boolean hideWithDebugScreen = true;
    }

    public static class NotificationSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public NotificationCorner corner = NotificationCorner.BOTTOM_RIGHT;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_OFFSET, max = Bounds.MAX_OFFSET)
        public int offsetX = 8;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_OFFSET, max = Bounds.MAX_OFFSET)
        public int offsetY = 8;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_DURATION_MS, max = Bounds.MAX_DURATION_MS)
        @ConfigEntry.Gui.Tooltip
        public int durationMs = 5000;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_FADE_MS, max = Bounds.MAX_FADE_MS)
        public int fadeInMs = 200;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_FADE_MS, max = Bounds.MAX_FADE_MS)
        public int fadeOutMs = 400;

        @ConfigEntry.Gui.Tooltip
        public boolean playSound = true;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_PERCENT, max = Bounds.MAX_PERCENT)
        public int soundVolumePercent = 60;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_SCALE_PERCENT, max = Bounds.MAX_SCALE_PERCENT)
        public int scalePercent = 100;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_SIMULTANEOUS, max = Bounds.MAX_SIMULTANEOUS)
        @ConfigEntry.Gui.Tooltip
        public int maxSimultaneous = 4;
    }

    public static class TrackedPlayerSettings {
        /**
         * Transient action rather than a stored preference: it is consumed and cleared every time
         * the configuration is read, so it can never reopen the editor on its own at startup.
         */
        @ConfigEntry.Gui.Tooltip
        public boolean openEditor = false;

        @ConfigEntry.Gui.Tooltip
        public boolean openFactionEditor = false;

        /**
         * Edited only through {@link com.perplexddev.palantir.config.PlayerListEditorScreen},
         * not Cloth Config's own list widget: its add button does not reliably respond to clicks in
         * this Cloth Config version.
         */
        @ConfigEntry.Gui.Excluded
        public List<String> names = new ArrayList<>(Arrays.asList("PlayerOne", "DangerousPlayer"));

        /** Any member of a matching faction is treated as tracked, same as an individual username. */
        @ConfigEntry.Gui.Excluded
        public List<String> factions = new ArrayList<>();
    }

    public static class IgnoredPlayerSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean openEditor = false;

        @ConfigEntry.Gui.Tooltip
        public boolean openFactionEditor = false;

        @ConfigEntry.Gui.Excluded
        public List<String> names = new ArrayList<>();

        /** Any member of a matching faction is hidden entirely, same as an individually ignored username. */
        @ConfigEntry.Gui.Excluded
        public List<String> factions = new ArrayList<>();
    }

    public static class AppearanceSettings {
        /**
         * Transient action rather than a stored preference: it is consumed and cleared every time
         * the configuration is read, so it can never reopen the color wheel on its own at startup.
         */
        @ConfigEntry.Gui.Tooltip
        public boolean openColorWheel = false;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int hudBackgroundColor = 0xB0101014;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int hudBorderColor = 0xFF3C4A5A;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int hudTitleColor = 0xFF7FD1FF;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int normalPlayerColor = 0xFFDCE3EC;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int trackedPlayerColor = 0xFFFFC44D;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int notificationBackgroundColor = 0xE0121218;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int notificationBorderColor = 0xFFFFC44D;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int notificationTitleColor = 0xFFFFC44D;

        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int notificationBodyColor = 0xFFEDF1F6;

        @ConfigEntry.Gui.Tooltip
        public boolean roundedCorners = true;
    }

    public static class AdvancedSettings {
        /**
         * Transient action rather than a stored preference: it is consumed and cleared every time
         * the configuration is read, so it can never fire on its own at startup.
         */
        @ConfigEntry.Gui.Tooltip
        public boolean testNotification = false;

        @ConfigEntry.BoundedDiscrete(min = Bounds.MIN_STABILITY_MS, max = Bounds.MAX_STABILITY_MS)
        @ConfigEntry.Gui.Tooltip
        public int stabilityDelayMs = 0;

        @ConfigEntry.Gui.Tooltip
        public boolean debugLogging = false;

        @ConfigEntry.Gui.Tooltip
        public boolean showTeamNames = false;
    }
}
