package com.perplexddev.shardtracker.notification;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

/**
 * Plays the built-in alert sound for a notification.
 *
 * <p>Uses a vanilla sound so nothing has to be bundled, and fails silently when the sound manager
 * is not available (title screen, shutdown).
 */
public final class NotificationSound {

    private static final float PITCH = 1.4f;

    private final MinecraftClient client;

    public NotificationSound(MinecraftClient client) {
        this.client = client;
    }

    public void play(float volume) {
        if (volume <= 0.0f) {
            return;
        }
        if (client.getSoundManager() == null) {
            return;
        }
        client.getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL, PITCH, volume));
    }
}
