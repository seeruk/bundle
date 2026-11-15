package dev.seeruk.bundle.itemtext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Represents the raw lore input as custom data on an item, preserving all aspects of the raw input,
 * including formatting tags, so it can be edited again as it was written.
 */
public final class LoreInput {
    private static final String KEY = SeersItemText.id("lore_input").toString();

    private LoreInput() {}

    /**
     * Returns the raw lore input stored on the given item stack, but checks that it still matches
     * the current lore. If not, it returns an empty optional.
     */
    public static Optional<String> get(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(KEY)
            .filter(input -> LoreEditor.parse(input).equals(LoreEditor.lines(stack)));
    }

    /**
     * Writes the raw lore input as custom data on the given item stack. If the input is blank,
     * the custom data is cleared.
     */
    public static void set(ItemStack stack, String input) {
        if (input.isBlank()) {
            clear(stack);
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY, input));
        }
    }

    public static void clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(KEY));
    }
}
