package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.config.CommandsConfig;
import dev.seeruk.bundle.discord.config.DiscordConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static dev.seeruk.bundle.discord.SeersDiscord.LOGGER;

/**
 * The connection to Discord, as the configured bot.
 */
public final class DiscordBot {
    /**
     * All permissions required by the bot.
     */
    static final EnumSet<Permission> PERMISSIONS = EnumSet.of(
        Permission.VIEW_CHANNEL,
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MANAGE_WEBHOOKS
    );

    // What the bot's told about by Discord: messages in servers, and what's in them. Message
    // Content is a privileged intent, so it also has to be turned on in the developer portal.
    private static final EnumSet<GatewayIntent> INTENTS = EnumSet.of(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT
    );

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    // Discord doesn't allow "Discord" in a webhook's name
    private static final String WEBHOOK_NAME = "Minecraft Chat";

    private static final Object LOCK = new Object();

    // The current connection, if there is one, and whether the server has stopped since it was
    // started (so a login that finishes late is shut down straight away). Guarded by LOCK.
    private static JDA jda;
    private static boolean stopped;

    // The webhook messages are sent through, once the bot's ready. Guarded by LOCK.
    private static Webhook webhook;

    // Messages sent while there's no webhook yet (i.e. while the bot's logging in, or replacing a
    // deleted webhook), to send once there is, and how many didn't fit. Only set while waiting for
    // a webhook, and capped, in case that takes a while. Guarded by LOCK.
    private static final int MAX_PENDING = 50;
    private static List<WebhookMessage> pending;
    private static int dropped;

    private DiscordBot() {}

    /**
     * Logs in to Discord in the background, if the config has a bot and a channel.
     *
     * @param config   The mod's config.
     * @param received Given each message sent in the bot's channel, on one of JDA's threads.
     * @param commands Answers the bot's slash commands, called on one of JDA's threads.
     */
    public static void start(
        DiscordConfig config,
        Consumer<DiscordMessage> received,
        DiscordListener.CommandHandler commands
    ) {
        if (config.botToken().isBlank() || config.channelId().isBlank()) {
            LOGGER.info("Could not start, as bot_token and channel_id aren't both set in its config.yml");
            return;
        }

        // Messages sent while logging in (e.g. the server starting) wait for the webhook
        synchronized (LOCK) {
            stopped = false;
            pending = new ArrayList<>();
            dropped = 0;
        }

        Thread.ofVirtual().name("seers-discord-login").start(() -> login(config, received, commands));
    }

    /**
     * Disconnects from Discord, waiting a few seconds for anything still being sent.
     */
    public static void stop() {
        final JDA current;
        synchronized (LOCK) {
            stopped = true;
            current = jda;
            jda = null;
            webhook = null;
            pending = null;
        }

        // If the bot was never connected, we're done!
        if (current == null) {
            return;
        }

        // Try to shut down gracefully
        current.shutdown();

        try {
            if (!current.awaitShutdown(SHUTDOWN_TIMEOUT)) {
                // If we can't shut down gracefully, force shutdown
                current.shutdownNow();
            }
        } catch (InterruptedException e) {
            current.shutdownNow();
            Thread.currentThread().interrupt();
        }

        closeHttpClient(current);
    }

    private static void login(
        DiscordConfig config,
        Consumer<DiscordMessage> received,
        DiscordListener.CommandHandler commands
    ) {
        final JDA connected;
        try {
            connected = JDABuilder.createLight(config.botToken(), INTENTS)
                .addEventListeners(new DiscordListener(
                    config.channelId(),
                    received,
                    commands,
                    config.discord().commands().publicReplies()
                ))
                .setEnableShutdownHook(false) // The server controls the shutdown
                .build();
        } catch (InvalidTokenException e) {
            LOGGER.error("Couldn't log in to Discord, as bot_token is invalid");
            ready(null, Optional.empty());
            return;
        } catch (RuntimeException e) {
            LOGGER.error("Couldn't log in to Discord", e);
            ready(null, Optional.empty());
            return;
        }

        synchronized (LOCK) {
            if (stopped) {
                connected.shutdownNow();
                closeHttpClient(connected);
                return;
            }
            jda = connected;
        }

        try {
            connected.awaitReady();
        } catch (InterruptedException | IllegalStateException e) {
            // Stopped before it was ready, either by the server stopping, or by Discord refusing the
            // connection (which the listener logs)
            ready(connected, Optional.empty());
            return;
        }

        final var channel = usableChannel(connected, config.channelId());
        channel.ifPresent(found -> addCommands(found, config.discord().commands()));
        ready(connected, channel.flatMap(found -> findOrCreateWebhook(connected, found)));
    }

