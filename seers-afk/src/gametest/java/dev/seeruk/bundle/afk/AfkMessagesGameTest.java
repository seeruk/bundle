package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;


public class AfkMessagesGameTest {
    @GameTest
    public void messagesFillInTheDisplayName(GameTestHelper helper) {
        final var player = player(helper);
        final var name = player.getDisplayName().getString();
        final var messages = new AfkConfig.Messages(TextTemplate.parse("${display_name} went"), TextTemplate.parse("${display_name} came back"));

        helper.assertValueEqual(AfkMessages.message(player, true, messages).getString(), name + " went", "AFK message");
        helper.assertValueEqual(AfkMessages.message(player, false, messages).getString(), name + " came back", "back message");
        helper.succeed();
    }
}
