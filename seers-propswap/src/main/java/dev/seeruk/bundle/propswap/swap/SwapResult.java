package dev.seeruk.bundle.propswap.swap;

import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The outcome of a {@link Swap}.
 *
 * @param state   The block state after the swap.
 * @param message Shown to the player in the action bar.
 * @param lock    Whether to lock the block (see {@link LockedBlocks}).
 */
public record SwapResult(BlockState state, Component message, boolean lock) {
    public SwapResult(BlockState state, Component message) {
        this(state, message, false);
    }

    public SwapResult locked() {
        return new SwapResult(state, message, true);
    }

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
