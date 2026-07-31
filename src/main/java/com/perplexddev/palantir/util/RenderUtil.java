package com.perplexddev.palantir.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Panel drawing shared by the HUD and the notification stack.
 *
 * <p>Rounded corners are approximated by insetting the top and bottom rows by one pixel, which
 * costs three quads instead of one and needs no texture or shader. At a 1px radius that inset
 * alone leaves the corner cell untouched by both the background and the border fills, which reads
 * as a transparent hole rather than a curve, so it is filled separately at reduced alpha to read
 * as a soft corner instead.
 */
public final class RenderUtil {

    private static final int CORNER_INSET = 1;
    private static final float CORNER_DOT_ALPHA_FACTOR = 0.5f;

    private RenderUtil() {
    }

    /** Draws a filled panel with a one-pixel border. */
    public static void panel(MatrixStack matrices, int x, int y, int width, int height,
                             int backgroundColor, int borderColor, boolean rounded) {
        RenderSystem.enableBlend();
        fillRect(matrices, x, y, width, height, backgroundColor, rounded);
        drawBorder(matrices, x, y, width, height, borderColor, rounded);
        RenderSystem.disableBlend();
    }

    /** Draws a filled rectangle, optionally with the corner pixels trimmed. */
    public static void fillRect(MatrixStack matrices, int x, int y, int width, int height,
                                int color, boolean rounded) {
        if (ColorUtil.alpha(color) == 0 || width <= 0 || height <= 0) {
            return;
        }

        if (!rounded || width <= CORNER_INSET * 2 || height <= CORNER_INSET * 2) {
            DrawableHelper.fill(matrices, x, y, x + width, y + height, color);
            return;
        }

        DrawableHelper.fill(matrices, x + CORNER_INSET, y, x + width - CORNER_INSET, y + height, color);
        DrawableHelper.fill(matrices, x, y + CORNER_INSET, x + CORNER_INSET, y + height - CORNER_INSET, color);
        DrawableHelper.fill(matrices, x + width - CORNER_INSET, y + CORNER_INSET,
                x + width, y + height - CORNER_INSET, color);
        fillCorners(matrices, x, y, width, height, ColorUtil.withAlphaFactor(color, CORNER_DOT_ALPHA_FACTOR));
    }

    /** Softens the four corner cells the inset fills above deliberately leave untouched. */
    private static void fillCorners(MatrixStack matrices, int x, int y, int width, int height, int color) {
        DrawableHelper.fill(matrices, x, y, x + 1, y + 1, color);
        DrawableHelper.fill(matrices, x + width - 1, y, x + width, y + 1, color);
        DrawableHelper.fill(matrices, x, y + height - 1, x + 1, y + height, color);
        DrawableHelper.fill(matrices, x + width - 1, y + height - 1, x + width, y + height, color);
    }

    private static void drawBorder(MatrixStack matrices, int x, int y, int width, int height,
                                   int color, boolean rounded) {
        if (ColorUtil.alpha(color) == 0 || width <= 0 || height <= 0) {
            return;
        }

        int inset = rounded ? CORNER_INSET : 0;
        int right = x + width;
        int bottom = y + height;

        DrawableHelper.fill(matrices, x + inset, y, right - inset, y + 1, color);
        DrawableHelper.fill(matrices, x + inset, bottom - 1, right - inset, bottom, color);
        DrawableHelper.fill(matrices, x, y + inset, x + 1, bottom - inset, color);
        DrawableHelper.fill(matrices, right - 1, y + inset, right, bottom - inset, color);

        if (rounded) {
            fillCorners(matrices, x, y, width, height, ColorUtil.withAlphaFactor(color, CORNER_DOT_ALPHA_FACTOR));
        }
    }
}
