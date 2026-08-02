package com.perplexddev.palantir;

import com.perplexddev.palantir.color.ColorWheelScreen;
import com.perplexddev.palantir.config.PlayerListEditorScreen;
import com.perplexddev.palantir.config.Settings;
import com.perplexddev.palantir.hud.HudEditScreen;
import com.perplexddev.palantir.hud.ShardHudRenderer;
import com.perplexddev.palantir.keybind.PalantirKeyBindings;
import com.perplexddev.palantir.notification.NotificationManager;
import com.perplexddev.palantir.notification.NotificationRenderer;
import com.perplexddev.palantir.notification.NotificationSound;
import com.perplexddev.palantir.notification.ShardNotification;
import com.perplexddev.palantir.tracker.ObservedPlayer;
import com.perplexddev.palantir.tracker.ShardScanner;
import com.perplexddev.palantir.tracker.ShardTracker;
import com.perplexddev.palantir.tracker.ShardUpdate;
import com.perplexddev.palantir.webhook.HttpWebhookSender;
import com.perplexddev.palantir.webhook.WebhookNotifier;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.slf4j.Logger;

import java.util.List;

/**
 * Wires the tracker, HUD and notification systems to Fabric's client events.
 *
 * <p>The detection pass runs on {@code END_CLIENT_TICK} so a tracked player's arrival is picked up
 * the moment the client applies the tab-list or scoreboard update. Everything expensive is guarded:
 * the pass returns immediately when disconnected, and derived render data is only rebuilt when the
 * shard membership or the configuration actually changes.
 */
public final class PalantirRuntime {

    private static final String NOTIFICATION_TITLE = "TRACKED PLAYER DETECTED";
    private static final String ENTERED_SUFFIX = " entered the shard";
    private static final String SAMPLE_PLAYER = "PlayerOne";

    private static PalantirRuntime instance;

    private final MinecraftClient client;
    private final Settings settings;
    private final Logger logger;

    private final ShardScanner scanner = new ShardScanner();
    private final ShardTracker tracker;
    private final NotificationManager notifications;
    private final NotificationSound sound;
    private final ShardHudRenderer hudRenderer;
    private final NotificationRenderer notificationRenderer;
    private final WebhookNotifier webhookNotifier;

    private boolean tracking;

    /**
     * Runtime-only visibility toggle for {@link PalantirKeyBindings#TOGGLE_HUD}. Deliberately
     * not persisted to config: it is a quick in-session hide, distinct from the "Enable HUD"
     * config option, and always resets to visible on restart.
     */
    private boolean hudVisible = true;

    private PalantirRuntime(MinecraftClient client, Settings settings, Logger logger) {
        this.client = client;
        this.settings = settings;
        this.logger = logger;
        this.tracker = new ShardTracker(settings.tracker());
        this.notifications = new NotificationManager(settings.notifications());
        this.sound = new NotificationSound(client);
        this.hudRenderer = new ShardHudRenderer(client);
        this.notificationRenderer = new NotificationRenderer(client);
        this.webhookNotifier = new WebhookNotifier(new HttpWebhookSender(logger));
    }

    public static PalantirRuntime create(MinecraftClient client, Settings settings, Logger logger) {
        PalantirRuntime runtime = new PalantirRuntime(client, settings, logger);
        runtime.registerEvents();
        instance = runtime;
        return runtime;
    }

    /** May be null before client initialisation completes. */
    public static PalantirRuntime instance() {
        return instance;
    }

