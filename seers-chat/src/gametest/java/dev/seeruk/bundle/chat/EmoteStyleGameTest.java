package dev.seeruk.bundle.chat;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Style;

public class EmoteStyleGameTest {
    @GameTest
    public void emotesAreGreyAndItalic(GameTestHelper helper) {
        // The mod's data replaces Vanilla's emote chat type, which clients are sent when they join
        final var emote = helper.getLevel().registryAccess()
            .lookupOrThrow(Registries.CHAT_TYPE)
            .getOrThrow(ChatType.EMOTE_COMMAND);

        helper.assertValueEqual(
            emote.value().chat().style(),
            Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true),
            "emote style"
        );
        helper.succeed();
    }
}
