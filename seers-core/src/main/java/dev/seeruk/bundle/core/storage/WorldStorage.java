package dev.seeruk.bundle.core.storage;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Server-wide data that's saved with the world alongside Vanilla's saved data. The data is stored
 * in the overworld's data storage. Data here is always accessible (i.e. is not reliant on a player
 * being online, for example).
 *
 * <p>Any value should be immutable (e.g. a record, or an immutable map), and changed with
 * {@link #set} or {@link #update}, so it's always saved when it changes.
 */
public final class WorldStorage<T> {
    private final SavedDataType<Holder<T>> type;

    private WorldStorage(SavedDataType<Holder<T>> type) {
        this.type = type;
    }

    /**
     * @param modId    The mod the data belongs to.
     * @param name     What the data's called, which is also its file name.
     * @param codec    How to save and load the value.
     * @param defaults The value to use until something's been saved.
     */
    public static <T> WorldStorage<T> create(String modId, String name, Codec<T> codec, Supplier<T> defaults) {
        // No data fixer, as it's our own data, not Vanilla's
        return new WorldStorage<>(new SavedDataType<>(
            Identifier.fromNamespaceAndPath(modId, name),
            () -> new Holder<>(defaults.get()),
            codec.xmap(Holder::new, Holder::value),
            null
        ));
    }

    public T get(MinecraftServer server) {
        return holder(server).value();
    }

    /**
     * Replaces the value, and marks it to be saved.
     */
    public void set(MinecraftServer server, T value) {
        final var holder = holder(server);
        holder.value = value;
        holder.setDirty();
    }

    /**
     * Replaces the value with whatever {@code update} returns for the current one.
     */
    public void update(MinecraftServer server, UnaryOperator<T> update) {
        set(server, update.apply(get(server)));
    }

    /**
     * Whether there are changes that haven't been saved yet. Mostly useful for tests...
     */
    public boolean isDirty(MinecraftServer server) {
        return holder(server).isDirty();
    }

    private Holder<T> holder(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(type);
    }

    private static final class Holder<T> extends SavedData {
        // Volatile, as the value can be read from any level's thread
        private volatile T value;

        private Holder(T value) {
            this.value = value;
        }

        private T value() {
            return value;
        }
    }
}
