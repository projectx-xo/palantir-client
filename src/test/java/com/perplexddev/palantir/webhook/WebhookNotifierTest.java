package com.perplexddev.palantir.webhook;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookNotifierTest {

    private static final int COLOR = 0x2DD4BF;
    private static final long T0 = 1_700_000_000_000L;

    private record SentCall(String url, String body) {
    }

    private final List<SentCall> sent = new ArrayList<>();
    private final WebhookNotifier notifier = new WebhookNotifier((url, body) -> sent.add(new SentCall(url, body)));

    @Test
    void sendsPayloadWhenEnabledWithUrl() {
        notifier.notifyArrival(new WebhookOptions(true, "https://example.com/hook", COLOR), "PlayerOne", "Ikea", T0);

        assertEquals(1, sent.size());
        assertEquals("https://example.com/hook", sent.get(0).url());
        assertTrue(sent.get(0).body().contains("PlayerOne"));
    }

    @Test
    void doesNothingWhenDisabled() {
        notifier.notifyArrival(new WebhookOptions(false, "https://example.com/hook", COLOR), "PlayerOne", "", T0);

        assertTrue(sent.isEmpty());
    }

    @Test
    void doesNothingWhenUrlIsBlank() {
        notifier.notifyArrival(new WebhookOptions(true, "   ", COLOR), "PlayerOne", "", T0);

        assertTrue(sent.isEmpty());
    }

    @Test
    void payloadUsesTheConfiguredEmbedColor() {
        notifier.notifyArrival(new WebhookOptions(true, "https://example.com/hook", 0x123456), "PlayerOne", "", T0);

        assertTrue(sent.get(0).body().contains("\"color\":1193046"));
    }

    @Test
    void suppressesARepeatArrivalForTheSamePlayerWithinTheCooldownWindow() {
        WebhookOptions options = new WebhookOptions(true, "https://example.com/hook", COLOR);

        notifier.notifyArrival(options, "PlayerOne", "", T0);
        notifier.notifyArrival(options, "PlayerOne", "", T0 + 2_000);

        assertEquals(1, sent.size());
    }

    @Test
    void repeatSuppressionIsCaseInsensitive() {
        WebhookOptions options = new WebhookOptions(true, "https://example.com/hook", COLOR);

        notifier.notifyArrival(options, "PlayerOne", "", T0);
        notifier.notifyArrival(options, "PLAYERONE", "", T0 + 2_000);

        assertEquals(1, sent.size());
    }

    @Test
    void allowsAnotherArrivalForTheSamePlayerAfterTheCooldownElapses() {
        WebhookOptions options = new WebhookOptions(true, "https://example.com/hook", COLOR);

        notifier.notifyArrival(options, "PlayerOne", "", T0);
        notifier.notifyArrival(options, "PlayerOne", "", T0 + WebhookNotifier.COOLDOWN_MILLIS);

        assertEquals(2, sent.size());
    }

    @Test
    void doesNotSuppressADifferentPlayerWithinTheCooldownWindow() {
        WebhookOptions options = new WebhookOptions(true, "https://example.com/hook", COLOR);

        notifier.notifyArrival(options, "PlayerOne", "", T0);
        notifier.notifyArrival(options, "PlayerTwo", "", T0 + 5_000);

        assertEquals(2, sent.size());
    }
}