    /**
     * Adds the bot's slash commands that are turned on to its server, replacing whatever it had
     * before, so ones that have been turned off are removed. They're added to its server rather
     * than everywhere, as Discord shows those straight away.
     */
    private static void addCommands(TextChannel channel, CommandsConfig config) {
        channel.getGuild().updateCommands()
            .addCommands(DiscordCommands.enabled(config))
            .queue(null, error -> LOGGER.warn("Couldn't add the bot's commands in Discord", error));
    }

    /**
     * Sends the message through the bot's webhook, if it's connected, or once there's a webhook, if
     * the bot's logging in, or replacing a deleted one. Mentions are turned off, so players can't
     * ping anyone (e.g. with {@code @everyone}).
     */
    public static void send(WebhookMessage message) {
        final Webhook current;
        synchronized (LOCK) {
            if (pending != null) {
                addPending(message);
                return;
            }
            current = webhook;
        }

        if (current == null) {
            return;
        }

        // Discord refuses a message with empty content, so one that's only embeds is sent as such
        final var action = message.content().isEmpty()
            ? current.sendMessageEmbeds(message.embeds())
            : current.sendMessage(message.content()).setEmbeds(message.embeds());

        // A null username and avatar mean the webhook's own are used
        action.setUsername(message.username())
            .setAvatarUrl(message.avatarUrl())
            .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
            .queue(
                null, error -> {
                    if (error instanceof ErrorResponseException response
                        && response.getErrorResponse() == ErrorResponse.UNKNOWN_WEBHOOK) {
                        replaceWebhook(current, message);
                    } else {
                        LOGGER.warn("Couldn't send a message to Discord", error);
                    }
                }
            );
    }

    /**
     * The webhook the bot made in the channel before, or a new one if there isn't one.
     * Logs what went wrong if neither works.
     */
    private static Optional<Webhook> findOrCreateWebhook(JDA connected, TextChannel channel) {
        try {
            final var self = connected.getSelfUser().getIdLong();
            return Optional.of(channel.retrieveWebhooks().complete().stream()
                .filter(existing -> existing.getOwnerAsUser() != null && existing.getOwnerAsUser().getIdLong() == self)
                .filter(existing -> existing.getToken() != null)
                .findFirst()
                .orElseGet(() -> channel.createWebhook(WEBHOOK_NAME).complete()));
        } catch (RuntimeException e) {
            // If we're signalling some kind of change to our JDA instance (e.g. shutting down),
            // then it's not worth logging an error.
            if (isCurrent(connected)) {
                LOGGER.error(
                    "Couldn't set up a webhook in #{}, so messages can't be sent to Discord",
                    channel.getName(),
                    e
                );
            }
            return Optional.empty();
        }
    }

