package dev.seeruk.bundle.itemtext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.Arrays;
import java.util.List;

/**
 * Shared helpers for building items with lore in GameTests.
 */
public final class LoreFixtures {
    private LoreFixtures() {}

    public static ItemStack withLore(String... lines) {
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        stack.set(DataComponents.LORE, new ItemLore(literals(lines)));
        return stack;
    }

    public static List<Component> literals(String... lines) {
        return Arrays.stream(lines).<Component>map(Component::literal).toList();
    }
}
