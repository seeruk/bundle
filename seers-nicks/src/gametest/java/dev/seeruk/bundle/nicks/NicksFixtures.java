package dev.seeruk.bundle.nicks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.UUID;

/**
 * Shared helpers for Nicks' GameTests.
 */
public final class NicksFixtures {
    private NicksFixtures() {}

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
