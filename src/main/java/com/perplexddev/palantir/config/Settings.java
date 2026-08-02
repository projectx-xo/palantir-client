package com.perplexddev.palantir.config;

import com.perplexddev.palantir.color.AppearanceColorKey;
import com.perplexddev.palantir.hud.HudAnchor;
import com.perplexddev.palantir.hud.HudOptions;
import com.perplexddev.palantir.notification.NotificationOptions;
import com.perplexddev.palantir.tracker.TrackedPlayers;
import com.perplexddev.palantir.tracker.WildcardNameMatcher;
import com.perplexddev.palantir.tracker.TrackerOptions;
import com.perplexddev.palantir.util.ColorUtil;
import com.perplexddev.palantir.webhook.WebhookOptions;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the Cloth Config registration and turns the raw config into the immutable option objects the
 * rest of the mod consumes.
 *
 * <p>Derivation happens once per config change, never per tick or per frame: tracked names are
 * normalised here, and colours are pre-multiplied with their opacity here.
 */
public final class Settings {

    private static final float PERCENT = 100.0f;

    private final me.shedaniel.autoconfig.ConfigHolder<PalantirConfig> holder;
    private final List<Runnable> changeListeners = new ArrayList<>();

    private TrackerOptions trackerOptions = TrackerOptions.DEFAULTS;
    private HudOptions hudOptions;
    private NotificationOptions notificationOptions;
    private WebhookOptions webhookOptions;
    private boolean modEnabled = true;
    private boolean debugLogging;
    private boolean showTeamNames;
    private boolean testNotificationPending;
    private boolean colorWheelPending;
    private boolean trackedPlayersEditorPending;
    private boolean ignoredPlayersEditorPending;
    private boolean trackedFactionsEditorPending;
    private boolean ignoredFactionsEditorPending;

    private Settings(me.shedaniel.autoconfig.ConfigHolder<PalantirConfig> holder) {
        this.holder = holder;
    }

    /** Registers the config with Cloth and derives the initial option set. */
    public static Settings register() {
        me.shedaniel.autoconfig.ConfigHolder<PalantirConfig> holder =
                AutoConfig.register(PalantirConfig.class, GsonConfigSerializer::new);

        Settings settings = new Settings(holder);
        settings.derive();

        holder.registerSaveListener((ignored, config) -> {
            boolean testRequested = config.advanced.testNotification;
            boolean colorWheelRequested = config.appearance.openColorWheel;
            boolean trackedEditorRequested = config.trackedPlayers.openEditor;
            boolean ignoredEditorRequested = config.ignoredPlayers.openEditor;
            boolean trackedFactionsEditorRequested = config.trackedPlayers.openFactionEditor;
            boolean ignoredFactionsEditorRequested = config.ignoredPlayers.openFactionEditor;
            settings.derive();
            settings.notifyListeners();
            settings.testNotificationPending |= testRequested;
            settings.colorWheelPending |= colorWheelRequested;
            settings.trackedPlayersEditorPending |= trackedEditorRequested;
            settings.ignoredPlayersEditorPending |= ignoredEditorRequested;
            settings.trackedFactionsEditorPending |= trackedFactionsEditorRequested;
            settings.ignoredFactionsEditorPending |= ignoredFactionsEditorRequested;
            return ActionResult.PASS;
        });
        holder.registerLoadListener((ignored, config) -> {
            settings.derive();
            settings.notifyListeners();
            return ActionResult.PASS;
        });

        return settings;
    }

    /**
     * Returns true once after the user ticks "test notification" in the config screen.
     *
     * <p>The stored flag is always cleared while deriving, so a config file that still contains it
     * cannot replay the notification at startup.
     */
    public boolean consumeTestNotificationRequest() {
        if (!testNotificationPending) {
            return false;
        }
        testNotificationPending = false;
        return true;
    }

    /**
     * Returns true once after the user ticks "Open Color Wheel" in the config screen.
     *
     * <p>The stored flag is always cleared while deriving, so a config file that still contains it
     * cannot reopen the color wheel on its own at startup.
     */
    public boolean consumeColorWheelRequest() {
        if (!colorWheelPending) {
            return false;
        }
        colorWheelPending = false;
        return true;
    }

