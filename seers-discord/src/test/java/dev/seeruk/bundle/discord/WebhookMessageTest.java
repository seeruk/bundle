package dev.seeruk.bundle.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebhookMessageTest {
    private static MessageEmbed embed(int length) {
        return new EmbedBuilder().setDescription("x".repeat(length)).build();
    }

    private static List<Integer> sizes(List<WebhookMessage> messages) {
        return messages.stream().map(message -> message.embeds().size()).toList();
    }

    @Test
    void packsEmbedsThatFitIntoOneMessage() {
        final var embeds = List.of(embed(10), embed(10), embed(10));

        assertEquals(List.of(3), sizes(WebhookMessage.packed(embeds)));
    }

    @Test
    void startsAnotherMessageAfterTheMostEmbedsOneCanHave() {
        final var embeds = IntStream.range(0, 11).mapToObj(i -> embed(10)).toList();

        assertEquals(List.of(10, 1), sizes(WebhookMessage.packed(embeds)));
    }

    @Test
    void startsAnotherMessageWhenTheEmbedsHaveTooMuchText() {
        // 4,000 characters each, where a message can only have 6,000 between its embeds
        final var embeds = Collections.nCopies(3, embed(4_000));

        assertEquals(List.of(1, 1, 1), sizes(WebhookMessage.packed(embeds)));
    }

    @Test
    void packsNothingIntoNoMessages() {
        assertEquals(List.of(), WebhookMessage.packed(List.of()));
    }
}
