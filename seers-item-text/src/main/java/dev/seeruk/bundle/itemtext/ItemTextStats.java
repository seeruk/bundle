package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.itemtext.SeersItemText.MOD_ID;

/**
 * Keeps per-player item text statistics in Vanilla scoreboard objectives (see
 * {@link ScoreboardStat}).
 */
public final class ItemTextStats {
    public static final String NAME_WRITTEN_OBJECTIVE = "name_written";
    public static final String LORE_WRITTEN_OBJECTIVE = "lore_written";

    /**
     * Total number of new characters each player has written in item names.
     */
    static final ScoreboardStat NAME_WRITTEN = new ScoreboardStat(MOD_ID, NAME_WRITTEN_OBJECTIVE, "Name Characters Written");

    /**
     * Total number of new characters each player has written in item lore.
     */
    static final ScoreboardStat LORE_WRITTEN = new ScoreboardStat(MOD_ID, LORE_WRITTEN_OBJECTIVE, "Lore Characters Written");

    private ItemTextStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        NAME_WRITTEN.ensureObjective(server);
        LORE_WRITTEN.ensureObjective(server);
    }

    /**
     * Adds the length of the new name to the player's score.
     */
    public static void recordNameWritten(
        Player player,
        ItemStack item,
        Optional<Component> original,
        Optional<Component> edited
    ) {
        // Restyling a name doesn't count, only changes to its plain text
        final var plain = edited.map(Component::getString);
        if (plain.equals(original.map(Component::getString))) {
            return;
        }

        final int written = plain.map(String::length).orElse(0);
        if (written > 0) {
            NAME_WRITTEN.add(player, written);
        }
    }

    /**
     * Adds the length of any new lines of lore to the player's score.
     */
    public static void recordLoreWritten(
        Player player,
        ItemStack item,
        List<Component> original,
        List<Component> edited
    ) {
        final var written = newLoreLength(original, edited);
        if (written > 0) {
            LORE_WRITTEN.add(player, written);
        }
    }

    /**
     * Compares lines of lore to determine the number of new plaintext characters written. Lines are
     * matched by value instead of position so that lines that have moved (e.g. on insert / removal
     * of a line) don't count towards the total.
     */
    static int newLoreLength(List<Component> original, List<Component> edited) {
        final var remaining = asPlainText(original);
        final var newLines = asPlainText(edited);
        var written = 0;

        for (final var line : newLines) {
            // Each original line can only be matched once, so duplicate new lines still count
            if (!remaining.remove(line)) {
                written += line.length();
            }
        }

        return written;
    }

    private static ArrayList<String> asPlainText(List<Component> lines) {
        return new ArrayList<>(lines.stream().map(Component::getString).toList());
    }
}
