package com.perplexddev.shardtracker.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded, elapsed-time driven notification queue. Contains no Minecraft types.
 *
 * <p>The queue is strictly capped at the configured maximum: pushing past it discards the oldest
 * entry rather than growing. Expiry is driven by wall-clock elapsed time, so animations are
 * unaffected by tick rate.
 */
public final class NotificationManager {

    private final List<ActiveNotification> queue = new ArrayList<>();
    private final List<ActiveNotification> queueView = Collections.unmodifiableList(queue);

    private NotificationOptions options;

    public NotificationManager(NotificationOptions options) {
        this.options = options;
    }

    public void configure(NotificationOptions options) {
        this.options = options;
        trimToMaximum();
    }

    public void push(ShardNotification notification, long nowMillis) {
        if (!options.enabled()) {
            return;
        }
        queue.add(new ActiveNotification(notification, nowMillis));
        trimToMaximum();
    }

    /** Drops notifications whose lifetime has elapsed. Cheap no-op while nothing is on screen. */
    public void update(long nowMillis) {
        if (queue.isEmpty()) {
            return;
        }
        queue.removeIf(active -> nowMillis - active.shownAtMillis() >= options.durationMs());
    }

    /** Oldest first, matching stack order. */
    public List<ActiveNotification> active() {
        return queueView;
    }

    public boolean hasActiveNotifications() {
        return !queue.isEmpty();
    }

    public float opacityOf(ActiveNotification notification, long nowMillis) {
        return NotificationAnimation.opacity(
                nowMillis - notification.shownAtMillis(),
                options.durationMs(),
                options.fadeInMs(),
                options.fadeOutMs());
    }

    public void clear() {
        queue.clear();
    }

    private void trimToMaximum() {
        while (queue.size() > options.maxSimultaneous()) {
            queue.remove(0);
        }
    }
}