    private void registerEvents() {
        settings.addChangeListener(this::onConfigChanged);

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, minecraftClient) -> resetTracking());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraftClient) -> onDisconnect());

        HudRenderCallback.EVENT.register(this::onHudRender);

        // Keeps the test notification visible when the config screen is opened outside a world.
        ScreenEvents.AFTER_INIT.register((minecraftClient, screen, width, height) ->
                ScreenEvents.afterRender(screen).register((currentScreen, matrices, mouseX, mouseY, delta) -> {
                    if (client.world == null) {
                        notificationRenderer.render(matrices, notifications,
                                settings.notifications(), System.currentTimeMillis());
                    }
                }));
    }

    private void onEndClientTick(MinecraftClient ignored) {
        handleKeyBindings();

        long now = System.currentTimeMillis();

        if (settings.consumeTestNotificationRequest()) {
            showTestNotification(now);
        }
        if (settings.consumeColorWheelRequest()) {
            client.setScreen(new ColorWheelScreen(settings));
        }
        if (settings.consumeTrackedPlayersEditorRequest()) {
            client.setScreen(new PlayerListEditorScreen(new LiteralText("Tracked Players"), "",
                    "username", settings.trackedPlayerNames(), settings::saveTrackedPlayerNames));
        }
        if (settings.consumeIgnoredPlayersEditorRequest()) {
            client.setScreen(new PlayerListEditorScreen(new LiteralText("Ignored Players"),
                    "Use * as a wildcard, e.g. hoodcartel*", "username or pattern",
                    settings.ignoredPlayerPatterns(), settings::saveIgnoredPlayerPatterns));
        }
        if (settings.consumeTrackedFactionsEditorRequest()) {
            client.setScreen(new PlayerListEditorScreen(new LiteralText("Tracked Factions"),
                    "Use * as a wildcard, e.g. Ikea*", "faction name or pattern",
                    settings.trackedFactionPatterns(), settings::saveTrackedFactionPatterns));
        }
        if (settings.consumeIgnoredFactionsEditorRequest()) {
            client.setScreen(new PlayerListEditorScreen(new LiteralText("Ignored Factions"),
                    "Use * as a wildcard, e.g. Ikea*", "faction name or pattern",
                    settings.ignoredFactionPatterns(), settings::saveIgnoredFactionPatterns));
        }

        // Runs unconditionally so anything on screen still fades out when tracking is inactive.
        notifications.update(now);

        if (!hudVisible) {
            // Deliberately does not reset the tracker: resuming compares fresh tab-list state
            // against whatever was last known, so a tracked player who is still present when the
            // HUD is shown again is correctly detected as an arrival rather than silently missed.
            return;
        }

        if (!settings.modEnabled()) {
            stopTracking();
            return;
        }

        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null || client.player == null) {
            stopTracking();
            return;
        }

        tracking = true;
        List<ObservedPlayer> observed = scanner.scan(networkHandler, client.player.getUuid());
        ShardUpdate update = tracker.update(observed, now);

        if (update.hasNotifications()) {
            raiseArrivalNotifications(update.enteredTracked(), now);
        }
        if (update.membershipChanged() && settings.debugLogging()) {
            logMembershipChange(networkHandler);
        }
    }

    /** Both keybindings default to unbound and are configured from the vanilla Controls screen. */
    private void handleKeyBindings() {
        while (PalantirKeyBindings.TOGGLE_HUD.wasPressed()) {
            hudVisible = !hudVisible;
            announceVisibility();
        }
        while (PalantirKeyBindings.OPEN_HUD_EDITOR.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new HudEditScreen(settings, tracker.snapshot()));
            }
        }
    }

    private void announceVisibility() {
        if (client.inGameHud == null) {
            return;
        }
        String message = hudVisible ? "Palantir Client HUD shown" : "Palantir Client HUD hidden";
        client.inGameHud.setOverlayMessage(new LiteralText(message), false);
    }

    private void onHudRender(MatrixStack matrices, float tickDelta) {
        if (!settings.modEnabled() || client.player == null) {
            return;
        }
        if (hudVisible) {
            hudRenderer.render(matrices, tracker.snapshot(), settings.hud());
        }
        notificationRenderer.render(matrices, notifications, settings.notifications(),
                System.currentTimeMillis());
    }

    private void raiseArrivalNotifications(List<ShardUpdate.TrackedArrival> arrivals, long nowMillis) {
        for (int i = 0; i < arrivals.size(); i++) {
            ShardUpdate.TrackedArrival arrival = arrivals.get(i);
            notifications.push(new ShardNotification(NOTIFICATION_TITLE, arrivalMessage(arrival)), nowMillis);
            webhookNotifier.notifyArrival(settings.webhook(), arrival.displayName(), arrival.faction(), nowMillis);
        }
        playAlert();
    }

    private static String arrivalMessage(ShardUpdate.TrackedArrival arrival) {
        if (arrival.faction().isEmpty()) {
            return arrival.displayName() + ENTERED_SUFFIX;
        }
        return arrival.displayName() + " [" + arrival.faction() + "]" + ENTERED_SUFFIX;
    }

    private void showTestNotification(long nowMillis) {
        notifications.push(
                new ShardNotification(NOTIFICATION_TITLE, SAMPLE_PLAYER + ENTERED_SUFFIX), nowMillis);
        playAlert();
    }

    /** One sound per batch, so a burst of arrivals cannot stack into a noise. */
    private void playAlert() {
        var options = settings.notifications();
        if (options.enabled() && options.playSound()) {
            sound.play(options.soundVolume());
        }
    }

    private void onConfigChanged() {
        tracker.configure(settings.tracker());
        notifications.configure(settings.notifications());
        hudRenderer.invalidate();
        notificationRenderer.clearCaches();
    }

    private void onDisconnect() {
        resetTracking();
        notifications.clear();
    }

    private void stopTracking() {
        if (tracking) {
            resetTracking();
        }
    }

    private void resetTracking() {
        tracker.reset();
        scanner.reset();
        tracking = false;
    }

    private void logMembershipChange(ClientPlayNetworkHandler networkHandler) {
        logger.info("Shard membership changed: {} players, {} tracked",
                tracker.snapshot().totalCount(), tracker.snapshot().trackedCount());
        if (settings.showTeamNames()) {
            logger.info("Scoreboard teams: {}", scanner.describeTeams(networkHandler));
        }
    }
}
