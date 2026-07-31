package com.perplexddev.shardtracker.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's optional keybindings. Both default to unbound so they never collide with an existing
 * bind; the player assigns them from Options &gt; Controls &gt; Key Binds &gt; Shard Tracker.
 */
public final class ShardTrackerKeyBindings {

    private static final String CATEGORY = "key.categories.shardtracker";

    public static final KeyBinding TOGGLE_HUD = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.shardtracker.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

    public static final KeyBinding OPEN_HUD_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.shardtracker.open_hud_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

    private ShardTrackerKeyBindings() {
    }

    /** Invoking this static method forces the field initialisers above to run exactly once. */
    public static void ensureRegistered() {
    }
}
