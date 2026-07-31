package com.perplexddev.shardtracker.color;

import com.perplexddev.shardtracker.config.Settings;
import com.perplexddev.shardtracker.util.RenderUtil;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.EnumMap;
import java.util.Map;

/**
 * Interactive color wheel editor for the mod's nine appearance colors.
 *
 * <p>The left column lists all nine colors as clickable swatches; selecting one loads its hue,
 * saturation, brightness and alpha into the wheel and sliders. Dragging within the wheel sets hue
 * and saturation (angle and radius); the two sliders set brightness and alpha. Changes are held in
 * a working copy and only written back through {@link Settings#saveAppearanceColors} when the
 * screen closes.
 */
public final class ColorWheelScreen extends Screen {

    private static final int WHEEL_RADIUS = 55;
    private static final int WHEEL_CELL_SIZE = 3;
    private static final int SWATCH_SIZE = 16;
    private static final int SWATCH_ROW_WIDTH = 220;
    private static final int ROW_HEIGHT = 22;
    private static final int SWATCH_LIST_X = 20;
    private static final int SWATCH_LIST_Y = 32;
    private static final int SLIDER_WIDTH = 110;
    private static final int SLIDER_HEIGHT = 20;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;
    private static final int OPAQUE_MASK = 0xFF000000;
    private static final String HINT = "Click a color to edit it · Esc to save and close";

    private final Settings settings;
    private final Map<AppearanceColorKey, Integer> workingColors;

    private AppearanceColorKey selected = AppearanceColorKey.HUD_BACKGROUND;
    private float hue;
    private float saturation;
    private float value;
    private int alpha;

    private int wheelCenterX;
    private int wheelCenterY;
    private boolean draggingWheel;

    public ColorWheelScreen(Settings settings) {
        super(new LiteralText("Edit Shard Tracker Colors"));
        this.settings = settings;
        this.workingColors = new EnumMap<>(settings.appearanceColors());
    }

    @Override
    protected void init() {
        wheelCenterX = width - 150;
        wheelCenterY = 110;

        loadSelectedColor();
        addColorSliders();
    }

    private void addColorSliders() {
        addDrawableChild(new ValueSlider(wheelCenterX - SLIDER_WIDTH / 2, wheelCenterY + WHEEL_RADIUS + 18,
                SLIDER_WIDTH, SLIDER_HEIGHT, value));
        addDrawableChild(new AlphaSlider(wheelCenterX - SLIDER_WIDTH / 2, wheelCenterY + WHEEL_RADIUS + 42,
                SLIDER_WIDTH, SLIDER_HEIGHT, alpha / 255.0));
    }

    private void loadSelectedColor() {
        int argb = workingColors.get(selected);
        hue = ColorWheelMath.hueOf(argb);
        saturation = ColorWheelMath.saturationOf(argb);
        value = ColorWheelMath.valueOf(argb);
        alpha = ColorWheelMath.alphaOf(argb);
    }

    private void select(AppearanceColorKey key) {
        if (key == selected) {
            return;
        }
        selected = key;
        loadSelectedColor();
        clearChildren();
        addColorSliders();
    }

    private void onValueSliderChanged(float newValue) {
        value = newValue;
        applyWorkingColor();
    }

    private void onAlphaSliderChanged(int newAlpha) {
        alpha = newAlpha;
        applyWorkingColor();
    }

    private void applyWorkingColor() {
        workingColors.put(selected, ColorWheelMath.toArgb(hue, saturation, value, alpha));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float tickDelta) {
        renderBackground(matrices);

        drawSwatchList(matrices);
        drawWheel(matrices);
        drawHexReadout(matrices);

        super.render(matrices, mouseX, mouseY, tickDelta);

        DrawableHelper.drawCenteredText(matrices, textRenderer, HINT, width / 2, height - 16, HIGHLIGHT_COLOR);
    }

