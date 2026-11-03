package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.core.Result;
import dev.seeruk.bundle.itemtext.api.ItemLoreEvents;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LoreText {
    public static final int MAX_LINES = 10;
    public static final int MAX_LINE_LENGTH = 60;

    private LoreText() {}

    /**
     * Retrieves the lines of lore on an item, if it has any, or an empty list otherwise.
     */
    public static List<Component> lines(ItemStack stack) {
        final var lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return List.of();
        }

        return lore.lines();
    }

    public static Result<List<Component>, String> add(Player player, ItemStack stack, String input) {
        final var parsed = TextParsers.parseAsUntrusted(input);
        final var issue = validateLine(parsed);
        if (issue.isPresent()) {
            return new Result.Failure<>(issue.get());
        }

        final var lines = new ArrayList<>(lines(stack));
        if (lines.size() >= MAX_LINES) {
            return new Result.Failure<>("Lore can span at most " + MAX_LINES + " lines");
        }

        lines.add(parsed);
        return write(player, stack, lines.size() - 1, lines);
    }

    private static Optional<String> validateLine(Component line) {
        final var text = line.getString();

        if (text.length() > MAX_LINE_LENGTH) {
            return Optional.of("Lore lines cannot be longer than " + MAX_LINE_LENGTH + " characters");
        }

        // Empty lines are allowed, so that a player can add a blank line without having to add one
        // with text, and then use /itemlore clear to remove the text from it.

        return Optional.empty();
    }

    private static Result<List<Component>, String> write(Player player, ItemStack stack, int line, List<Component> lines) {
        final var lore = !lines.isEmpty() ? new ItemLore(lines) : ItemLore.EMPTY;

        stack.set(DataComponents.LORE, lore);
        ItemLoreEvents.WRITTEN.invoker().onWritten(player, stack, line, lines.get(line));

        return new Result.Success<>(lines);
    }
}
