package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.markdown.Mentions;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.CloseCode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static dev.seeruk.bundle.discord.SeersDiscord.LOGGER;

/**
 * Listens for messages people send in the bot's channel, and passes them on, as
 * {@link DiscordMessage}s. Messages from bots and webhooks are left out, which includes the ones
 * the bot relays from the game, so they don't come straight back. Also says what to fix if Discord
 * refuses the bot's connection because of the intents it asks for.
 *
 * <p>JDA calls this on its own threads, so whatever messages are passed to needs to hand them to
 * the right thread itself.
 */
final class DiscordListener extends ListenerAdapter {
    private final long channelId;
    private final Consumer<DiscordMessage> received;

    /**
     * @param channelId The bot's channel, which is the only one listened to.
     * @param received  Given each message.
     */
    DiscordListener(String channelId, Consumer<DiscordMessage> received) {
        this.channelId = Long.parseLong(channelId);
        this.received = received;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        final var message = event.getMessage();
        if (event.getChannel().getIdLong() != channelId
            || event.isWebhookMessage()
            || event.getAuthor().isBot()
            || event.getAuthor().isSystem()
            // e.g. "Seer pinned a message", which Discord shows as a message
            || message.getType().isSystem()) {
            return;
        }

        received.accept(new DiscordMessage(
            name(message.getMember(), message.getAuthor()),
            message.getAuthor().getName(),
            message.getContentRaw(),
            mentions(message),
            // This can be missing even for a reply, e.g. if the message it replied to was deleted
            Optional.ofNullable(message.getReferencedMessage()).map(DiscordListener::reply),
            message.getAttachments().stream()
                .map(attachment -> new DiscordMessage.Attachment(
                    attachment.getFileName(),
                    attachment.getUrl(),
                    attachment.isImage()
                ))
                .toList(),
            message.getStickers().stream().map(StickerItem::getName).toList()
        ));
    }

    /**
     * Discord refuses the connection if the bot asks for Message Content without it being turned
     * on for the bot, which JDA only logs as a close code, so this says how to fix it.
     */
    @Override
    public void onShutdown(ShutdownEvent event) {
        if (event.getCloseCode() == CloseCode.DISALLOWED_INTENTS) {
            LOGGER.error(
                "Discord refused the bot's connection, as it can't read messages yet. Turn on Message "
                    + "Content Intent on the Bot page of the bot's application in the Discord developer "
                    + "portal, then restart the server"
            );
        }
    }

    private static DiscordMessage.Reply reply(Message replied) {
        return new DiscordMessage.Reply(
            name(replied.getMember(), replied.getAuthor()),
            replied.getAuthor().getName(),
            replied.getContentRaw(),
            mentions(replied)
        );
    }

    /**
     * Returns what someone's called in the bot's server, i.e. their nickname if they have one, or
     * else their Discord name. Discord doesn't always send who they are in the server (e.g. for the
     * author of a message that's been replied to), so their Discord name is the fallback.
     */
    private static String name(Member member, User user) {
        return member != null ? member.getEffectiveName() : user.getEffectiveName();
    }

    /**
     * Returns the names of what the message mentions, as the message's text only has their IDs.
     */
    private static Mentions mentions(Message message) {
        final var mentioned = message.getMentions();
        final var members = byId(mentioned.getMembers(), Member::getEffectiveName);

        return new Mentions(
            // Mentioned users are named as they are in the server, where Discord says
            byId(mentioned.getUsers(), user -> members.getOrDefault(user.getIdLong(), user.getEffectiveName())),
            byId(mentioned.getRoles(), Role::getName),
            byId(mentioned.getChannels(), GuildChannel::getName)
        );
    }

    private static <T extends ISnowflake> Map<Long, String> byId(List<T> things, Function<T, String> name) {
        return things.stream().collect(Collectors.toMap(ISnowflake::getIdLong, name, (first, second) -> first));
    }
}
