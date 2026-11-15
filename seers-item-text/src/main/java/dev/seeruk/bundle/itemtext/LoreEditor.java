package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.itemtext.api.ItemLoreEvents;
import dev.seeruk.bundle.text.TextComponents;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * LoreEditor is a service that ties together various disparate pieces of lore-editing related
 * functionality into the core business logic of this mod, in terms of editing lore on items.
 *
 * <p>This ties together pulling lore from an {@code ItemStack}, editing lines of lore, and firing
 * events at appropriate points.
 */
public final class LoreEditor {
    private LoreEditor() {}

    /**
     * Retrieve the lines of lore from an item stack.
     *
     * @param stack The item stack to retrieve lore from.
     * @return The lines of lore from the item stack, or an empty list if there was no lore.
     */
    public static List<Component> lines(ItemStack stack) {
        final var lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return List.of();
        }

        return lore.lines();
    }

    /**
     * Replaces all the lore with lines written as one piece of text (see {@link #parse}), and
     * remembers the text so it can be edited again as it was written.
     */
    public static LoreEditResult replace(Player player, ItemStack stack, String input) {
        final var lines = parse(input);
        final var result = edit(player, stack, () -> LoreLines.replace(lines(stack), lines));
        if (result instanceof LoreEditResult.Edited) {
            LoreInput.set(stack, input);
        }

        return result;
    }

    /**
     * Turns one continuous piece of text into appropriately split lines of lore, after parsing.
     * It's important the parsing happens first, so formatting tags are applied across the whole
     * text (and not just per line). Text is wrapped as needed.
     */
    public static List<Component> parse(String input) {
        final var lines = new ArrayList<Component>();
        for (final var line : TextComponents.splitLines(TextParsers.parseAsUntrusted(input))) {
            lines.addAll(TextComponents.wrap(line, LoreLines.MAX_LINE_LENGTH));
        }

        while (!lines.isEmpty() && lines.getLast().getString().isBlank()) {
            lines.removeLast();
        }

        return lines;
    }

    /**
     * Removes all the lore, and the raw input stored on the input.
     */
    public static LoreEditResult remove(Player player, ItemStack stack) {
        final var result = edit(player, stack, () -> new LoreEditResult.Edited(lines(stack), ItemLore.EMPTY.lines()));
        LoreInput.clear(stack);
        return result;
    }

    private static LoreEditResult edit(Player player, ItemStack stack, Supplier<LoreEditResult> operation) {
        final var result = operation.get();
        // If the lore hasn't changed, we don't want to deny the edit, but we also don't want to do
        // things like fire events - we can just ignore it silently.
        if (result instanceof LoreEditResult.Edited edited && !edited.original().equals(edited.edited())) {
            save(player, stack, edited);
        }

        return result;
    }

    private static void save(Player player, ItemStack stack, LoreEditResult.Edited result) {
        final var lore = result.edited().isEmpty() ? ItemLore.EMPTY : new ItemLore(result.edited());

        stack.set(DataComponents.LORE, lore);
        ItemLoreEvents.CHANGED.invoker().onLoreChanged(player, stack, result.original(), result.edited());
    }
}
