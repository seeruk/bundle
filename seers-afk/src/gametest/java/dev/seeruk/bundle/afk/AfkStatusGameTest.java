package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.AfkFixtures.Changed;
import dev.seeruk.bundle.afk.api.AfkReason;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Util;

import java.util.List;

import static dev.seeruk.bundle.afk.AfkFixtures.eventsFor;
import static dev.seeruk.bundle.afk.AfkFixtures.player;

public class AfkStatusGameTest {
    private static final long TIMEOUT = 1000;

    @GameTest
    public void playerGoesAfkAfterTheTimeout(GameTestHelper helper) {
        final var player = player(helper);

        AfkStatus.update(player, player.getLastActionTime() + TIMEOUT, TIMEOUT);

        helper.assertTrue(AfkStatus.isAfk(player), "Player wasn't AFK");
        helper.assertValueEqual(eventsFor(player), List.of(new Changed(player.getUUID(), true, AfkReason.TIMEOUT)), "events");
        helper.succeed();
    }

    @GameTest
    public void playerIsntAfkBeforeTheTimeout(GameTestHelper helper) {
        final var player = player(helper);

        AfkStatus.update(player, player.getLastActionTime() + TIMEOUT - 1, TIMEOUT);

        helper.assertFalse(AfkStatus.isAfk(player), "Player was AFK");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired");
        helper.succeed();
    }

    @GameTest
    public void doingSomethingBringsPlayerBack(GameTestHelper helper) {
        final var player = player(helper);
        AfkStatus.update(player, player.getLastActionTime() + TIMEOUT, TIMEOUT);

        // A tick later, so the action happens at a different time to the one before going AFK
        helper.runAfterDelay(1, () -> {
            player.resetLastActionTime();
            AfkStatus.update(player, Util.getMillis(), TIMEOUT);

            helper.assertFalse(AfkStatus.isAfk(player), "Player was still AFK");
            helper.assertValueEqual(eventsFor(player).getLast(), new Changed(player.getUUID(), false, AfkReason.ACTIVITY), "event");
            helper.succeed();
        });
    }

    @GameTest
    public void forgettingPlayerDoesntAnnounceIt(GameTestHelper helper) {
        final var player = player(helper);
        AfkStatus.update(player, player.getLastActionTime() + TIMEOUT, TIMEOUT);

        AfkStatus.forget(player);

        helper.assertFalse(AfkStatus.isAfk(player), "Player was still AFK");
        helper.assertValueEqual(eventsFor(player).size(), 1, "event count");
        helper.succeed();
    }
}
