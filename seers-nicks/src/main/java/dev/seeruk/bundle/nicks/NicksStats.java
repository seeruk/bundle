package dev.seeruk.bundle.nicks;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.scores.ScoreHolder;

import java.util.Optional;

import static dev.seeruk.bundle.nicks.SeersNicks.MOD_ID;

/**
 * Keeps per-player nickname statistics in Vanilla scoreboard objectives (see
 * {@link ScoreboardStat}).
 */
public final class NicksStats {
    public static final String NICKNAMES_SET_OBJECTIVE = "nicknames_set";

    /**
     * Number of times each player's nickname has been set.
     */
    static final ScoreboardStat NICKNAMES_SET = new ScoreboardStat(MOD_ID, NICKNAMES_SET_OBJECTIVE, "Nicknames Set");

    private NicksStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        NICKNAMES_SET.ensureObjective(server);
    }

    /**
     * Adds one to the player's score when their nickname is set, but not reset.
     */
    public static void recordNickChanged(
        MinecraftServer server,
        NameAndId player,
        Optional<Component> original,
        Optional<Component> edited
    ) {
        if (edited.isPresent()) {
            // Scored by name, as the player may be offline
            NICKNAMES_SET.add(server, ScoreHolder.forNameOnly(player.name()), 1);
        }
    }
}
