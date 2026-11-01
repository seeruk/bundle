package dev.seeruk.bundle.disposal;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import dev.seeruk.bundle.disposal.api.DisposalEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static dev.seeruk.bundle.disposal.SeersDisposal.MOD_ID;

/**
 * Keeps per-player disposal statistics in Vanilla scoreboard objectives (see
 * {@link ScoreboardStat}).
 */
public final class DisposalStats {
    /**
     * Total number of items each player has disposed of.
     */
    public static final String DISPOSED_OBJECTIVE = "items_disposed";

    private static final ScoreboardStat DISPOSED = new ScoreboardStat(MOD_ID, DISPOSED_OBJECTIVE, "Items Disposed");

    private DisposalStats() {}

    /**
     * Creates any missing objectives. Called when the server starts, and safe to call more than
     * once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        DISPOSED.ensureObjective(server);
    }

    /**
     * Adds the disposed items to the player's score. Listens to {@link DisposalEvents#DISPOSED}.
     */
    public static void recordDisposed(Player player, List<ItemStack> items) {
        DISPOSED.add(player, items.stream().mapToInt(ItemStack::getCount).sum());
    }
}
