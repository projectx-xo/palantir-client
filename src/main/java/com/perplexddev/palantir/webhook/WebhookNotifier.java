package com.perplexddev.palantir.webhook;

import com.perplexddev.palantir.util.PlayerNameUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Gates and dispatches a tracked-player arrival to the configured webhook, if any.
 *
 * <p>Also absorbs repeat arrivals for the same player within {@link #COOLDOWN_MILLIS}: a server
 * that briefly drops and reassigns scoreboard teams can make the tracker see the same real-world
 * arrival as several distinct ones a few seconds apart, which without this guard would spam the
 * Discord channel with duplicate alerts for what a human would recognise as one event.
 */
public final class WebhookNotifier {

    static final long COOLDOWN_MILLIS = 5_000;

    private final WebhookSender sender;
    private final Map<String, Long> lastSentAtMillis = new HashMap<>();

    public WebhookNotifier(WebhookSender sender) {
        this.sender = sender;
    }

    public void notifyArrival(WebhookOptions options, String displayName, String faction, long nowMillis) {
        if (!options.enabled() || options.url().isBlank()) {
            return;
        }

        String key = PlayerNameUtil.normalize(displayName);
        Long lastSent = lastSentAtMillis.get(key);
        if (lastSent != null && nowMillis - lastSent < COOLDOWN_MILLIS) {
            return;
        }

        lastSentAtMillis.put(key, nowMillis);
        sender.send(options.url(), WebhookPayload.build(displayName, faction, options.embedColor(), nowMillis));
    }
}
