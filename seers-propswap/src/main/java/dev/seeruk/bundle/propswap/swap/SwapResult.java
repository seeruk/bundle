package dev.seeruk.bundle.propswap.swap;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The outcome of a {@link Swap}.
 *
 * @param message Shown to the player in the action bar.
 */
public record SwapResult(BlockState state, Component message) {
    /**
     * A property was changed, with the same message as the debug stick (e.g. {@code "lit" to true}).
     */
    public static <T extends Comparable<T>> SwapResult changed(BlockState state, Property<T> property) {
        final var message = Component.translatable(
            "item.minecraft.debug_stick.update",
            property.getName(),
            property.getName(state.getValue(property))
        );
        return new SwapResult(state, message);
    }
}