    /** Returns true once after the user ticks "Open Editor" under Tracked Players and saves. */
    public boolean consumeTrackedPlayersEditorRequest() {
        if (!trackedPlayersEditorPending) {
            return false;
        }
        trackedPlayersEditorPending = false;
        return true;
    }

    /** Returns true once after the user ticks "Open Editor" under Ignored Players and saves. */
    public boolean consumeIgnoredPlayersEditorRequest() {
        if (!ignoredPlayersEditorPending) {
            return false;
        }
        ignoredPlayersEditorPending = false;
        return true;
    }

    /** Returns true once after the user ticks the faction editor toggle under Tracked Players. */
    public boolean consumeTrackedFactionsEditorRequest() {
        if (!trackedFactionsEditorPending) {
            return false;
        }
        trackedFactionsEditorPending = false;
        return true;
    }

    /** Returns true once after the user ticks the faction editor toggle under Ignored Players. */
    public boolean consumeIgnoredFactionsEditorRequest() {
        if (!ignoredFactionsEditorPending) {
            return false;
        }
        ignoredFactionsEditorPending = false;
        return true;
    }

    /** Runs whenever derived options change, so caches downstream can be invalidated. */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    /** Persists a new HUD anchor/offset/independent horizontal and vertical scale from the editor. */
    public void saveHudPlacement(HudAnchor anchor, int offsetX, int offsetY, int scaleXPercent, int scaleYPercent) {
        PalantirConfig config = holder.getConfig();
        config.hud.anchor = anchor;
        config.hud.offsetX = clamp(offsetX, PalantirConfig.Bounds.MIN_OFFSET, PalantirConfig.Bounds.MAX_OFFSET);
        config.hud.offsetY = clamp(offsetY, PalantirConfig.Bounds.MIN_OFFSET, PalantirConfig.Bounds.MAX_OFFSET);
        config.hud.scaleXPercent = clamp(scaleXPercent,
                PalantirConfig.Bounds.MIN_SCALE_PERCENT, PalantirConfig.Bounds.MAX_SCALE_PERCENT);
        config.hud.scaleYPercent = clamp(scaleYPercent,
                PalantirConfig.Bounds.MIN_SCALE_PERCENT, PalantirConfig.Bounds.MAX_SCALE_PERCENT);
        holder.save();
    }

    /** Current value of each of the nine editable colors, for seeding the color wheel editor. */
    public Map<AppearanceColorKey, Integer> appearanceColors() {
        PalantirConfig.AppearanceSettings appearance = holder.getConfig().appearance;
        Map<AppearanceColorKey, Integer> colors = new EnumMap<>(AppearanceColorKey.class);
        for (AppearanceColorKey key : AppearanceColorKey.values()) {
            colors.put(key, key.get(appearance));
        }
        return colors;
    }

    /** Persists all nine colors at once from the interactive color wheel editor. */
    public void saveAppearanceColors(Map<AppearanceColorKey, Integer> colors) {
        PalantirConfig.AppearanceSettings appearance = holder.getConfig().appearance;
        colors.forEach((key, argb) -> key.set(appearance, argb));
        holder.save();
    }

    /** Current tracked usernames, for seeding {@link PlayerListEditorScreen}. */
    public List<String> trackedPlayerNames() {
        return new ArrayList<>(holder.getConfig().trackedPlayers.names);
    }

    /** Persists the tracked-player list from the editor. */
    public void saveTrackedPlayerNames(List<String> names) {
        holder.getConfig().trackedPlayers.names = new ArrayList<>(names);
        holder.save();
    }

    /** Current ignore patterns, for seeding {@link PlayerListEditorScreen}. */
    public List<String> ignoredPlayerPatterns() {
        return new ArrayList<>(holder.getConfig().ignoredPlayers.names);
    }

    /** Persists the ignored-player pattern list from the editor. */
    public void saveIgnoredPlayerPatterns(List<String> patterns) {
        holder.getConfig().ignoredPlayers.names = new ArrayList<>(patterns);
        holder.save();
    }

