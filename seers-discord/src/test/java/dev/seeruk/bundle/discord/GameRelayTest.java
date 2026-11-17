package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.testing.TextChunks;
import dev.seeruk.bundle.core.testing.TextChunks.Chunk;
import dev.seeruk.bundle.discord.markdown.Mentions;
import dev.seeruk.bundle.text.TextComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TextChunks.chunk;
import static org.junit.jupiter.api.Assertions.*;

public class GameRelayTest {
    private static final DiscordConfig.GameFormats FORMATS = new DiscordConfig.GameFormats(
        // The default format
        new GameTemplate("<blue>[Discord]</blue> <aqua><hover '@${username}'>${display_name}</hover></aqua>: "
            + "${message}")
    );

    // What's shown before each message with FORMATS
    private static final String PREFIX = "[Discord] Seer: ";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void shouldShowMessagesInTheFormat() {
        final var chunks = TextChunks.chunks(relay(message("Hello")).orElseThrow());

        assertEquals(List.of("[Discord]", " ", "Seer", ": ", "Hello"), chunks.stream().map(Chunk::text).toList());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), chunks.get(0).style().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), chunks.get(2).style().getColor());
        assertEquals(Style.EMPTY, chunks.get(4).style());
    }

    @Test
    void shouldShowTheSendersUsernameWhenTheirNameIsHoveredOver() {
        final var name = TextChunks.chunks(relay(message("Hello")).orElseThrow()).get(2);

        assertEquals("@seeruk", ((HoverEvent.ShowText) name.style().getHoverEvent()).value().getString());
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

        assertEquals("[Discord] Seer :smile:: Hello", shown.getString());
    }

    @Test
    void shouldCutLongMessagesDown() {
        final var text = messageText(relay(message("a".repeat(300))).orElseThrow());

        assertEquals(GameRelay.MAX_MESSAGE_LENGTH, text.length());
        assertTrue(text.endsWith(GameRelay.TOO_LONG), "Should end with " + GameRelay.TOO_LONG);
    }

    @Test
    void shouldNotCutMessagesThatFit() {
        final var fits = "a".repeat(GameRelay.MAX_MESSAGE_LENGTH);

        assertEquals(fits, messageText(relay(message(fits)).orElseThrow()));
    }

    @Test
    void shouldCountEmojiAsOneCharacterWhenCutting() {
        final var text = messageText(relay(message("😀".repeat(300))).orElseThrow());

        assertEquals(GameRelay.MAX_MESSAGE_LENGTH, TextComponents.length(text));
    }

    @Test
    void shouldCutMessagesWithTooManyLinesDown() {
        final var text = messageText(relay(message("Line\n".repeat(20))).orElseThrow());

        assertEquals("Line\n".repeat(GameRelay.MAX_LINES - 1) + "Line" + GameRelay.TOO_LONG, text);
    }

    @Test
    void shouldNotCutMessagesWithLinesThatFit() {
        final var fits = String.join("\n", Collections.nCopies(GameRelay.MAX_LINES, "Line"));

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

        assertEquals("[Discord] cSeer: lHello [file: knotes.txt] [sticker: aWave]", shown.getString());
    }

    @Test
    void shouldShowWhatARepliedToMessageSaidWhenHoveredOver() {
        final var reply = new DiscordMessage.Reply("Bob", "What's **up**?", Mentions.NONE);

        final var hover = hover(relay(withReply(reply)).orElseThrow());

        assertEquals("Replying to Bob:\nWhat's up?", hover.getString());
    }

    @Test
    void shouldCutLongRepliesDown() {
        final var reply = new DiscordMessage.Reply("Bob", "a".repeat(300), Mentions.NONE);

        final var hover = hover(relay(withReply(reply)).orElseThrow());

        assertEquals("Replying to Bob:\n".length() + GameRelay.MAX_REPLY_LENGTH, hover.getString().length());
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

    // What's shown when hovering over the message itself
    private static Component hover(Component shown) {
        final var style = TextChunks.chunks(shown).getLast().style();
        return ((HoverEvent.ShowText) style.getHoverEvent()).value();
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
