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
    class NewLore {
        static Stream<Arguments> shouldFindNewLoreWritten() {
            return Stream.of(
                Arguments.of(
                    "Finds nothing when both are empty",
                    List.of(),
                    List.of(),
                    new ItemTextStats.LoreWritten(0, 0)
                ),
                Arguments.of(
                    "Finds every word when there was no lore",
                    List.of(),
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    new ItemTextStats.LoreWritten(16, 2)
                ),
                Arguments.of(
                    "Finds nothing when all lore is removed",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(),
                    new ItemTextStats.LoreWritten(0, 0)
                ),
                Arguments.of(
                    "Finds nothing when the lore is unchanged",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    new ItemTextStats.LoreWritten(0, 0)
                ),
                Arguments.of(
                    "Finds only the appended line",
                    List.of(literal("Testing 1")),
                    List.of(literal("Testing 1"), literal("Test")),
                    new ItemTextStats.LoreWritten(4, 1)
                ),
                Arguments.of(
                    "Finds only the replaced line",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 1"), literal("Test")),
                    new ItemTextStats.LoreWritten(4, 1)
                ),
                Arguments.of(
                    "Doesn't find lines shifted down by an insert",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Test"), literal("Testing 1"), literal("Testing 2")),
                    new ItemTextStats.LoreWritten(4, 1)
                ),
                Arguments.of(
                    "Doesn't find blank lines",
                    List.of(literal("Testing 1")),
                    List.of(literal("Testing 1"), literal(""), literal("Test")),
                    new ItemTextStats.LoreWritten(4, 1)
                ),
                Arguments.of(
                    "Doesn't find reordered lines",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 2"), literal("Testing 1")),
                    new ItemTextStats.LoreWritten(0, 0)
                ),
                Arguments.of(
                    "Finds nothing for removed or cleared lines",
                    List.of(literal("Testing 1"), literal("Testing 2"), literal("Testing 3")),
                    List.of(literal(""), literal("Testing 3")),
                    new ItemTextStats.LoreWritten(0, 0)
                ),
                Arguments.of(
                    "Finds duplicates of existing words",
                    List.of(literal("Testing")),
                    List.of(literal("Testing"), literal("Testing")),
                    new ItemTextStats.LoreWritten(7, 1)
                ),
                Arguments.of(
                    "Doesn't find words moved to other lines",
                    List.of(literal("The quick brown"), literal("fox jumps")),
                    List.of(literal("The quick"), literal("brown fox jumps")),
                    new ItemTextStats.LoreWritten(0, 0)
                ),
                Arguments.of(
                    "Finds only the new word when lines wrap differently",
                    List.of(literal("quick brown"), literal("fox")),
                    List.of(literal("The quick"), literal("brown fox")),
                    new ItemTextStats.LoreWritten(3, 1)
                ),
                Arguments.of(
                    "Counts an emoji as one character",
                    List.of(),
                    List.of(literal("😀")),
                    new ItemTextStats.LoreWritten(1, 1)
                ),
                Arguments.of(
                    "Finds words as plain text, without their styling",
                    List.of(),
                    List.of(styledLine()),
                    new ItemTextStats.LoreWritten(11, 1)
                ),
                Arguments.of(
                    "Doesn't find restyled words",
                    List.of(literal("Testing")),
                    List.of(literal("Testing").withStyle(ChatFormatting.RED)),
                    new ItemTextStats.LoreWritten(0, 0)
                ),
                Arguments.of(
                    "Doesn't find unchanged styled lines",
                    List.of(styledLine()),
                    List.of(styledLine(), literal("Test")),
                    new ItemTextStats.LoreWritten(4, 1)
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldFindNewLoreWritten(
            final String description,
            final List<Component> original,
            final List<Component> edited,
            final ItemTextStats.LoreWritten expected
        ) {
            assertEquals(expected, ItemTextStats.loreWritten(original, edited));
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
