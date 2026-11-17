package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.markdown.Mentions;

import java.util.List;
import java.util.Optional;

/**
 * A message from Discord, with everything needed to show it in game.
 *
 * @param displayName What the sender's called in the bot's server, e.g. their nickname.
 * @param username    The sender's Discord username.
 * @param content     The message, in Discord's Markdown.
 * @param mentions    The names of what the message mentions.
 * @param reply       The message it replies to, if it's a reply.
 * @param attachments Files attached to it, e.g. images.
 * @param stickers    The names of any stickers sent with it.
 */
public record DiscordMessage(
    String displayName,
    String username,
    String content,
    Mentions mentions,
    Optional<Reply> reply,
    List<Attachment> attachments,
    List<String> stickers
) {
    /**
     * @param displayName What the replied-to message's sender is called in the bot's server.
     * @param username    The replied-to message's sender's Discord username.
     * @param content     The replied-to message, in Discord's Markdown.
     * @param mentions    The names of what the replied-to message mentions.
     */
    public record Reply(String displayName, String username, String content, Mentions mentions) {}

    /**
     * @param fileName The file's name, e.g. {@code screenshot.png}.
     * @param url      Where the file can be opened.
     * @param image    Whether it's an image.
     */
    public record Attachment(String fileName, String url, boolean image) {}
}
