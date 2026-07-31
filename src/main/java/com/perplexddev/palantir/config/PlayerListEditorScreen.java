package com.perplexddev.palantir.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A plain-widget editor for a list of player names or patterns.
 *
 * <p>Cloth Config's own list widget has a header "+" button whose click hitbox does not reliably
 * line up with where it renders in this Cloth Config version, so adding a name through it silently
 * does nothing. This screen replaces it with an ordinary vanilla {@link ButtonWidget} and one
 * {@link TextFieldWidget} per row, both of which are used everywhere in vanilla Minecraft UI and
 * don't share that bug. Used for both Tracked Players and Ignored Players.
 *
 * <p>Rows are only rebuilt (destroying and recreating their text fields) on Add/Remove; typing is
 * left entirely to each field's own widget state so it is never disturbed mid-edit. The list of
 * rows scrolls independently of the Add button, which stays fixed near the top so it is always
 * reachable regardless of how long the list gets.
 */
public final class PlayerListEditorScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int FIELD_WIDTH = 220;
    private static final int FIELD_HEIGHT = 20;
    private static final int REMOVE_BUTTON_WIDTH = 60;
    private static final int ROW_SPACING = 8;
    private static final int LIST_LEFT = 20;
    private static final int LIST_TOP = 56;
    private static final int ADD_BUTTON_Y = 28;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final String HINT = "Esc to save and close";

    private final String subtitle;
    private final String placeholder;
    private final List<String> initialValues;
    private final Consumer<List<String>> saveConsumer;
    private final List<Row> rows = new ArrayList<>();

    private ButtonWidget addButton;
    private int scrollOffset;

    private record Row(TextFieldWidget field, ButtonWidget removeButton) {
    }

    public PlayerListEditorScreen(Text title, String subtitle, String placeholder,
                                  List<String> initialValues, Consumer<List<String>> saveConsumer) {
        super(title);
        this.subtitle = subtitle;
        this.placeholder = placeholder;
        this.initialValues = initialValues;
        this.saveConsumer = saveConsumer;
    }

    @Override
    protected void init() {
        rebuildRows(initialValues.isEmpty() ? List.of("") : initialValues, -1);
    }

    private void rebuildRows(List<String> values, int focusIndex) {
        clearChildren();
        rows.clear();

        for (String value : values) {
            addRow(value);
        }
        addButton = ButtonWidget.builder(Text.literal("+ Add"), button -> onAdd())
                .dimensions(LIST_LEFT, ADD_BUTTON_Y, 80, FIELD_HEIGHT)
                .build();
        addDrawableChild(addButton);

        layoutRows();

        if (focusIndex >= 0 && focusIndex < rows.size()) {
            TextFieldWidget field = rows.get(focusIndex).field();
            setFocused(field);
            field.setFocused(true);
        }
    }

    private void addRow(String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, FIELD_WIDTH, FIELD_HEIGHT,
                Text.literal("username"));
        field.setMaxLength(64);
        field.setText(value);
        // TextFieldWidget draws the suggestion as inline ghost text after the cursor whenever the
        // cursor sits at the end of the field's text -- true regardless of content, not just when
        // it's empty. Setting it once at creation only fixed already-filled rows: a row that starts
        // blank (via "+ Add") still got the placeholder attached, and nothing cleared it once you
        // typed into it, so it stayed glued to the end of whatever you typed, e.g.
        // "Cannunerusername or pattern". Driving it off the field's own change listener keeps it
        // correct for the whole lifetime of the row, not just its creation.
        field.setSuggestion(value.isEmpty() ? placeholder : null);
        field.setChangedListener(text -> field.setSuggestion(text.isEmpty() ? placeholder : null));

        ButtonWidget removeButton = ButtonWidget.builder(Text.literal("Remove"), button -> onRemove(field))
                .dimensions(0, 0, REMOVE_BUTTON_WIDTH, FIELD_HEIGHT)
                .build();

        rows.add(new Row(field, removeButton));
        addDrawableChild(field);
        addDrawableChild(removeButton);
    }

    private void layoutRows() {
        int y = LIST_TOP - scrollOffset;
        for (Row row : rows) {
            row.field().setX(LIST_LEFT);
            row.field().setY(y);
            row.removeButton().setX(LIST_LEFT + FIELD_WIDTH + ROW_SPACING);
            row.removeButton().setY(y);
            y += ROW_HEIGHT;
        }
    }

    private void onAdd() {
        List<String> values = currentValues();
        values.add("");
        rebuildRows(values, values.size() - 1);
    }

    private void onRemove(TextFieldWidget target) {
        List<String> values = currentValues();
        int index = indexOfField(target);
        if (index >= 0) {
            values.remove(index);
        }
        rebuildRows(values, -1);
    }

    private int indexOfField(TextFieldWidget target) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).field() == target) {
                return i;
            }
        }
        return -1;
    }

    private List<String> currentValues() {
        List<String> values = new ArrayList<>(rows.size());
        for (Row row : rows) {
            values.add(row.field().getText());
        }
        return values;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int visibleHeight = Math.max(0, height - LIST_TOP - 24);
        int contentHeight = rows.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * ROW_HEIGHT)));
        layoutRows();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        renderBackground(context, mouseX, mouseY, tickDelta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, TEXT_COLOR);
        if (!subtitle.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, subtitle, width / 2, 24, TEXT_COLOR);
        }

        super.render(context, mouseX, mouseY, tickDelta);

        context.drawCenteredTextWithShadow(textRenderer, HINT, width / 2, height - 16, TEXT_COLOR);
    }

    @Override
    public void removed() {
        List<String> cleaned = new ArrayList<>();
        for (String value : currentValues()) {
            if (!value.isBlank()) {
                cleaned.add(value);
            }
        }
        saveConsumer.accept(cleaned);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
