package com.perplexddev.shardtracker.hud;

import com.perplexddev.shardtracker.config.Settings;
import com.perplexddev.shardtracker.tracker.ShardPlayerState;
import com.perplexddev.shardtracker.tracker.ShardSnapshot;
import com.perplexddev.shardtracker.util.RenderUtil;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.List;

/**
 * Interactive move/resize mode for the HUD panel.
 *
 * <p>Dragging the panel body repositions it. Width and height resize independently: the bar on
 * the right edge drags horizontal scale only, the bar on the bottom edge drags vertical scale
 * only. Both happen directly in real screen pixels so the panel tracks the cursor exactly
 * regardless of scale. On close, the final position is resolved back to whichever anchor keeps
 * the panel closest to where it was dropped, via {@link HudPlacement}, and persisted through
 * {@link Settings}.
 *
 * <p>Shows the live snapshot when there is one, so you can position the HUD while looking at your
 * actual current player list rather than a guess. Falls back to a fixed sample of players when
 * nothing is currently detected (not connected, or an empty shard), so the editor still has
 * something to show. Either way, the panel's size comes entirely from {@link HudPanelBuilder} with
 * no minimum width or height imposed here: a long real list is simply as tall as it needs to be,
 * and stretching one axis far more than the other is allowed to look as odd as you make it.
 */
public final class HudEditScreen extends Screen {

    private static final ShardSnapshot FALLBACK_SNAPSHOT = new ShardSnapshot(
            List.of(
                    new ShardPlayerState("PlayerOne", "Ikea", true),
                    new ShardPlayerState("FactionLeader", "", true),
                    new ShardPlayerState("NormalPlayer", "", false)),
            2);

    private static final int HANDLE_THICKNESS = 4;
    private static final int HANDLE_LENGTH = 16;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;
    private static final int HANDLE_COLOR = 0xFFFFC44D;
    private static final String HINT =
            "Drag to move · right edge resizes width · bottom edge resizes height · Esc to save and close";

    private enum Drag {NONE, MOVING, RESIZING_WIDTH, RESIZING_HEIGHT}

    private final Settings settings;
    private final ShardSnapshot displaySnapshot;

    private HudPanel panel;
    private float scaleX;
    private float scaleY;
    private double panelX;
    private double panelY;

    private Drag drag = Drag.NONE;
    private double dragStartDistanceX;
    private float dragStartScaleX;
    private double dragStartDistanceY;
    private float dragStartScaleY;

    public HudEditScreen(Settings settings, ShardSnapshot liveSnapshot) {
        super(new LiteralText("Edit Shard Tracker HUD"));
        this.settings = settings;
        this.displaySnapshot = liveSnapshot.isEmpty() ? FALLBACK_SNAPSHOT : liveSnapshot;
    }

    @Override
    protected void init() {
        HudOptions options = settings.hud();
        TextMeasurer measurer = text -> textRenderer.getWidth(text);

        this.scaleX = options.scaleX();
        this.scaleY = options.scaleY();
        this.panel = HudPanelBuilder.build(displaySnapshot, options, measurer);

        int logicalScreenWidth = Math.round(width / scaleX);
        int logicalScreenHeight = Math.round(height / scaleY);
        int logicalX = HudLayout.x(options.anchor(), options.offsetX(), panel.width(), logicalScreenWidth);
        int logicalY = HudLayout.y(options.anchor(), options.offsetY(), panel.height(), logicalScreenHeight);
        this.panelX = logicalX * (double) scaleX;
        this.panelY = logicalY * (double) scaleY;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float tickDelta) {
        renderBackground(matrices);

        int x = Math.round((float) panelX);
        int y = Math.round((float) panelY);
        int realWidth = Math.round(panel.width() * scaleX);
        int realHeight = Math.round(panel.height() * scaleY);

        HudOptions options = settings.hud();
        matrices.push();
        matrices.translate(panelX, panelY, 0);
        matrices.scale(scaleX, scaleY, 1.0f);
        RenderUtil.panel(matrices, 0, 0, panel.width(), panel.height(),
                options.backgroundColor(), options.borderColor(), options.roundedCorners());
        int textY = HudPanelBuilder.VERTICAL_PADDING;
        for (HudRow row : panel.rows()) {
            if (!row.text().isEmpty()) {
                textRenderer.drawWithShadow(matrices, row.text(), HudPanelBuilder.HORIZONTAL_PADDING, textY, row.color());
            }
            textY += HudPanelBuilder.LINE_HEIGHT;
        }
        matrices.pop();

        drawSelectionChrome(matrices, x, y, realWidth, realHeight);
        DrawableHelper.drawCenteredText(matrices, textRenderer, HINT, width / 2, height - 16, HIGHLIGHT_COLOR);

        super.render(matrices, mouseX, mouseY, tickDelta);
    }

