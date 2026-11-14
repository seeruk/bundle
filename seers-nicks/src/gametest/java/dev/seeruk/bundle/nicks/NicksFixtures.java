package dev.seeruk.bundle.nicks;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.GameType;

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
}
