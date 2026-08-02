package com.perplexddev.palantir.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * Builds the Discord-compatible embed JSON body for a tracked-player arrival.
 *
 * <p>Every string value flows through Gson's own serialization rather than manual concatenation, so
 * a player display name or faction (both attacker-controllable via the tab list/scoreboard) can never
 * break out of the JSON structure. {@code allowed_mentions} is always forced empty as a second layer
 * of defense, on top of embeds never being mention-parsed by Discord in the first place.
 */
public final class WebhookPayload {

    /** Kept on the {@code main} branch regardless of which branch produced the build. */
    public static final String ICON_URL =
            "https://raw.githubusercontent.com/projectx-xo/palantir-client/main/src/main/resources/assets/palantir/icon.png";

    private static final int MAX_NAME_LENGTH = 256;
    private static final String FALLBACK_VALUE = "Unknown";
    private static final String FOOTER_TEXT = "Palantir Client";

    private WebhookPayload() {
    }

    public static String build(String displayName, String faction, int embedColor, long nowMillis) {
        String name = sanitize(displayName);
        String description = faction.isEmpty()
                ? "**" + name + "** entered the shard"
                : "**" + name + "** `[" + sanitize(faction) + "]` entered the shard";

        JsonObject footer = new JsonObject();
        footer.addProperty("text", FOOTER_TEXT);
        footer.addProperty("icon_url", ICON_URL);

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "Tracked player detected");
        embed.addProperty("description", description);
        embed.addProperty("color", embedColor);
        embed.addProperty("timestamp", Instant.ofEpochMilli(nowMillis).toString());
        embed.add("footer", footer);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);

        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());

        JsonObject payload = new JsonObject();
        payload.add("allowed_mentions", allowedMentions);
        payload.add("embeds", embeds);
        return payload.toString();
    }

    /** Strips control characters (a malicious display name has no legitimate use for them) and bounds length. */
    private static String sanitize(String raw) {
        StringBuilder cleaned = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                cleaned.append(c);
            }
        }
        String trimmed = cleaned.toString().trim();
        if (trimmed.isEmpty()) {
            return FALLBACK_VALUE;
        }
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }
}
