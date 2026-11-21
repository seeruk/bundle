package dev.seeruk.bundle.core.storage;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Per-player data that's saved with the player, and kept when they die. It's only available while
 * the player is online.
 *
 * <p>Any value should be immutable (e.g. a record), and changed with {@link #set} or
 * {@link #update}. Storage must be created while mods initialize (e.g. in a static field), so
 * saved values can be read back when players join.
 */
public final class PlayerStorage<T> {
    private final AttachmentType<T> type;
    private final Supplier<T> defaults;

    private PlayerStorage(AttachmentType<T> type, Supplier<T> defaults) {
        this.type = type;
        this.defaults = defaults;
    }

    /**
     * @param modId    The mod the data belongs to.
     * @param name     What the data's called.
     * @param codec    How to save and load the value.
     * @param defaults The value to use until something's been saved for the player.
     */
    public static <T> PlayerStorage<T> create(String modId, String name, Codec<T> codec, Supplier<T> defaults) {
        return new PlayerStorage<>(
            AttachmentRegistry.create(
                Identifier.fromNamespaceAndPath(modId, name),
                builder -> builder.persistent(codec).copyOnDeath()
            ),
            defaults
        );
    }

    public T get(Player player) {
        return player.getAttachedOrGet(type, defaults);
    }

    public void set(Player player, T value) {
        player.setAttached(type, value);
    }

    /**
     * Replaces the value with whatever {@code update} returns for the current value.
     */
    public void update(Player player, UnaryOperator<T> update) {
        set(player, update.apply(get(player)));
    }
}
