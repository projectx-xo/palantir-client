package com.perplexddev.shardtracker.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationManagerTest {

    private static final long T0 = 10_000L;
    private static final long DURATION = 5000L;

    private static NotificationOptions options(int maxSimultaneous) {
        return new NotificationOptions(true, NotificationCorner.BOTTOM_RIGHT, 8, 8,
                DURATION, 200, 400, true, 0.6f, 1.0f, maxSimultaneous,
                0xE0000000, 0xFFFFC44D, 0xFFFFC44D, 0xFFFFFFFF, true);
    }

    private static NotificationOptions disabled() {
        NotificationOptions base = options(4);
        return new NotificationOptions(false, base.corner(), base.offsetX(), base.offsetY(),
                base.durationMs(), base.fadeInMs(), base.fadeOutMs(), base.playSound(),
                base.soundVolume(), base.scale(), base.maxSimultaneous(), base.backgroundColor(),
                base.borderColor(), base.titleColor(), base.bodyColor(), base.roundedCorners());
    }

    private static List<String> bodiesOf(NotificationManager manager) {
        return manager.active().stream().map(active -> active.notification().body()).toList();
    }

    @Test
    void showsAPushedNotification() {
        NotificationManager manager = new NotificationManager(options(4));

        manager.push(new ShardNotification("TRACKED PLAYER DETECTED", "PlayerOne entered the shard"), T0);

        assertEquals(1, manager.active().size());
        assertEquals("PlayerOne entered the shard", manager.active().get(0).notification().body());
    }

    @Test
    void ignoresPushesWhileDisabled() {
        NotificationManager manager = new NotificationManager(disabled());

        manager.push(new ShardNotification("title", "body"), T0);

        assertTrue(manager.active().isEmpty());
    }

    @Test
    void keepsNotificationUntilItsDurationElapses() {
        NotificationManager manager = new NotificationManager(options(4));
        manager.push(new ShardNotification("title", "body"), T0);

        manager.update(T0 + DURATION - 1);

        assertEquals(1, manager.active().size());
    }

    @Test
    void removesNotificationOnceItsDurationHasElapsed() {
        NotificationManager manager = new NotificationManager(options(4));
        manager.push(new ShardNotification("title", "body"), T0);

        manager.update(T0 + DURATION);

        assertTrue(manager.active().isEmpty());
    }

    @Test
    void dropsTheOldestNotificationWhenTheQueueIsFull() {
        NotificationManager manager = new NotificationManager(options(2));

        manager.push(new ShardNotification("title", "first"), T0);
        manager.push(new ShardNotification("title", "second"), T0 + 10);
        manager.push(new ShardNotification("title", "third"), T0 + 20);

        assertEquals(2, manager.active().size());
        assertEquals(List.of("second", "third"), bodiesOf(manager));
    }

    @Test
    void neverExceedsTheConfiguredMaximum() {
        NotificationManager manager = new NotificationManager(options(3));

        for (int i = 0; i < 20; i++) {
            manager.push(new ShardNotification("title", "body " + i), T0 + i);
        }

        assertEquals(3, manager.active().size());
    }

    @Test
    void trimsTheQueueWhenTheMaximumIsLowered() {
        NotificationManager manager = new NotificationManager(options(4));
        manager.push(new ShardNotification("title", "first"), T0);
        manager.push(new ShardNotification("title", "second"), T0 + 10);
        manager.push(new ShardNotification("title", "third"), T0 + 20);

        manager.configure(options(1));

        assertEquals(List.of("third"), bodiesOf(manager));
    }

    @Test
    void clearRemovesEverything() {
        NotificationManager manager = new NotificationManager(options(4));
        manager.push(new ShardNotification("title", "body"), T0);

        manager.clear();

        assertTrue(manager.active().isEmpty());
    }

    @Test
    void expiresOnlyTheNotificationsThatAreDue() {
        NotificationManager manager = new NotificationManager(options(4));
        manager.push(new ShardNotification("title", "old"), T0);
        manager.push(new ShardNotification("title", "new"), T0 + 3000);

        manager.update(T0 + DURATION + 10);

        assertEquals(List.of("new"), bodiesOf(manager));
    }

    @Test
    void reportsWhetherAnythingNeedsRendering() {
        NotificationManager manager = new NotificationManager(options(4));

        assertFalse(manager.hasActiveNotifications());
        manager.push(new ShardNotification("title", "body"), T0);
        assertTrue(manager.hasActiveNotifications());
    }

    @Test
    void exposesOpacityForRendering() {
        NotificationManager manager = new NotificationManager(options(4));
        manager.push(new ShardNotification("title", "body"), T0);

        float atStart = manager.opacityOf(manager.active().get(0), T0);
        float midway = manager.opacityOf(manager.active().get(0), T0 + 2500);

        assertEquals(0.0f, atStart, 0.001f);
        assertEquals(1.0f, midway, 0.001f);
    }
}
