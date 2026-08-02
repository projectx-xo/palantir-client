package com.perplexddev.palantir.webhook;

/** Delivers an already-built JSON body to a URL. Implementations must never block or throw. */
public interface WebhookSender {

    void send(String url, String jsonBody);
}
