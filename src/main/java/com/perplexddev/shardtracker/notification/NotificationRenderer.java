package com.perplexddev.shardtracker.notification;

import com.perplexddev.shardtracker.util.ColorUtil;
import com.perplexddev.shardtracker.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the notification stack.
 *
 * <p>Text widths are cached per notification content, so repeated arrivals of the same player do no
 * measurement work. Positions come from {@link NotificationLayout}, which keeps every box on screen
 * and stacks them without overlap.
 */
public final class NotificationRenderer {

    private static final int HORIZONTAL_PADDING = 6;
    private static final int VERTICAL_PADDING = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int SPACING = 4;
    private static final int MAX_CACHED_WIDTHS = 32;

    private final MinecraftClient client;
    private final Map<ShardNotification, Integer> widthCache = new HashMap<>();

    public NotificationRenderer(MinecraftClient client) {
        this.client = client;
    }

    public void render(MatrixStack matrices, NotificationManager manager,
                       NotificationOptions options, long nowMillis) {
        List<ActiveNotification> active = manager.active();
        if (active.isEmpty()) {
            return;
        }

        float scale = options.scale();
        int screenWidth = Math.round(client.getWindow().getScaledWidth() / scale);
        int screenHeight = Math.round(client.getWindow().getScaledHeight() / scale);
        int height = LINE_HEIGHT * 2 + VERTICAL_PADDING * 2;

        matrices.push();
        matrices.scale(scale, scale, 1.0f);

        // Newest first, so the most recent notification sits closest to the anchored corner.
        for (int stackIndex = 0; stackIndex < active.size(); stackIndex++) {
            ActiveNotification entry = active.get(active.size() - 1 - stackIndex);
            float opacity = manager.opacityOf(entry, nowMillis);
            if (opacity <= 0.0f) {
                continue;
            }
            drawNotification(matrices, entry, options, opacity, stackIndex,
                    screenWidth, screenHeight, height);
        }

        matrices.pop();
    }

    public void clearCaches() {
        widthCache.clear();
    }

    private void drawNotification(MatrixStack matrices, ActiveNotification entry,
                                  NotificationOptions options, float opacity, int stackIndex,
                                  int screenWidth, int screenHeight, int height) {
        int width = widthOf(entry.notification());
        int x = NotificationLayout.baseX(options.corner(), options.offsetX(), width, screenWidth)
                + NotificationLayout.slideOffset(options.corner(), opacity);
        int y = NotificationLayout.y(options.corner(), options.offsetY(), height, screenHeight,
                stackIndex, SPACING);

        RenderUtil.panel(matrices, x, y, width, height,
                ColorUtil.withAlphaFactor(options.backgroundColor(), opacity),
                ColorUtil.withAlphaFactor(options.borderColor(), opacity),
                options.roundedCorners());

        TextRenderer textRenderer = client.textRenderer;
        int textX = x + HORIZONTAL_PADDING;
        int textY = y + VERTICAL_PADDING;

        textRenderer.drawWithShadow(matrices, entry.notification().title(), textX, textY,
                ColorUtil.withAlphaFactor(options.titleColor(), opacity));
        textRenderer.drawWithShadow(matrices, entry.notification().body(), textX, textY + LINE_HEIGHT,
                ColorUtil.withAlphaFactor(options.bodyColor(), opacity));
    }

    private int widthOf(ShardNotification notification) {
        Integer cached = widthCache.get(notification);
        if (cached != null) {
            return cached;
        }

        TextRenderer textRenderer = client.textRenderer;
        int width = Math.max(textRenderer.getWidth(notification.title()),
                textRenderer.getWidth(notification.body()))
                + HORIZONTAL_PADDING * 2;

        if (widthCache.size() >= MAX_CACHED_WIDTHS) {
            widthCache.clear();
        }
        widthCache.put(notification, width);
        return width;
    }
}