    /** Current tracked-faction patterns, for seeding {@link PlayerListEditorScreen}. */
    public List<String> trackedFactionPatterns() {
        return new ArrayList<>(holder.getConfig().trackedPlayers.factions);
    }

    /** Persists the tracked-faction pattern list from the editor. */
    public void saveTrackedFactionPatterns(List<String> patterns) {
        holder.getConfig().trackedPlayers.factions = new ArrayList<>(patterns);
        holder.save();
    }

    /** Current ignored-faction patterns, for seeding {@link PlayerListEditorScreen}. */
    public List<String> ignoredFactionPatterns() {
        return new ArrayList<>(holder.getConfig().ignoredPlayers.factions);
    }

    /** Persists the ignored-faction pattern list from the editor. */
    public void saveIgnoredFactionPatterns(List<String> patterns) {
        holder.getConfig().ignoredPlayers.factions = new ArrayList<>(patterns);
        holder.save();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public TrackerOptions tracker() {
        return trackerOptions;
    }

    public HudOptions hud() {
        return hudOptions;
    }

    public NotificationOptions notifications() {
        return notificationOptions;
    }

    public WebhookOptions webhook() {
        return webhookOptions;
    }

    public boolean modEnabled() {
        return modEnabled;
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    public boolean showTeamNames() {
        return showTeamNames;
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    private void derive() {
        PalantirConfig config = holder.getConfig();

        config.advanced.testNotification = false;
        config.appearance.openColorWheel = false;
        config.trackedPlayers.openEditor = false;
        config.ignoredPlayers.openEditor = false;
        config.trackedPlayers.openFactionEditor = false;
        config.ignoredPlayers.openFactionEditor = false;
        modEnabled = config.general.enabled;
        debugLogging = config.advanced.debugLogging;
        showTeamNames = config.advanced.showTeamNames;

        trackerOptions = new TrackerOptions(
                TrackedPlayers.of(config.trackedPlayers.names),
                WildcardNameMatcher.of(config.ignoredPlayers.names),
                WildcardNameMatcher.of(config.trackedPlayers.factions),
                WildcardNameMatcher.of(config.ignoredPlayers.factions),
                config.general.notifyForPlayersAlreadyPresent,
                config.general.trackLocalPlayer,
                config.advanced.stabilityDelayMs);

        hudOptions = new HudOptions(
                config.hud.enabled,
                config.hud.anchor,
                config.hud.offsetX,
                config.hud.offsetY,
                config.hud.scaleXPercent / PERCENT,
                config.hud.scaleYPercent / PERCENT,
                config.hud.showTitle,
                config.hud.showTotalCount,
                config.hud.showTrackedCount,
                config.hud.showPlayerList,
                config.hud.showTrackedFirst,
                config.hud.maxVisiblePlayers,
                ColorUtil.withOpacityPercent(config.appearance.hudBackgroundColor,
                        config.hud.backgroundOpacityPercent),
                config.appearance.hudBorderColor,
                config.appearance.hudTitleColor,
                config.appearance.normalPlayerColor,
                config.appearance.trackedPlayerColor,
                config.appearance.roundedCorners,
                config.hud.hideWhenEmpty,
                config.hud.hideWithDebugScreen);

        notificationOptions = new NotificationOptions(
                config.notifications.enabled,
                config.notifications.corner,
                config.notifications.offsetX,
                config.notifications.offsetY,
                config.notifications.durationMs,
                config.notifications.fadeInMs,
                config.notifications.fadeOutMs,
                config.notifications.playSound,
                config.notifications.soundVolumePercent / PERCENT,
                config.notifications.scalePercent / PERCENT,
                config.notifications.maxSimultaneous,
                config.appearance.notificationBackgroundColor,
                config.appearance.notificationBorderColor,
                config.appearance.notificationTitleColor,
                config.appearance.notificationBodyColor,
                config.appearance.roundedCorners);

        webhookOptions = new WebhookOptions(config.webhook.enabled, config.webhook.webhookUrl, config.webhook.embedColor);
    }
}
