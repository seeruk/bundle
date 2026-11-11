package dev.seeruk.bundle.itemtext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Shared helpers for building items with custom names in GameTests.
 */
public final class NameFixtures {
    private NameFixtures() {}

    public static ItemStack withName(String name) {
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    public static Optional<Component> literal(String name) {
        return Optional.of(Component.literal(name));
    }
}