    private void drawSelectionChrome(MatrixStack matrices, int x, int y, int realWidth, int realHeight) {
        RenderUtil.fillRect(matrices, x - 1, y - 1, realWidth + 2, 1, HIGHLIGHT_COLOR, false);
        RenderUtil.fillRect(matrices, x - 1, y + realHeight, realWidth + 2, 1, HIGHLIGHT_COLOR, false);
        RenderUtil.fillRect(matrices, x - 1, y - 1, 1, realHeight + 2, HIGHLIGHT_COLOR, false);
        RenderUtil.fillRect(matrices, x + realWidth, y - 1, 1, realHeight + 2, HIGHLIGHT_COLOR, false);

        Bounds widthHandle = widthHandleBounds(x, y, realWidth, realHeight);
        Bounds heightHandle = heightHandleBounds(x, y, realWidth, realHeight);
        RenderUtil.fillRect(matrices, widthHandle.x(), widthHandle.y(), widthHandle.width(), widthHandle.height(),
                HANDLE_COLOR, false);
        RenderUtil.fillRect(matrices, heightHandle.x(), heightHandle.y(), heightHandle.width(), heightHandle.height(),
                HANDLE_COLOR, false);
    }

    /** Thin vertical bar centred on the right edge; dragging it changes width only. */
    private static Bounds widthHandleBounds(int x, int y, int realWidth, int realHeight) {
        return new Bounds(x + realWidth - HANDLE_THICKNESS, y + realHeight / 2 - HANDLE_LENGTH / 2,
                HANDLE_THICKNESS, HANDLE_LENGTH);
    }

    /** Thin horizontal bar centred on the bottom edge; dragging it changes height only. */
    private static Bounds heightHandleBounds(int x, int y, int realWidth, int realHeight) {
        return new Bounds(x + realWidth / 2 - HANDLE_LENGTH / 2, y + realHeight - HANDLE_THICKNESS,
                HANDLE_LENGTH, HANDLE_THICKNESS);
    }

    private record Bounds(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int x = Math.round((float) panelX);
        int y = Math.round((float) panelY);
        int realWidth = Math.round(panel.width() * scaleX);
        int realHeight = Math.round(panel.height() * scaleY);

        if (widthHandleBounds(x, y, realWidth, realHeight).contains(mouseX, mouseY)) {
            drag = Drag.RESIZING_WIDTH;
            dragStartDistanceX = mouseX - panelX;
            dragStartScaleX = scaleX;
            return true;
        }
        if (heightHandleBounds(x, y, realWidth, realHeight).contains(mouseX, mouseY)) {
            drag = Drag.RESIZING_HEIGHT;
            dragStartDistanceY = mouseY - panelY;
            dragStartScaleY = scaleY;
            return true;
        }
        if (mouseX >= panelX && mouseX <= panelX + realWidth && mouseY >= panelY && mouseY <= panelY + realHeight) {
            drag = Drag.MOVING;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        switch (drag) {
            case MOVING -> {
                panelX += deltaX;
                panelY += deltaY;
                return true;
            }
            case RESIZING_WIDTH -> {
                double currentDistance = mouseX - panelX;
                scaleX = HudResizeMath.resize(dragStartScaleX, dragStartDistanceX, currentDistance);
                return true;
            }
            case RESIZING_HEIGHT -> {
                double currentDistance = mouseY - panelY;
                scaleY = HudResizeMath.resize(dragStartScaleY, dragStartDistanceY, currentDistance);
                return true;
            }
            default -> {
                return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        drag = Drag.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        int scaleXPercent = Math.round(scaleX * 100.0f);
        int scaleYPercent = Math.round(scaleY * 100.0f);
        int logicalScreenWidth = Math.round(width / scaleX);
        int logicalScreenHeight = Math.round(height / scaleY);
        int logicalX = Math.round((float) (panelX / scaleX));
        int logicalY = Math.round((float) (panelY / scaleY));

        HudPlacement.Resolved resolved = HudPlacement.resolve(
                logicalX, logicalY, panel.width(), panel.height(), logicalScreenWidth, logicalScreenHeight);

        settings.saveHudPlacement(resolved.anchor(), resolved.offsetX(), resolved.offsetY(),
                scaleXPercent, scaleYPercent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
