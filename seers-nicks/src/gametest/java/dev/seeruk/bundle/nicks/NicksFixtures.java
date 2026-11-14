package dev.seeruk.bundle.nicks;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Shared helpers for Nicks' GameTests.
 */
public final class NicksFixtures {
    private NicksFixtures() {}

    /**
     * A survival player in the level, with no nickname.
     */
    @SuppressWarnings("removal")
    public static ServerPlayer player(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        // Vanilla's mock player is in creative by default
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    public static NameAndId nameAndId(ServerPlayer player) {
        return new NameAndId(player.getUUID(), player.getGameProfile().name());
    }

    /**
     * A player the server hasn't seen, with a name unique to this test run.
     */
    public static NameAndId offline(String name) {
        return new NameAndId(UUID.randomUUID(), unique(name));
    }

    /**
     * A nickname unique to this test run, as tests share stored nicknames, and players can't share
     * a nickname.
     */
    public static String nickname(String name) {
        return unique(name);
    }

    private static String unique(String name) {
        return name + UUID.randomUUID().toString().substring(0, 4);
    }
}
