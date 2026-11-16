package dev.seeruk.bundle.text;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextTemplateTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void fillsInPlaceholders() {
        final var template = TextTemplate.parse("${player} is now AFK");

        assertEquals("Seer is now AFK", template.format(Map.of("player", Component.literal("Seer"))).getString());
    }

    @Test
    void keepsTheValuesStyle() {
        final var name = Component.literal("Seer").withStyle(ChatFormatting.RED);
        final var formatted = TextTemplate.parse("<yellow>${player} is now AFK</yellow>").format(Map.of(
            "player",
            name
        ));

        final var parts = formatted.toFlatList();
        assertEquals("Seer", parts.getFirst().getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), parts.getFirst().getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), parts.getLast().getStyle().getColor());
    }

    @Test
    void canBeUsedMoreThanOnce() {
        final var template = TextTemplate.parse("${player}");

        assertEquals("Seer", template.format(Map.of("player", Component.literal("Seer"))).getString());
        assertEquals("Elliot", template.format(Map.of("player", Component.literal("Elliot"))).getString());
    }

    @Test
    void leavesPlaceholdersWithoutValuesAsWritten() {
        assertEquals("${other} is now AFK", TextTemplate.parse("${other} is now AFK").format(Map.of()).getString());
    }

    @Test
    void keepsTheTemplateAsWritten() {
        assertEquals("<yellow>${player}</yellow>", TextTemplate.parse("<yellow>${player}</yellow>").template());
    }
}