    /**
     * Replaces a stale webhook reference with a new one. How can one become stale? If the webhook
     * has been deleted in Discord, but we still have a reference to it here.
     *
     * <p>Messages sent while it's being replaced wait in {@link #pending}, and are sent once it's
     * been successfully replaced.
     *
     * @param deleted The webhook that's been deleted.
     * @param unsent  The message that couldn't be sent through it.
     */
    private static void replaceWebhook(Webhook deleted, WebhookMessage unsent) {
        final Webhook current;
        synchronized (LOCK) {
            current = webhook;
            if (current == deleted) {
                webhook = null;
                pending = new ArrayList<>();
                dropped = 0;
            }
            if (pending != null) {
                addPending(unsent);
            }
        }

        if (current != deleted) {
            // If it's already been replaced, send the unsent message through the new webhook. If
            // there isn't a webhook in place, it's being replaced (so it's waiting), or the
            // server's stopped.
            if (current != null) {
                send(unsent);
            }
            return;
        }

        LOGGER.warn("The bot's webhook in #{} was deleted. Creating a new one", deleted.getChannel().getName());

        final var connected = deleted.getJDA();
        final var channel = deleted.getChannel().asTextChannel();

        Thread.ofVirtual().name("seers-discord-webhook").start(() ->
            ready(connected, findOrCreateWebhook(connected, channel)));
    }

    /**
     * Sets the webhook messages are sent through, or none if there isn't one, and sends (or drops)
     * the messages that were waiting for it.
     *
     * @param connected The connection the webhook belongs to, or null if the bot couldn't log in.
     *                  If the server's stopped since it was made, nothing's changed or sent.
     * @param found     The webhook, if there is one.
     */
    private static void ready(JDA connected, Optional<Webhook> found) {
        // Set together, so nothing sent in between is lost
        final List<WebhookMessage> waiting;
        final int overflow;
        synchronized (LOCK) {
            if (jda != connected || pending == null) {
                return;
            }
            waiting = pending;
            overflow = dropped;
            pending = null;
            webhook = found.orElse(null);
        }

        if (found.isEmpty()) {
            if (waiting.size() + overflow > 0) {
                LOGGER.warn("{} messages weren't sent to Discord, as there's no webhook", waiting.size() + overflow);
            }
            return;
        }
        if (overflow > 0) {
            LOGGER.warn("{} messages weren't sent to Discord, as too many were waiting", overflow);
        }

        waiting.forEach(DiscordBot::send);
    }

    /**
     * Adds the message to those waiting for a webhook, or counts it as dropped if there are already
     * too many.
     */
    private static void addPending(WebhookMessage message) {
        if (pending.size() < MAX_PENDING) {
            pending.add(message);
        } else {
            dropped++;
        }
    }

    /**
     * Compares the given JDA instance with the bot's current one, returning true if they match.
     * The instance is modified when stopping and starting the server, so this can detect that a
     * change is occurring.
     */
    private static boolean isCurrent(JDA connected) {
        synchronized (LOCK) {
            return jda == connected;
        }
    }

    /**
     * Stops the HTTP client JDA made, which JDA can leave running when it shuts down. Its threads
     * can keep the server's process running for up to a minute after the server stops.
     *
     * <p>See JDA's
     * <a href="https://jda.wiki/using-jda/troubleshooting/#shutdown-but-the-process-doesnt-exit">
     * troubleshooting guide</a> for more information.
     */
    private static void closeHttpClient(JDA jda) {
        final var http = jda.getHttpClient();
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }

    /**
     * The bot's channel, if the bot can use it. Logs what's wrong if it can't, as that's the most
     * likely thing to need fixing when setting up.
     */
    private static Optional<TextChannel> usableChannel(JDA jda, String channelId) {
        final var channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            LOGGER.error(
                "Connected to Discord as {}, but couldn't find a text channel with ID {} (check channel_id, and that the bot is a member of that channel's server)",
                jda.getSelfUser().getName(),
                channelId
            );
            return Optional.empty();
        }

        final var missing = EnumSet.copyOf(PERMISSIONS);
        missing.removeAll(channel.getGuild().getSelfMember().getPermissions(channel));
        if (!missing.isEmpty()) {
            LOGGER.error(
                "Connected to Discord as {}, but the bot is missing permissions in #{}: {}",
                jda.getSelfUser().getName(),
                channel.getName(),
                missing.stream().map(Permission::getName).toList()
            );
            return Optional.empty();
        }

        LOGGER.info("Connected to Discord as {}, in #{}", jda.getSelfUser().getName(), channel.getName());
        return Optional.of(channel);
    }
}
