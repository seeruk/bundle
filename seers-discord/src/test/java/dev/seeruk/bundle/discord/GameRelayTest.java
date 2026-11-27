package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.testing.TextChunks;
import dev.seeruk.bundle.core.testing.TextChunks.Chunk;
import dev.seeruk.bundle.discord.config.DiscordConfig;
import dev.seeruk.bundle.discord.markdown.Mentions;
import dev.seeruk.bundle.text.TextColours;
import dev.seeruk.bundle.text.TextComponents;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameRelayTest {
    // Templates are parsed when they're made, which needs the game set up first, so this runs
    // before FORMATS is made
    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final DiscordConfig.GameSettings FORMATS = new DiscordConfig.GameSettings(
        // The default formats
        TextTemplate.parse(
            "${reply}<blue><<hover 'Discord user @${username}'>${display_name}</hover>></blue> ${message}"
        ),
        TextTemplate.parse("<gray><hover '${display_name}: ${message}'>➥</hover></gray> ")
    );

    // What's shown before each message with FORMATS
    private static final String PREFIX = "<Seer> ";

    @Test
    void shouldShowMessagesInTheFormat() {
        final var chunks = TextChunks.chunks(relay(message("Hello")).orElseThrow());

        // Like players' chat, with the whole name in blue
        assertEquals(List.of("<", "Seer", ">", " ", "Hello"), chunks.stream().map(Chunk::text).toList());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), chunks.get(0).style().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), chunks.get(1).style().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), chunks.get(2).style().getColor());
        assertEquals(Style.EMPTY, chunks.get(4).style());
    }

    @Test
    void shouldShowTheSendersUsernameWhenTheirNameIsHoveredOver() {
        final var name = TextChunks.chunks(relay(message("Hello")).orElseThrow()).get(1);

        assertEquals("Discord user @seeruk", hoverText(name));
    }

    @Test
    void shouldKeepTheMessagesFormatting() {
        final var chunks = TextChunks.chunks(relay(message("**Hello**")).orElseThrow());

        assertEquals(chunk("Hello", Style.EMPTY.withBold(true)), chunks.getLast());
    }

    @Test
    void shouldNameEmojiInTheSendersName() {
        final var shown = relay(new DiscordMessage(
            "Seer 😄", "seeruk", "Hello", Mentions.NONE, Optional.empty(), List.of(), List.of()
        )).orElseThrow();

        assertEquals("<Seer :smile:> Hello", shown.getString());
    }

    @Test
    void shouldCutLongMessagesDown() {
        final var text = messageText(relay(message("a".repeat(300))).orElseThrow());

        assertEquals(GameRelay.MESSAGE.length(), text.length());
        assertTrue(text.endsWith(GameRelay.TOO_LONG), "Should end with " + GameRelay.TOO_LONG);
    }

    @Test
    void shouldNotCutMessagesThatFit() {
        final var fits = "a".repeat(GameRelay.MESSAGE.length());

        assertEquals(fits, messageText(relay(message(fits)).orElseThrow()));
    }

    @Test
    void shouldCountEmojiAsOneCharacterWhenCutting() {
        final var text = messageText(relay(message("😀".repeat(300))).orElseThrow());

        assertEquals(GameRelay.MESSAGE.length(), TextComponents.length(text));
    }

    @Test
    void shouldCutMessagesWithTooManyLinesDown() {
        final var text = messageText(relay(message("Line\n".repeat(20))).orElseThrow());

        assertEquals("Line\n".repeat(GameRelay.MESSAGE.lines() - 1) + "Line" + GameRelay.TOO_LONG, text);
    }

    @Test
    void shouldNotCutMessagesWithLinesThatFit() {
        final var fits = String.join("\n", Collections.nCopies(GameRelay.MESSAGE.lines(), "Line"));

        assertEquals(fits, messageText(relay(message(fits)).orElseThrow()));
    }

    @Test
    void shouldRemoveTheGamesFormattingCodesFromEverythingShownFromDiscord() {
        final var shown = relay(new DiscordMessage(
            "\u00A7cSeer",
            "seeruk",
            "\u00A7l**Hello**",
            Mentions.NONE,
            Optional.empty(),
            List.of(new DiscordMessage.Attachment("\u00A7knotes.txt", "https://cdn.example.com/notes.txt", false)),
            List.of("\u00A7aWave")
        )).orElseThrow();

        assertEquals("<cSeer> lHello [file: knotes.txt] [sticker: aWave]", shown.getString());
    }

    @Test
    void shouldShowLongMessagesInFullWhenTheirEndIsHoveredOver() {
        final var text = "a".repeat(300);

        final var end = TextChunks.chunks(relay(message(text)).orElseThrow()).getLast();

        assertEquals(GameRelay.TOO_LONG, end.text());
        assertEquals(TextColor.fromLegacyFormat(TextColours.HOVERABLE), end.style().getColor());
        assertEquals(text, hoverText(end));
    }

    @Test
    void shouldCutTheFullMessageDownToFitOnScreen() {
        final var end = TextChunks.chunks(relay(message("a".repeat(2000))).orElseThrow()).getLast();

        assertEquals(GameRelay.FULL_MESSAGE.length(), hoverText(end).length());
        assertTrue(hoverText(end).endsWith(GameRelay.TOO_LONG), "Should end with " + GameRelay.TOO_LONG);
    }

    @Test
    void shouldNotShowMessagesThatFitInAHover() {
        final var chunks = TextChunks.chunks(relay(message("Hello")).orElseThrow());

        assertNull(chunks.getLast().style().getHoverEvent());
    }

    @Test
    void shouldMarkRepliesWithTheReplyFormat() {
        final var reply = new DiscordMessage.Reply("Bob", "bob", "What's **up**?", Mentions.NONE);

        final var chunks = TextChunks.chunks(relay(withReply(reply)).orElseThrow());

        assertEquals("➥", chunks.getFirst().text());
        assertEquals("Bob: What's up?", hoverText(chunks.getFirst()));
    }

    @Test
    void shouldNotMarkMessagesThatArentReplies() {
        assertEquals(PREFIX + "Hello", relay(message("Hello")).orElseThrow().getString());
    }

    @Test
    void shouldCutLongRepliesDown() {
        final var reply = new DiscordMessage.Reply("Bob", "bob", "a".repeat(300), Mentions.NONE);

        final var marker = TextChunks.chunks(relay(withReply(reply)).orElseThrow()).getFirst();

        assertEquals("Bob: ".length() + GameRelay.REPLY.length(), hoverText(marker).length());
    }

    @Test
    void shouldLinkAttachments() {
        final var shown = relay(withAttachments(
            "Look",
            new DiscordMessage.Attachment("screenshot.png", "https://cdn.example.com/screenshot.png", true),
            new DiscordMessage.Attachment("notes.txt", "https://cdn.example.com/notes.txt", false)
        )).orElseThrow();

        assertEquals(PREFIX + "Look [image] [file: notes.txt]", shown.getString());
        assertEquals(
            TextColor.fromLegacyFormat(TextColours.CLICKABLE),
            TextChunks.chunks(shown).getLast().style().getColor()
        );
        assertEquals(
            List.of("https://cdn.example.com/screenshot.png", "https://cdn.example.com/notes.txt"),
            links(shown)
        );
    }

    @Test
    void shouldShowMessagesThatAreOnlyAnAttachment() {
        final var shown = relay(withAttachments(
            "",
            new DiscordMessage.Attachment("screenshot.png", "https://cdn.example.com/screenshot.png", true)
        )).orElseThrow();

        assertEquals(PREFIX + "[image]", shown.getString());
    }

    @Test
    void shouldShowStickersByName() {
        final var shown = relay(new DiscordMessage(
            "Seer", "seeruk", "Hi", Mentions.NONE, Optional.empty(), List.of(), List.of("Wave")
        )).orElseThrow();

        assertEquals(PREFIX + "Hi [sticker: Wave]", shown.getString());
    }

    @Test
    void shouldShowNothingForMessagesWithNothingToShow() {
        assertEquals(Optional.empty(), relay(message("")));
    }

    private static Optional<Component> relay(DiscordMessage message) {
        return GameRelay.message(message, FORMATS);
    }

    private static DiscordMessage message(String content) {
        return new DiscordMessage("Seer", "seeruk", content, Mentions.NONE, Optional.empty(), List.of(), List.of());
    }

    private static DiscordMessage withReply(DiscordMessage.Reply reply) {
        return new DiscordMessage(
            "Seer", "seeruk", "Not much", Mentions.NONE, Optional.of(reply), List.of(), List.of()
        );
    }

    private static DiscordMessage withAttachments(String content, DiscordMessage.Attachment... attachments) {
        return new DiscordMessage(
            "Seer", "seeruk", content, Mentions.NONE, Optional.empty(), List.of(attachments), List.of()
        );
    }

    // The message, without the format around it
    private static String messageText(Component shown) {
        return shown.getString().substring(PREFIX.length());
    }

    // What's shown when hovering over a piece of text
    private static String hoverText(Chunk chunk) {
        return ((HoverEvent.ShowText) chunk.style().getHoverEvent()).value().getString();
    }

    private static List<String> links(Component shown) {
        return TextChunks.chunks(shown).stream()
            .map(chunk -> chunk.style().getClickEvent())
            .filter(event -> event instanceof ClickEvent.OpenUrl)
            .map(event -> ((ClickEvent.OpenUrl) event).uri())
            .map(URI::toString)
            .distinct()
            .toList();
    }
}
