package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.itemtext.api.ItemLoreEvents;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

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

    public static LoreEditResult add(Player player, ItemStack stack, String input) {
        final var parsed = TextParsers.parseAsUntrusted(input);
        return edit(player, stack, () -> LoreLines.add(lines(stack), parsed));
    }

    public static LoreEditResult set(Player player, ItemStack stack, int lineNumber, String input) {
        final var parsed = TextParsers.parseAsUntrusted(input);
        return edit(player, stack, () -> LoreLines.set(lines(stack), lineNumber, parsed));
    }

    public static LoreEditResult insert(Player player, ItemStack stack, int lineNumber, String input) {
        final var parsed = TextParsers.parseAsUntrusted(input);
        return edit(player, stack, () -> LoreLines.insert(lines(stack), lineNumber, parsed));
    }

    public static LoreEditResult remove(Player player, ItemStack stack, int lineNumber) {
        return edit(player, stack, () -> LoreLines.remove(lines(stack), lineNumber));
    }

    public static LoreEditResult removeAll(Player player, ItemStack stack) {
        return edit(player, stack, () -> new LoreEditResult.Edited(lines(stack), ItemLore.EMPTY.lines()));
    }

    public static LoreEditResult clear(Player player, ItemStack stack, int lineNumber) {
        return edit(player, stack, () -> LoreLines.clear(lines(stack), lineNumber));
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
        ItemLoreEvents.EDITED.invoker().onEdited(player, stack, result.original(), result.edited());
    }
}
