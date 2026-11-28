package dev.seeruk.bundle.digest.book.template;

import dev.seeruk.bundle.digest.book.template.Value.Amount;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VariablesTest {
    private static final Variables VARIABLES = new Variables(Map.of("count", Amount.count(3), "player", Value.Text.of("Seer")));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void fillsInEveryPlaceholder() {
        final var formatted = VARIABLES.formatIfReadable(TextTemplate.parse("${player} and ${count|plural:friend}"));

        assertEquals(Optional.of("Seer and 3 friends"), formatted.map(Component::getString));
    }

    @Test
    void formatsNothingIfAPlaceholderDoesNotReadWell() {
        assertEquals(Optional.empty(), VARIABLES.formatIfReadable(TextTemplate.parse("${player} beat ${leader}")));
    }

    @Test
    void formatsAnywayLeavingWhatDoesNotReadWellAsWritten() {
        final var formatted = VARIABLES.format(TextTemplate.parse("${player} beat ${leader}"));

        assertEquals("Seer beat ${leader}", formatted.getString());
    }

    @Test
    void addsValuesReplacingThoseWithTheSameName() {
        final var item = VARIABLES.with(Map.of("player", Value.Text.of("Alex"), "rank", Amount.count(1)));

        assertEquals(
            Optional.of("Alex is 1 of 3"),
            item.formatIfReadable(TextTemplate.parse("${player} is ${rank} of ${count}")).map(Component::getString)
        );
        assertEquals(Optional.of("Seer"), VARIABLES.fill("player").map(Component::getString));
    }
}
