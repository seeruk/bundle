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
    class NewLoreLines {
        static Stream<Arguments> shouldFindNewLoreLines() {
            return Stream.of(
                Arguments.of(
                    "Finds nothing when both are empty",
                    List.of(),
                    List.of(),
                    List.of()
                ),
                Arguments.of(
                    "Finds every line when there was no lore",
                    List.of(),
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of("Testing 1", "Testing 2")
                ),
                Arguments.of(
                    "Finds nothing when all lore is removed",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(),
                    List.of()
                ),
                Arguments.of(
                    "Finds nothing when the lore is unchanged",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of()
                ),
                Arguments.of(
                    "Finds only the appended line",
                    List.of(literal("Testing 1")),
                    List.of(literal("Testing 1"), literal("Test")),
                    List.of("Test")
                ),
                Arguments.of(
                    "Finds only the replaced line",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 1"), literal("Test")),
                    List.of("Test")
                ),
                Arguments.of(
                    "Doesn't find lines shifted down by an insert",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Test"), literal("Testing 1"), literal("Testing 2")),
                    List.of("Test")
                ),
                Arguments.of(
                    "Finds blank lines added by inserting past the end",
                    List.of(literal("Testing 1")),
                    List.of(literal("Testing 1"), literal(""), literal("Test")),
                    List.of("", "Test")
                ),
                Arguments.of(
                    "Doesn't find reordered lines",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    List.of(literal("Testing 2"), literal("Testing 1")),
                    List.of()
                ),
                Arguments.of(
                    "Finds only a blank line for removed or cleared lines",
                    List.of(literal("Testing 1"), literal("Testing 2"), literal("Testing 3")),
                    List.of(literal(""), literal("Testing 3")),
                    List.of("")
                ),
                Arguments.of(
                    "Finds duplicates of existing lines",
                    List.of(literal("Testing")),
                    List.of(literal("Testing"), literal("Testing")),
                    List.of("Testing")
                ),
                Arguments.of(
                    "Finds lines as plain text, without their styling",
                    List.of(),
                    List.of(styledLine()),
                    List.of("Red with bold")
                ),
                Arguments.of(
                    "Doesn't find restyled lines",
                    List.of(literal("Testing")),
                    List.of(literal("Testing").withStyle(ChatFormatting.RED)),
                    List.of()
                ),
                Arguments.of(
                    "Doesn't find unchanged styled lines",
                    List.of(styledLine()),
                    List.of(styledLine(), literal("Test")),
                    List.of("Test")
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldFindNewLoreLines(
            final String description,
            final List<Component> original,
            final List<Component> edited,
            final List<String> expected
        ) {
            assertEquals(expected, ItemTextStats.newLoreLines(original, edited));
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
