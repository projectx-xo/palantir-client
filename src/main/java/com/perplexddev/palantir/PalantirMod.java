package com.perplexddev.palantir;

import com.perplexddev.palantir.config.Settings;
import com.perplexddev.palantir.debug.DumpScoreboardCommand;
import com.perplexddev.palantir.keybind.PalantirKeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point. Registers the configuration and wires the tracking, HUD and notification
 * systems to Fabric's client events.
 */
public final class PalantirMod implements ClientModInitializer {

    public static final String MOD_ID = "palantir";

    private static final Logger LOGGER = LoggerFactory.getLogger("Palantir Client");

    @Override
    public void onInitializeClient() {
        PalantirKeyBindings.ensureRegistered();
        Settings settings = Settings.register();
        PalantirRuntime.create(MinecraftClient.getInstance(), settings, LOGGER);
        // TEMPORARY: /palantirdump, remove alongside DumpScoreboardCommand once done debugging.
        DumpScoreboardCommand.register(LOGGER);
        LOGGER.info("Palantir Client initialised");
    }

    public static Logger logger() {
        return LOGGER;
    }
}
