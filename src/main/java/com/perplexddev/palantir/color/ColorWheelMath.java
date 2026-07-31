package com.perplexddev.palantir.color;

import java.awt.Color;

/**
 * Pure hue/saturation/value/alpha math for the interactive color wheel.
 *
 * <p>The wheel encodes hue as the angle around its centre (0° pointing right, increasing
 * counter-clockwise as drawn on screen) and saturation as the fraction of the radius; value and
 * alpha are controlled separately by sliders. {@link Color#HSBtoRGB} and {@link Color#RGBtoHSB}
 * do the actual colour-space conversion; everything here is the wheel's own geometry.
 */
public final class ColorWheelMath {

    private ColorWheelMath() {
    }

    public record HueSaturation(float hue, float saturation) {
    }

    /** ARGB for one wheel pixel; fully transparent outside the circle, opaque on and inside it. */
    public static int wheelPixelArgb(int x, int y, int centerX, int centerY, int radius, float value) {
        double dx = x - centerX;
        double dy = y - centerY;
        double distance = Math.hypot(dx, dy);
        if (distance > radius) {
            return 0;
        }

        HueSaturation hueSaturation = hueSaturationFromPoint(x, y, centerX, centerY, radius);
        int rgb = Color.HSBtoRGB(hueSaturation.hue() / 360f, hueSaturation.saturation(), value);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** Converts a point relative to the wheel's centre into hue (degrees) and clamped saturation. */
    public static HueSaturation hueSaturationFromPoint(double x, double y, double centerX, double centerY,
                                                        double radius) {
        double dx = x - centerX;
        double dy = y - centerY;
        double angle = Math.toDegrees(Math.atan2(-dy, dx));
        float hue = (float) ((angle + 360.0) % 360.0);
        float saturation = radius <= 0 ? 0f : (float) Math.min(1.0, Math.hypot(dx, dy) / radius);
        return new HueSaturation(hue, saturation);
    }

    public static int toArgb(float hue, float saturation, float value, int alpha) {
        int rgb = Color.HSBtoRGB(hue / 360f, saturation, value);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static float hueOf(int argb) {
        return hsb(argb)[0] * 360f;
    }

    public static float saturationOf(int argb) {
        return hsb(argb)[1];
    }

    public static float valueOf(int argb) {
        return hsb(argb)[2];
    }

    public static int alphaOf(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static float[] hsb(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return Color.RGBtoHSB(r, g, b, null);
    }
}
