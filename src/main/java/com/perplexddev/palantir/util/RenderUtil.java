package com.perplexddev.palantir.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Panel drawing shared by the HUD and the notification stack.
 *
 * <p>{@link net.minecraft.client.gui.DrawContext} isn't available here: both call sites render
 * from Fabric API's {@code HudRenderCallback}/{@code ScreenEvents.afterRender}, which still hand a
 * raw {@link MatrixStack} rather than a {@code DrawContext}. {@link #fill} therefore mirrors
 * {@code DrawContext#fill}'s own implementation directly against that {@code MatrixStack}.
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
    public static void panel(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                             int x, int y, int width, int height,
                             int backgroundColor, int borderColor, boolean rounded) {
        RenderSystem.enableBlend();
        fillRect(matrices, vertexConsumers, x, y, width, height, backgroundColor, rounded);
        drawBorder(matrices, vertexConsumers, x, y, width, height, borderColor, rounded);
        RenderSystem.disableBlend();
    }

    /** Draws a filled rectangle, optionally with the corner pixels trimmed. */
    public static void fillRect(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                                int x, int y, int width, int height, int color, boolean rounded) {
        if (ColorUtil.alpha(color) == 0 || width <= 0 || height <= 0) {
            return;
        }

        if (!rounded || width <= CORNER_INSET * 2 || height <= CORNER_INSET * 2) {
            fill(matrices, vertexConsumers, x, y, x + width, y + height, color);
            return;
        }

        fill(matrices, vertexConsumers, x + CORNER_INSET, y, x + width - CORNER_INSET, y + height, color);
        fill(matrices, vertexConsumers, x, y + CORNER_INSET, x + CORNER_INSET, y + height - CORNER_INSET, color);
        fill(matrices, vertexConsumers, x + width - CORNER_INSET, y + CORNER_INSET,
                x + width, y + height - CORNER_INSET, color);
        fillCorners(matrices, vertexConsumers, x, y, width, height, ColorUtil.withAlphaFactor(color, CORNER_DOT_ALPHA_FACTOR));
    }

    /** Draws shadowed text, mirroring {@code DrawContext#drawTextWithShadow}. */
    public static void drawTextWithShadow(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                                          TextRenderer textRenderer, String text, int x, int y, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        textRenderer.draw(text, x, y, color, true, matrix, vertexConsumers,
                TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0, textRenderer.isRightToLeft());
        vertexConsumers.draw();
    }

    /** Softens the four corner cells the inset fills above deliberately leave untouched. */
    private static void fillCorners(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                                    int x, int y, int width, int height, int color) {
        fill(matrices, vertexConsumers, x, y, x + 1, y + 1, color);
        fill(matrices, vertexConsumers, x + width - 1, y, x + width, y + 1, color);
        fill(matrices, vertexConsumers, x, y + height - 1, x + 1, y + height, color);
        fill(matrices, vertexConsumers, x + width - 1, y + height - 1, x + width, y + height, color);
    }

    private static void drawBorder(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                                   int x, int y, int width, int height, int color, boolean rounded) {
        if (ColorUtil.alpha(color) == 0 || width <= 0 || height <= 0) {
            return;
        }

        int inset = rounded ? CORNER_INSET : 0;
        int right = x + width;
        int bottom = y + height;

        fill(matrices, vertexConsumers, x + inset, y, right - inset, y + 1, color);
        fill(matrices, vertexConsumers, x + inset, bottom - 1, right - inset, bottom, color);
        fill(matrices, vertexConsumers, x, y + inset, x + 1, bottom - inset, color);
        fill(matrices, vertexConsumers, right - 1, y + inset, right, bottom - inset, color);

        if (rounded) {
            fillCorners(matrices, vertexConsumers, x, y, width, height, ColorUtil.withAlphaFactor(color, CORNER_DOT_ALPHA_FACTOR));
        }
    }

    /** A single filled quad, following {@code DrawContext#fill(RenderLayer, int, int, int, int, int, int)}. */
    private static void fill(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                             int x1, int y1, int x2, int y2, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getGui());
        buffer.vertex(matrix, x1, y1, 0).color(color);
        buffer.vertex(matrix, x1, y2, 0).color(color);
        buffer.vertex(matrix, x2, y2, 0).color(color);
        buffer.vertex(matrix, x2, y1, 0).color(color);
        vertexConsumers.draw();
    }
}
