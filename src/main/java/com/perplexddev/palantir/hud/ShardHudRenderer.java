package com.perplexddev.palantir.hud;

import com.perplexddev.palantir.tracker.ShardSnapshot;
import com.perplexddev.palantir.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Draws the shard HUD.
 *
 * <p>The panel is rebuilt only when the snapshot or the options instance changes; both are
 * immutable and replaced wholesale, so identity comparison is a sufficient cache key. Per-frame
 * work is limited to two placement calculations and the draw calls themselves.
 */
public final class ShardHudRenderer {

    private final MinecraftClient client;
    private final TextMeasurer measurer;

    private ShardSnapshot cachedSnapshot;
    private HudOptions cachedOptions;
    private HudPanel panel = HudPanel.EMPTY;

    public ShardHudRenderer(MinecraftClient client) {
        this.client = client;
        this.measurer = text -> client.textRenderer.getWidth(text);
    }

    public void render(MatrixStack matrices, ShardSnapshot snapshot, HudOptions options) {
        if (!options.enabled()) {
            return;
        }
        if (options.hideWhenEmpty() && snapshot.isEmpty()) {
            return;
        }
        if (options.hideWithDebugScreen() && client.options.debugEnabled) {
            return;
        }

        rebuildIfStale(snapshot, options);
        if (panel.isEmpty()) {
            return;
        }

        float scaleX = options.scaleX();
        float scaleY = options.scaleY();
        int screenWidth = Math.round(client.getWindow().getScaledWidth() / scaleX);
        int screenHeight = Math.round(client.getWindow().getScaledHeight() / scaleY);

        int x = HudLayout.x(options.anchor(), options.offsetX(), panel.width(), screenWidth);
        int y = HudLayout.y(options.anchor(), options.offsetY(), panel.height(), screenHeight);

        matrices.push();
        matrices.scale(scaleX, scaleY, 1.0f);

        RenderUtil.panel(matrices, x, y, panel.width(), panel.height(),
                options.backgroundColor(), options.borderColor(), options.roundedCorners());

        TextRenderer textRenderer = client.textRenderer;
        int textX = x + HudPanelBuilder.HORIZONTAL_PADDING;
        int textY = y + HudPanelBuilder.VERTICAL_PADDING;
        for (HudRow row : panel.rows()) {
            if (!row.text().isEmpty()) {
                textRenderer.drawWithShadow(matrices, row.text(), textX, textY, row.color());
            }
            textY += HudPanelBuilder.LINE_HEIGHT;
        }

        matrices.pop();
    }

    /** Forces a rebuild, used when the configuration changes. */
    public void invalidate() {
        cachedSnapshot = null;
        cachedOptions = null;
    }

    private void rebuildIfStale(ShardSnapshot snapshot, HudOptions options) {
        if (snapshot == cachedSnapshot && options == cachedOptions) {
            return;
        }
        panel = HudPanelBuilder.build(snapshot, options, measurer);
        cachedSnapshot = snapshot;
        cachedOptions = options;
    }
}
