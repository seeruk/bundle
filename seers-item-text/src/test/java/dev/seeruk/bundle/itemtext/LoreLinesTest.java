package dev.seeruk.bundle.itemtext;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoreLinesTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Replace {
        @Test
        void replacesEveryLine() {
            final var lines = List.<Component>of(literal("Testing 1"));
            final var replacement = List.<Component>of(literal("Testing 2"), literal("Testing 3"));

            assertEquals(new LoreEditResult.Edited(lines, replacement), LoreLines.replace(lines, replacement));
        }

        @Test
        void replacesWithNoLines() {
            final var lines = List.<Component>of(literal("Testing 1"));

            assertEquals(new LoreEditResult.Edited(lines, List.of()), LoreLines.replace(lines, List.of()));
        }

        @Test
        void refusesTooManyLines() {
            final var replacement = numberedLines(LoreLines.MAX_LINES + 1);

            assertEquals(
                new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, LoreLines.MAX_LINES + 1),
                LoreLines.replace(List.of(), replacement)
            );
        }
    }

    private static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    private static List<Component> numberedLines(int count) {
        return IntStream.range(0, count)
            .<Component>mapToObj(i -> literal("%d".formatted(i)))
            .toList();
    }
}
