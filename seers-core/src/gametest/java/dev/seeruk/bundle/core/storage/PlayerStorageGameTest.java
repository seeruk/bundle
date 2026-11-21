package dev.seeruk.bundle.core.storage;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.players.NameAndId;

import java.util.UUID;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class PlayerStorageGameTest {
    private static final PlayerStorage<String> STORAGE = PlayerStorage.create(
        "seers-core-gametest",
        "player_storage",
        Codec.STRING,
        () -> "nothing yet"
    );

    @GameTest
    public void usesDefaultsUntilSomethingIsSaved(GameTestHelper helper) {
        helper.assertValueEqual(STORAGE.get(player(helper)), "nothing yet", "value");
        helper.succeed();
    }

    @GameTest
    public void setAndUpdateReplaceValue(GameTestHelper helper) {
        final var player = player(helper);

        STORAGE.set(player, "first");
        helper.assertValueEqual(STORAGE.get(player), "first", "after set");

        STORAGE.update(player, value -> value + " and second");
        helper.assertValueEqual(STORAGE.get(player), "first and second", "after update");
        helper.succeed();
    }

    @GameTest
    public void valueIsKeptOnRespawn(GameTestHelper helper) {
        final var player = player(helper);
        STORAGE.set(player, "kept");

        player.kill(helper.getLevel());
        player.connection.handleClientCommand(
            new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)
        );

        final var respawned = helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID());
        helper.assertTrue(respawned != player, "Player didn't respawn");
        helper.assertValueEqual(STORAGE.get(respawned), "kept", "value");
        helper.succeed();
    }

    @GameTest
    public void valueIsSavedWithThePlayer(GameTestHelper helper) {
        final var id = UUID.randomUUID();
        final var nameAndId = new NameAndId(id, "test_" + id.toString().substring(0, 8));
        final var player = player(helper, nameAndId);
        STORAGE.set(player, "saved");

        player.connection.disconnect(Component.literal("Left the game"));
        final var rejoined = player(helper, nameAndId);

        helper.assertTrue(rejoined != player, "Player didn't rejoin");
        helper.assertValueEqual(STORAGE.get(rejoined), "saved", "value");
        helper.succeed();
    }
}
