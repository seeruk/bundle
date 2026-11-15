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
    public static final String NAME_CHARACTERS_OBJECTIVE = "name_characters";
    public static final String LORE_CHARACTERS_OBJECTIVE = "lore_characters";
    public static final String ITEMS_RENAMED_OBJECTIVE = "items_renamed";
    public static final String LORE_LINES_WRITTEN_OBJECTIVE = "lore_lines_written";

    /**
     * Total number of new characters each player has written in item names.
     */
    static final ScoreboardStat NAME_CHARACTERS = new ScoreboardStat(MOD_ID, NAME_CHARACTERS_OBJECTIVE, "Name Characters Written");

    /**
     * Total number of new characters each player has written in item lore.
     */
    static final ScoreboardStat LORE_CHARACTERS = new ScoreboardStat(MOD_ID, LORE_CHARACTERS_OBJECTIVE, "Lore Characters Written");

    /**
     * Total number of times each player has given an item a new name.
     */
    static final ScoreboardStat ITEMS_RENAMED = new ScoreboardStat(MOD_ID, ITEMS_RENAMED_OBJECTIVE, "Items Renamed");

    /**
     * Total number of new, non-blank lines of lore each player has written.
     */
    static final ScoreboardStat LORE_LINES_WRITTEN = new ScoreboardStat(MOD_ID, LORE_LINES_WRITTEN_OBJECTIVE, "Lore Lines Written");

    private ItemTextStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        NAME_CHARACTERS.ensureObjective(server);
        LORE_CHARACTERS.ensureObjective(server);
        ITEMS_RENAMED.ensureObjective(server);
        LORE_LINES_WRITTEN.ensureObjective(server);
    }

    /**
     * Adds the length of the new name to the player's score, and one to the number of items they've
     * renamed.
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
            NAME_CHARACTERS.add(player, written);
            ITEMS_RENAMED.add(player, 1);
        }
    }

    /**
     * Adds the length of any new lines of lore to the player's score, and the number of those lines
     * that aren't blank.
     */
    public static void recordLoreWritten(
        Player player,
        ItemStack item,
        List<Component> original,
        List<Component> edited
    ) {
        final var newLines = newLoreLines(original, edited);

        final var characters = newLines.stream().mapToInt(String::length).sum();
        if (characters > 0) {
            LORE_CHARACTERS.add(player, characters);
        }

        final var lines = (int) newLines.stream().filter(line -> !line.isBlank()).count();
        if (lines > 0) {
            LORE_LINES_WRITTEN.add(player, lines);
        }
    }

    /**
     * Compares lines of lore to find the new lines written, as plain text. Lines are matched by
     * value instead of position so that lines that have moved (e.g. on insert / removal of a line)
     * aren't new.
     */
    static List<String> newLoreLines(List<Component> original, List<Component> edited) {
        final var remaining = asPlainText(original);
        final var written = new ArrayList<String>();

        for (final var line : asPlainText(edited)) {
            // Each original line can only be matched once, so duplicate new lines still count
            if (!remaining.remove(line)) {
                written.add(line);
            }
        }

        return written;
    }

    private static ArrayList<String> asPlainText(List<Component> lines) {
        return new ArrayList<>(lines.stream().map(Component::getString).toList());
    }
}
