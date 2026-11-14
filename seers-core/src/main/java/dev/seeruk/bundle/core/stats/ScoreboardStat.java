package dev.seeruk.bundle.core.stats;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * A per-player statistic kept in a Vanilla scoreboard objective, so it persists with the world and
 * works for everyone without a client mod. A real Vanilla statistic isn't an option, as Fabric
 * syncs that registry to clients.
 *
 * @param modId         the mod's ID, e.g. {@code seers-disposal}
 * @param objectiveName the objective's name, e.g. {@code items_disposed}
 * @param displayName   the objective's display name, e.g. {@code Items Disposed}
 */
public record ScoreboardStat(String modId, String objectiveName, String displayName) {
    /**
     * Creates the objective if it's missing. Safe to call repeatedly.
     */
    public void ensureObjective(MinecraftServer server) {
        final var scoreboard = server.getScoreboard();
        final var objectiveId = objectiveId();

        if (scoreboard.getObjective(objectiveId) == null) {
            scoreboard.addObjective(
                objectiveId,
                ObjectiveCriteria.DUMMY,
                Component.literal(displayName),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
            );
        }
    }

    /**
     * Adds to the player's score. Does nothing if the objective has been removed (i.e. doesn't
     * throw or anything). Call {@link #ensureObjective(MinecraftServer)} before calls that require
     * the objective to exist.
     */
    public void add(Player player, int amount) {
        add(player.level().getServer(), player, amount);
    }

    /**
     * Like {@link #add(Player, int)}, for any score holder, e.g. an offline player via
     * {@link ScoreHolder#forNameOnly(String)}.
     */
    public void add(MinecraftServer server, ScoreHolder holder, int amount) {
        final var scoreboard = server.getScoreboard();
        final var scoreObjective = scoreboard.getObjective(objectiveId());
        if (scoreObjective == null) {
            return; // Either removed, or failed to initialize.
        }

        scoreboard.getOrCreatePlayerScore(holder, scoreObjective).add(amount);
    }

    /**
     * The objective's full name, e.g. {@code seers_disposal.items_disposed}. Mod IDs use hyphens,
     * but objective names conventionally use underscores, so they're swapped.
     */
    public String objectiveId() {
        return "%s.%s".formatted(modId.replace('-', '_'), objectiveName);
    }
}
