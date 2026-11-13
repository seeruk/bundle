package dev.seeruk.bundle.propswap.swap;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The outcome of a {@link Swap}.
 *
 * @param message Shown to the player in the action bar.
 */
public record SwapResult(BlockState state, Component message) {
    /**
     * A property was changed, with the same message as the debug stick (e.g.
     * {@code "lit" to true}).
     */
    public static <T extends Comparable<T>> SwapResult changed(BlockState state, Property<T> property) {
        return changed(state, property.getName(), property.getName(state.getValue(property)));
    }

    /**
     * Several properties were changed at once, with a made-up name in the debug stick's message
     * (e.g. {@code "sides" to none}).
     */
    public static SwapResult changed(BlockState state, String name, String value) {
        return new SwapResult(state, Component.translatable("item.minecraft.debug_stick.update", name, value));
    }

    /**
     * Moves a property to its next value, or its previous one if {@code backwards}, like the debug
     * stick does.
     */
    public static <T extends Comparable<T>> SwapResult cycled(BlockState state, Property<T> property, boolean backwards) {
        final var values = property.getPossibleValues();
        final var current = state.getValue(property);
        final var value = backwards ? Util.findPreviousInIterable(values, current) : Util.findNextInIterable(values, current);

        return changed(state.setValue(property, value), property);
    }
}
