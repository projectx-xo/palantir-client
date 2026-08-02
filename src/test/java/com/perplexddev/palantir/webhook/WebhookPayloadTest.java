package com.perplexddev.palantir.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookPayloadTest {

    private static final long NOW_MILLIS = 1_700_000_000_000L;
    private static final int COLOR = 0x2DD4BF;

    @Test
    void titleIsTrackedPlayerDetected() {
        JsonObject embed = embedOf(WebhookPayload.build("PlayerOne", "", COLOR, NOW_MILLIS));

        assertEquals("Tracked player detected", embed.get("title").getAsString());
    }

    @Test
    void descriptionBoldsTheNameAndOmitsFactionWhenEmpty() {
        JsonObject embed = embedOf(WebhookPayload.build("PlayerOne", "", COLOR, NOW_MILLIS));

        assertEquals("**PlayerOne** entered the shard", embed.get("description").getAsString());
    }

    @Test
    void descriptionIncludesFactionWhenPresent() {
        JsonObject embed = embedOf(WebhookPayload.build("PlayerOne", "Ikea", COLOR, NOW_MILLIS));

        assertEquals("**PlayerOne** `[Ikea]` entered the shard", embed.get("description").getAsString());
    }

    @Test
    void usesTheConfiguredEmbedColor() {
        JsonObject embed = embedOf(WebhookPayload.build("PlayerOne", "", 0x123456, NOW_MILLIS));

        assertEquals(0x123456, embed.get("color").getAsInt());
    }

    @Test
    void footerShowsPalantirBrandingWithIconUrl() {
        JsonObject embed = embedOf(WebhookPayload.build("PlayerOne", "", COLOR, NOW_MILLIS));

        JsonObject footer = embed.getAsJsonObject("footer");
        assertEquals("Palantir Client", footer.get("text").getAsString());
        assertEquals(WebhookPayload.ICON_URL, footer.get("icon_url").getAsString());
    }

    @Test
    void includesTimestampMatchingSuppliedInstant() {
        JsonObject embed = embedOf(WebhookPayload.build("PlayerOne", "", COLOR, NOW_MILLIS));

        assertEquals(Instant.ofEpochMilli(NOW_MILLIS).toString(), embed.get("timestamp").getAsString());
    }

    @Test
    void alwaysSuppressesMentionParsingRegardlessOfContent() {
        JsonObject payload = parse(WebhookPayload.build("@everyone", "@here", COLOR, NOW_MILLIS));

        JsonArray parse = payload.getAsJsonObject("allowed_mentions").getAsJsonArray("parse");
        assertTrue(parse.isEmpty());
    }

    @Test
    void producesValidJsonWhenNameContainsQuotesAndBackslashes() {
        String maliciousName = "He said \"hi\" \\ everyone";

        JsonObject embed = embedOf(WebhookPayload.build(maliciousName, "", COLOR, NOW_MILLIS));

        assertEquals("**" + maliciousName + "** entered the shard", embed.get("description").getAsString());
    }

    @Test
    void keepsDiscordMentionSyntaxAsLiteralTextRatherThanBreakingStructure() {
        String mentionLike = "@everyone <@&123456789012345678>";

        JsonObject embed = embedOf(WebhookPayload.build(mentionLike, "", COLOR, NOW_MILLIS));

        assertEquals("**" + mentionLike + "** entered the shard", embed.get("description").getAsString());
    }

    @Test
    void stripsControlCharactersFromName() {
        JsonObject embed = embedOf(WebhookPayload.build("Bad\nNameHere", "", COLOR, NOW_MILLIS));

        assertEquals("**BadNameHere** entered the shard", embed.get("description").getAsString());
    }

    @Test
    void truncatesExcessivelyLongNames() {
        String longName = "a".repeat(500);

        JsonObject embed = embedOf(WebhookPayload.build(longName, "", COLOR, NOW_MILLIS));

        assertTrue(embed.get("description").getAsString().length() < 300);
    }

    @Test
    void fallsBackToPlaceholderWhenNameSanitizesToEmpty() {
        JsonObject embed = embedOf(WebhookPayload.build("", "", COLOR, NOW_MILLIS));

        assertEquals("**Unknown** entered the shard", embed.get("description").getAsString());
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject embedOf(String json) {
        return parse(json).getAsJsonArray("embeds").get(0).getAsJsonObject();
    }
}
