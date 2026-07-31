package com.perplexddev.shardtracker.util;

/** ARGB helpers. Colours are resolved once on config change, never per frame. */
public final class ColorUtil {

    private static final int ALPHA_SHIFT = 24;
    private static final int BYTE_MASK = 0xFF;
    private static final int RGB_MASK = 0x00FFFFFF;

    private ColorUtil() {
    }

    public static int alpha(int argb) {
        return (argb >>> ALPHA_SHIFT) & BYTE_MASK;
    }

    /** Scales a colour's alpha by a 0..100 percentage. */
    public static int withOpacityPercent(int argb, int opacityPercent) {
        return withAlphaFactor(argb, opacityPercent / 100.0f);
    }

    /** Scales a colour's alpha by a 0..1 factor, used for notification fades. */
    public static int withAlphaFactor(int argb, float factor) {
        int scaled = Math.round(alpha(argb) * clamp01(factor));
        return (scaled << ALPHA_SHIFT) | (argb & RGB_MASK);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
