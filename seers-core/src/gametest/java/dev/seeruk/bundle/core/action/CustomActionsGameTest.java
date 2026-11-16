package dev.seeruk.bundle.core.action;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class CustomActionsGameTest {
    private record Received(ServerPlayer player, CompoundTag payload) {}

    @GameTest
    public void passesActionsToTheirHandler(GameTestHelper helper) {
        final var player = player(helper);
        final var received = register("passes_actions");
        final var payload = new CompoundTag();
        payload.putString("lore", "Seer");

        player.connection.handleCustomClickAction(new ServerboundCustomClickActionPacket(
            id("passes_actions"),
            Optional.of(payload)
        ));

        helper.assertValueEqual(received, List.of(new Received(player, payload)), "received actions");
        helper.succeed();
    }

    @GameTest
    public void passesAnEmptyPayloadWhenThereIsNone(GameTestHelper helper) {
        final var player = player(helper);
        final var received = register("empty_payload");

        player.connection.handleCustomClickAction(new ServerboundCustomClickActionPacket(
            id("empty_payload"),
            Optional.empty()
        ));

        helper.assertValueEqual(received, List.of(new Received(player, new CompoundTag())), "received actions");
        helper.succeed();
    }

    @GameTest
    public void ignoresOtherActions(GameTestHelper helper) {
        final var player = player(helper);
        final var received = register("ignores_others");

        player.connection.handleCustomClickAction(new ServerboundCustomClickActionPacket(
            id("something_else"),
            Optional.empty()
        ));

        helper.assertTrue(received.isEmpty(), "Handler received another action");
        helper.succeed();
    }

    // Each test has its own ID, so handlers registered by other tests don't receive its actions
    private static List<Received> register(String name) {
        final var received = new ArrayList<Received>();
        CustomActions.register(id(name), (player, payload) -> received.add(new Received(player, payload)));
        return received;
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("seers-core-gametest", name);
    }
}
