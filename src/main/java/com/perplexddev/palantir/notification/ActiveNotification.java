package com.perplexddev.palantir.notification;

/** A notification currently on screen, with the wall-clock time it was raised. */
public record ActiveNotification(ShardNotification notification, long shownAtMillis) {
}
