package dev.seeruk.bundle.core.storage;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class WorldStorageGameTest {
    // Each test has its own storage, as the data is shared by the whole test server
    private static WorldStorage<String> storage(String name) {
        return WorldStorage.create("seers-core-gametest", name, Codec.STRING, () -> "nothing yet");
    }

    @GameTest
    public void usesDefaultsUntilSomethingIsSaved(GameTestHelper helper) {
        final var storage = storage("defaults");

        helper.assertValueEqual(storage.get(helper.getLevel().getServer()), "nothing yet", "value");
        helper.succeed();
    }

    @GameTest
    public void setAndUpdateReplaceValue(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var storage = storage("set_and_update");

        storage.set(server, "first");
        helper.assertValueEqual(storage.get(server), "first", "after set");

        storage.update(server, value -> value + " and second");
        helper.assertValueEqual(storage.get(server), "first and second", "after update");
        helper.succeed();
    }

    @GameTest
    public void changesAreMarkedToBeSaved(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var storage = storage("dirty");

        storage.set(server, "changed");

        helper.assertTrue(storage.isDirty(server), "Change wasn't marked to be saved");
        helper.succeed();
    }
}
