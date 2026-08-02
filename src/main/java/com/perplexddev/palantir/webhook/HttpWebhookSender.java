package com.perplexddev.palantir.webhook;

import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fire-and-forget HTTP POST of a webhook payload.
 *
 * <p>Never blocks the caller (the client tick thread) and never throws: a malformed URL, a
 * non-http(s) scheme, a connection failure or a non-2xx response are all just logged.
 */
public final class HttpWebhookSender implements WebhookSender {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final Logger logger;

    public HttpWebhookSender(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void send(String url, String jsonBody) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            logger.warn("Webhook URL is not valid: {}", e.getMessage());
            return;
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            logger.warn("Webhook URL must use http or https, got: {}", scheme);
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        logger.warn("Webhook delivery failed: {}", error.toString());
                    } else if (response.statusCode() >= 300) {
                        logger.warn("Webhook responded with status {}", response.statusCode());
                    }
                });
    }
}
