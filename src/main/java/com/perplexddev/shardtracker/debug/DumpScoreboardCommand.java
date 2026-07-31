package com.perplexddev.shardtracker.debug;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * TEMPORARY diagnostic command: dumps every current tab-list entry's full scoreboard-team
 * information to a dedicated file under {@code logs/}, for inspecting exactly what a specific
 * server sends.
 *
 * <p>Not part of the mod's real feature set. To remove: delete this file and the single
 * {@code DumpScoreboardCommand.register(...)} call in {@code ShardTrackerMod}.
 */
public final class DumpScoreboardCommand {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private DumpScoreboardCommand() {
    }

    public static void register(Logger logger) {
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("shardtrackerdump")
                .executes(context -> run(context.getSource(), logger)));
    }

    private static int run(FabricClientCommandSource source, Logger logger) {
        MinecraftClient client = source.getClient();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) {
            source.sendError(new LiteralText("Not connected to a server."));
            return 0;
        }

        Collection<PlayerListEntry> entries = networkHandler.getPlayerList();
        String dump = buildDump(entries);

        String filename = "shardtracker-dump-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".log";
        Path path = client.runDirectory.toPath().resolve("logs").resolve(filename);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, dump, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to write Shard Tracker scoreboard dump", e);
            source.sendError(new LiteralText("Failed to write dump: " + e.getMessage()));
            return 0;
        }

        logger.info("Shard Tracker scoreboard dump written to {}", path);
        source.sendFeedback(new LiteralText("Dumped " + entries.size() + " tab-list entries to " + path));
        return Command.SINGLE_SUCCESS;
    }

    private static String buildDump(Collection<PlayerListEntry> entries) {
        StringBuilder dump = new StringBuilder();
        dump.append("Shard Tracker scoreboard dump\n");
        dump.append("Generated: ").append(LocalDateTime.now()).append('\n');
        dump.append("Tab-list entries: ").append(entries.size()).append("\n\n");

        int index = 0;
        for (PlayerListEntry entry : entries) {
            dump.append('[').append(index++).append("] ")
                    .append(entry.getProfile().getName())
                    .append(" (").append(entry.getProfile().getId()).append(")\n");
            dump.append("    displayName: ").append(textOrNone(entry.getDisplayName())).append('\n');
            dump.append("    gameMode: ").append(entry.getGameMode()).append('\n');
            dump.append("    latency: ").append(entry.getLatency()).append('\n');
            appendTeam(dump, entry.getScoreboardTeam());
            dump.append('\n');
        }
        return dump.toString();
    }

    private static void appendTeam(StringBuilder dump, Team team) {
        if (team == null) {
            dump.append("    team: none\n");
            return;
        }
        dump.append("    team.name: ").append(team.getName()).append('\n');
        dump.append("    team.displayName: ").append(textOrNone(team.getDisplayName())).append('\n');
        dump.append("    team.prefix: ").append(textOrNone(team.getPrefix())).append('\n');
        dump.append("    team.suffix: ").append(textOrNone(team.getSuffix())).append('\n');
        dump.append("    team.color: ").append(team.getColor()).append('\n');
        dump.append("    team.collisionRule: ").append(team.getCollisionRule()).append('\n');
        dump.append("    team.nameTagVisibilityRule: ").append(team.getNameTagVisibilityRule()).append('\n');
        dump.append("    team.deathMessageVisibilityRule: ").append(team.getDeathMessageVisibilityRule()).append('\n');
        dump.append("    team.friendlyFireAllowed: ").append(team.isFriendlyFireAllowed()).append('\n');
        dump.append("    team.showFriendlyInvisibles: ").append(team.shouldShowFriendlyInvisibles()).append('\n');
    }

    private static String textOrNone(Text text) {
        return text == null ? "<none>" : text.getString();
    }
}
