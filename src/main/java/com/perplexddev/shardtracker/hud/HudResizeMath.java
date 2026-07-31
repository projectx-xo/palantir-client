package com.perplexddev.shardtracker.hud;

/**
 * Pure scale math for the interactive HUD resize handle.
 *
 * <p>The handle drag is expressed as a ratio of the cursor's distance from the panel's fixed
 * corner, at the current moment versus when the drag started, so the panel grows or shrinks
 * smoothly regardless of where the drag began.
 */
public final class HudResizeMath {

    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 3.0f;

    private HudResizeMath() {
    }

    public static float resize(float startScale, double startDistance, double currentDistance) {
        if (startDistance <= 0.0001) {
            return clamp(startScale);
        }
        float ratio = (float) (currentDistance / startDistance);
        return clamp(startScale * ratio);
    }

    private static float clamp(float scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }
}
