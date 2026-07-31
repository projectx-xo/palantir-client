package com.perplexddev.shardtracker;

import com.perplexddev.shardtracker.config.Settings;
import com.perplexddev.shardtracker.debug.DumpScoreboardCommand;
import com.perplexddev.shardtracker.keybind.ShardTrackerKeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point. Registers the configuration and wires the tracking, HUD and notification
 * systems to Fabric's client events.
 */
public final class ShardTrackerMod implements ClientModInitializer {

    public static final String MOD_ID = "shardtracker";

    private static final Logger LOGGER = LoggerFactory.getLogger("Shard Tracker");

    @Override
    public void onInitializeClient() {
        ShardTrackerKeyBindings.ensureRegistered();
        Settings settings = Settings.register();
        ShardTrackerRuntime.create(MinecraftClient.getInstance(), settings, LOGGER);
        // TEMPORARY: /shardtrackerdump, remove alongside DumpScoreboardCommand once done debugging.
        DumpScoreboardCommand.register(LOGGER);
        LOGGER.info("Shard Tracker initialised");
    }

    public static Logger logger() {
        return LOGGER;
    }
}
