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

    public static Result<List<Component>, String> set(Player player, ItemStack stack, int lineNumber, String input) {
        final var validLineNumber = validateLineNumber(lineNumber);
        if (validLineNumber.isPresent()) {
            return new Result.Failure<>(validLineNumber.get());
        }

        final var parsed = TextParsers.parseAsUntrusted(input);
        final var issue = validateLine(parsed);
        if (issue.isPresent()) {
            return new Result.Failure<>(issue.get());
        }

        final var lines = new ArrayList<>(lines(stack));
        if (lineNumber < 1 || lineNumber > lines.size() + 1) {
            return new Result.Failure<>("Line " + lineNumber + " does not exist");
        }

        lines.set(lineNumber - 1, parsed);
        return write(player, stack, lineNumber - 1, lines);
    }

    public static Result<List<Component>, String> insert(Player player, ItemStack stack, int lineNumber, String input) {
        final var validLineNumber = validateLineNumber(lineNumber);
        if (validLineNumber.isPresent()) {
            return new Result.Failure<>(validLineNumber.get());
        }

        final var parsed = TextParsers.parseAsUntrusted(input);
        final var issue = validateLine(parsed);
        if (issue.isPresent()) {
            return new Result.Failure<>(issue.get());
        }

        final var lines = new ArrayList<>(lines(stack));
        while (lines.size() < lineNumber - 1) {
            lines.add(Component.empty());
        }
        lines.add(lineNumber - 1, parsed);
        return write(player, stack, lineNumber - 1, lines);
    }

    public static Result<List<Component>, String> remove(Player player, ItemStack stack, int lineNumber) {
        final var lines = new ArrayList<>(lines(stack));
        final var validLineNumber = validateLineNumber(lineNumber);
        if (validLineNumber.isPresent()) {
            return new Result.Failure<>(validLineNumber.get());
        }

        lines.remove(lineNumber - 1);
        return save(stack, lines);
    }

    public static Result<List<Component>, String> removeAll(Player player, ItemStack stack) {
        if (lines(stack).isEmpty()) {
            return new Result.Failure<>("That item has no lore");
        }
        return save(stack, List.of());
    }

    public static Result<List<Component>, String> clear(Player player, ItemStack stack, int lineNumber) {
        final var lines = new ArrayList<>(lines(stack));
        final var validLineNumber = validateLineNumber(lineNumber);
        if (validLineNumber.isPresent()) {
            return new Result.Failure<>(validLineNumber.get());
        }

        lines.set(lineNumber - 1, Component.empty());
        return save(stack, lines);
    }

    private static Optional<String> validateLine(Component line) {
        final var text = line.getString();

        if (text.length() > MAX_LINE_LENGTH) {
            return Optional.of("Lore lines cannot be longer than " + MAX_LINE_LENGTH + " characters");
        }

        // Empty lines are allowed, so that a player can add a blank lineNumber without having to add one
        // with text, and then use /itemlore clear to remove the text from it.

        return Optional.empty();
    }

    private static Optional<String> validateLineNumber(int lineNumber) {
        return validateLineNumber(lineNumber, Optional.empty());
    }

    private static Optional<String> validateLineNumber(int lineNumber, Optional<Integer> currentLines) {
        if (lineNumber < 1 || lineNumber > MAX_LINES) {
            return Optional.of("Line number must be between 1 and " + MAX_LINES);
        }

        if (currentLines.isPresent() && lineNumber > currentLines.get() + 1) {
            return Optional.of("Line number can't be higher than the current number of lines");
        }

        return Optional.empty();
    }

    private static Result<List<Component>, String> write(Player player, ItemStack stack, int line, List<Component> lines) {
        final var result = save(stack, lines); // Attempts save before we fire the event...
        ItemLoreEvents.WRITTEN.invoker().onWritten(player, stack, line, lines.get(line));
        return result;
    }

    private static Result<List<Component>, String> save(ItemStack stack, List<Component> lines) {
        final var lore = !lines.isEmpty() ? new ItemLore(lines) : ItemLore.EMPTY;

        stack.set(DataComponents.LORE, lore);

        return new Result.Success<>(lines);
    }
}
