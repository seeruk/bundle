package dev.seeruk.bundle.itemtext;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemTextStatsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class NewLoreLength {
        static Stream<Arguments> shouldCountNewLoreLength() {
            return Stream.of(
                Arguments.of(
                    "Counts nothing when both are empty",
                    List.of(),
                    List.of(),
                    0
                ),
                Arguments.of(
                    "Counts every line when there was no lore",
                    List.of(),
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    18
                ),
                Arguments.of(
                    "Counts nothing when all lore is removed",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(),
                    0
                ),
                Arguments.of(
                    "Counts nothing when the lore is unchanged",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    0
                ),
                Arguments.of(
                    "Counts only the appended line",
                    List.of(literal("Testing 1")),
                    List.of(literal("Testing 1"), literal("Test")),
                    4
                ),
                Arguments.of(
                    "Counts only the replaced line",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 1"), literal("Test")),
                    4
                ),
                Arguments.of(
                    "Doesn't count lines shifted down by an insert",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Test"), literal("Testing 1"), literal("Testing 2")),
                    4
                ),
                Arguments.of(
                    "Doesn't count reordered lines",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 2"), literal("Testing 1")),
                    0
                ),
                Arguments.of(
                    "Counts nothing for removed or cleared lines",
                    List.of(literal("Testing 1"), literal("Testing 2"), literal("Testing 3")),
                    List.of(literal(""), literal("Testing 3")),
                    0
                ),
                Arguments.of(
                    "Counts duplicates of existing lines",
                    List.of(literal("Testing")),
                    List.of(literal("Testing"), literal("Testing")),
                    7
                ),
                Arguments.of(
                    "Styling doesn't count towards length",
                    List.of(),
                    List.of(styledLine()),
                    13
                ),
                Arguments.of(
                    "Doesn't count restyled lines",
                    List.of(literal("Testing")),
                    List.of(literal("Testing").withStyle(ChatFormatting.RED)),
                    0
                ),
                Arguments.of(
                    "Doesn't count unchanged styled lines",
                    List.of(styledLine()),
                    List.of(styledLine(), literal("Test")),
                    4
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldCountNewLoreLength(
            final String description,
            final List<Component> original,
            final List<Component> edited,
            final int expected
        ) {
            assertEquals(expected, ItemTextStats.newLoreLength(original, edited));
        }
    }

    private static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    private static Component styledLine() {
        return literal("Red").withStyle(ChatFormatting.RED)
            .append(literal(" with bold").withStyle(ChatFormatting.BOLD));
    }
}
