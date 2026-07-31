package com.perplexddev.shardtracker.notification;

/**
 * Elapsed-time driven fade curve, independent of tick rate and frame rate.
 *
 * <p>Fade-in and fade-out are evaluated as two independent ramps and combined with a minimum, so
 * overlapping fade settings degrade gracefully instead of producing opacities outside 0..1.
 */
public final class NotificationAnimation {

    private NotificationAnimation() {
    }

    public static float opacity(long elapsedMillis, long durationMs, long fadeInMs, long fadeOutMs) {
        if (elapsedMillis < 0 || durationMs <= 0 || elapsedMillis >= durationMs) {
            return 0.0f;
        }

        float fadingIn = fadeInMs > 0 ? (float) elapsedMillis / fadeInMs : 1.0f;
        float fadingOut = fadeOutMs > 0 ? (float) (durationMs - elapsedMillis) / fadeOutMs : 1.0f;

        return clamp01(Math.min(fadingIn, fadingOut));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
