package com.perplexddev.palantir.color;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorWheelMathTest {

    private static final int CENTER_X = 100;
    private static final int CENTER_Y = 100;
    private static final int RADIUS = 50;
    private static final float VALUE = 0.8f;

    private static int rgbOf(int argb) {
        return argb & 0xFFFFFF;
    }

    private static int alphaOf(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    @Test
    void centerPixelHasZeroSaturationRegardlessOfHue() {
        int pixel = ColorWheelMath.wheelPixelArgb(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y, RADIUS, VALUE);

        assertEquals(rgbOf(Color.HSBtoRGB(0f, 0f, VALUE)), rgbOf(pixel));
        assertEquals(0xFF, alphaOf(pixel));
    }

    @Test
    void rightEdgePixelIsPureRedAtFullSaturation() {
        int pixel = ColorWheelMath.wheelPixelArgb(CENTER_X + RADIUS, CENTER_Y, CENTER_X, CENTER_Y, RADIUS, VALUE);

        assertEquals(rgbOf(Color.HSBtoRGB(0f, 1f, VALUE)), rgbOf(pixel));
    }

    @Test
    void leftEdgePixelIsCyanAtFullSaturation() {
        int pixel = ColorWheelMath.wheelPixelArgb(CENTER_X - RADIUS, CENTER_Y, CENTER_X, CENTER_Y, RADIUS, VALUE);

        assertEquals(rgbOf(Color.HSBtoRGB(0.5f, 1f, VALUE)), rgbOf(pixel));
    }

    @Test
    void topEdgePixelHasHueOf90Degrees() {
        int pixel = ColorWheelMath.wheelPixelArgb(CENTER_X, CENTER_Y - RADIUS, CENTER_X, CENTER_Y, RADIUS, VALUE);

        assertEquals(rgbOf(Color.HSBtoRGB(90f / 360f, 1f, VALUE)), rgbOf(pixel));
    }

    @Test
    void bottomEdgePixelHasHueOf270Degrees() {
        int pixel = ColorWheelMath.wheelPixelArgb(CENTER_X, CENTER_Y + RADIUS, CENTER_X, CENTER_Y, RADIUS, VALUE);

        assertEquals(rgbOf(Color.HSBtoRGB(270f / 360f, 1f, VALUE)), rgbOf(pixel));
    }

    @Test
    void pixelOutsideRadiusIsFullyTransparent() {
        int pixel = ColorWheelMath.wheelPixelArgb(
                CENTER_X + RADIUS + 5, CENTER_Y, CENTER_X, CENTER_Y, RADIUS, VALUE);

        assertEquals(0, alphaOf(pixel));
    }

    @Test
    void pixelExactlyOnTheRadiusBoundaryIsOpaque() {
        int pixel = ColorWheelMath.wheelPixelArgb(CENTER_X + RADIUS, CENTER_Y, CENTER_X, CENTER_Y, RADIUS, VALUE);

        assertEquals(0xFF, alphaOf(pixel));
    }

    @Test
    void hueSaturationAtCenterIsZeroSaturation() {
        ColorWheelMath.HueSaturation result =
                ColorWheelMath.hueSaturationFromPoint(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y, RADIUS);

        assertEquals(0f, result.saturation(), 0.001f);
    }

    @Test
    void hueSaturationClampsSaturationBeyondTheRadius() {
        ColorWheelMath.HueSaturation result = ColorWheelMath.hueSaturationFromPoint(
                CENTER_X + RADIUS * 4L, CENTER_Y, CENTER_X, CENTER_Y, RADIUS);

        assertEquals(1.0f, result.saturation(), 0.001f);
        assertEquals(0f, result.hue(), 0.001f);
    }

    @Test
    void hueSaturationRoundTripsForSampleAnglesAndMagnitudes() {
        float[] hues = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};
        float[] saturations = {0.25f, 0.5f, 1.0f};

        for (float hue : hues) {
            for (float saturation : saturations) {
                double radians = Math.toRadians(hue);
                double x = CENTER_X + RADIUS * saturation * Math.cos(radians);
                double y = CENTER_Y - RADIUS * saturation * Math.sin(radians);

                ColorWheelMath.HueSaturation result =
                        ColorWheelMath.hueSaturationFromPoint(x, y, CENTER_X, CENTER_Y, RADIUS);

                assertEquals(hue, result.hue(), 0.5f, "hue mismatch at hue=" + hue + " sat=" + saturation);
                assertEquals(saturation, result.saturation(), 0.01f, "sat mismatch at hue=" + hue + " sat=" + saturation);
            }
        }
    }

    @Test
    void toArgbAndDecomposeRoundTripForPureRed() {
        int argb = ColorWheelMath.toArgb(0f, 1f, 1f, 255);

        assertEquals(0f, ColorWheelMath.hueOf(argb), 0.5f);
        assertEquals(1f, ColorWheelMath.saturationOf(argb), 0.01f);
        assertEquals(1f, ColorWheelMath.valueOf(argb), 0.01f);
        assertEquals(255, ColorWheelMath.alphaOf(argb));
    }

    @Test
    void toArgbAndDecomposeRoundTripForHalfAlphaGray() {
        int argb = ColorWheelMath.toArgb(210f, 0f, 0.5f, 128);

        assertEquals(0f, ColorWheelMath.saturationOf(argb), 0.01f);
        assertEquals(0.5f, ColorWheelMath.valueOf(argb), 0.01f);
        assertEquals(128, ColorWheelMath.alphaOf(argb));
    }

    @Test
    void toArgbPreservesTheExactAlphaByte() {
        int argb = ColorWheelMath.toArgb(120f, 0.6f, 0.7f, 200);

        assertEquals(200, ColorWheelMath.alphaOf(argb));
    }
}