    private void drawSwatchList(MatrixStack matrices) {
        AppearanceColorKey[] keys = AppearanceColorKey.values();
        for (int i = 0; i < keys.length; i++) {
            AppearanceColorKey key = keys[i];
            int rowY = SWATCH_LIST_Y + i * ROW_HEIGHT;
            if (key == selected) {
                RenderUtil.fillRect(matrices, SWATCH_LIST_X - 3, rowY - 3, SWATCH_SIZE + 6, SWATCH_SIZE + 6,
                        HIGHLIGHT_COLOR, false);
            }
            RenderUtil.fillRect(matrices, SWATCH_LIST_X, rowY, SWATCH_SIZE, SWATCH_SIZE,
                    workingColors.get(key) | OPAQUE_MASK, false);
            textRenderer.drawWithShadow(matrices, key.toString(), SWATCH_LIST_X + SWATCH_SIZE + 8,
                    rowY + (SWATCH_SIZE - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
        }
    }

    private void drawWheel(MatrixStack matrices) {
        for (int dx = -WHEEL_RADIUS; dx <= WHEEL_RADIUS; dx += WHEEL_CELL_SIZE) {
            for (int dy = -WHEEL_RADIUS; dy <= WHEEL_RADIUS; dy += WHEEL_CELL_SIZE) {
                int x = wheelCenterX + dx;
                int y = wheelCenterY + dy;
                int color = ColorWheelMath.wheelPixelArgb(x, y, wheelCenterX, wheelCenterY, WHEEL_RADIUS, value);
                if (ColorWheelMath.alphaOf(color) == 0) {
                    continue;
                }
                RenderUtil.fillRect(matrices, x, y, WHEEL_CELL_SIZE, WHEEL_CELL_SIZE, color, false);
            }
        }
        drawWheelCursor(matrices);
    }

    private void drawWheelCursor(MatrixStack matrices) {
        double radians = Math.toRadians(hue);
        int cursorX = wheelCenterX + (int) Math.round(WHEEL_RADIUS * saturation * Math.cos(radians));
        int cursorY = wheelCenterY - (int) Math.round(WHEEL_RADIUS * saturation * Math.sin(radians));
        RenderUtil.fillRect(matrices, cursorX - 3, cursorY - 3, 6, 6, HIGHLIGHT_COLOR, false);
        RenderUtil.fillRect(matrices, cursorX - 2, cursorY - 2, 4, 4, workingColors.get(selected) | OPAQUE_MASK, false);
    }

    private void drawHexReadout(MatrixStack matrices) {
        String hex = String.format("#%08X", workingColors.get(selected));
        DrawableHelper.drawCenteredText(matrices, textRenderer, hex, wheelCenterX,
                wheelCenterY + WHEEL_RADIUS + 66, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            AppearanceColorKey clickedRow = swatchRowAt(mouseX, mouseY);
            if (clickedRow != null) {
                select(clickedRow);
                return true;
            }
            if (withinWheel(mouseX, mouseY)) {
                draggingWheel = true;
                updateHueSaturationFromMouse(mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingWheel) {
            updateHueSaturationFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWheel = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateHueSaturationFromMouse(double mouseX, double mouseY) {
        ColorWheelMath.HueSaturation result =
                ColorWheelMath.hueSaturationFromPoint(mouseX, mouseY, wheelCenterX, wheelCenterY, WHEEL_RADIUS);
        hue = result.hue();
        saturation = result.saturation();
        applyWorkingColor();
    }

    private AppearanceColorKey swatchRowAt(double mouseX, double mouseY) {
        AppearanceColorKey[] keys = AppearanceColorKey.values();
        for (int i = 0; i < keys.length; i++) {
            int rowY = SWATCH_LIST_Y + i * ROW_HEIGHT;
            if (mouseX >= SWATCH_LIST_X - 3 && mouseX <= SWATCH_LIST_X + SWATCH_ROW_WIDTH
                    && mouseY >= rowY - 3 && mouseY <= rowY + SWATCH_SIZE + 3) {
                return keys[i];
            }
        }
        return null;
    }

    private boolean withinWheel(double mouseX, double mouseY) {
        return Math.hypot(mouseX - wheelCenterX, mouseY - wheelCenterY) <= WHEEL_RADIUS;
    }

    @Override
    public void removed() {
        settings.saveAppearanceColors(workingColors);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private final class ValueSlider extends SliderWidget {
        ValueSlider(int x, int y, int width, int height, double initialValue) {
            super(x, y, width, height, new LiteralText(""), initialValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(new LiteralText("Brightness: " + Math.round(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            onValueSliderChanged((float) this.value);
        }
    }

    private final class AlphaSlider extends SliderWidget {
        AlphaSlider(int x, int y, int width, int height, double initialValue) {
            super(x, y, width, height, new LiteralText(""), initialValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(new LiteralText("Alpha: " + Math.round(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            onAlphaSliderChanged((int) Math.round(this.value * 255));
        }
    }
}
