package dev.seeruk.bundle.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

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

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    // Discord doesn't allow "Discord" in a webhook's name
    private static final String WEBHOOK_NAME = "Minecraft Chat";

    private static final Object LOCK = new Object();

    // The current connection, if there is one, and whether the server has stopped since it was
    // started (so a login that finishes late is shut down straight away). Guarded by LOCK.
    private static JDA jda;
    private static boolean stopped;

    // The webhook messages are sent through, once the bot's ready. Volatile, as it's used from the
    // server thread, and set from the login thread.
    private static volatile Webhook webhook;

    private DiscordBot() {}

    /**
     * Logs in to Discord in the background, if the config has a bot and a channel.
     */
    public static void start(DiscordConfig config) {
        if (config.botToken().isBlank() || config.channelId().isBlank()) {
            LOGGER.info("Could not start, as bot_token and channel_id aren't both set in its config.yml");
            return;
        }

        synchronized (LOCK) {
            stopped = false;
        }

        Thread.ofVirtual().name("seers-discord-login").start(() -> login(config));
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

    private static void login(DiscordConfig config) {
        final JDA connected;
        try {
            connected = JDABuilder.createLight(config.botToken(), List.of())
                .setEnableShutdownHook(false) // The server controls the shutdown
                .build();
        } catch (InvalidTokenException e) {
            LOGGER.error("Couldn't log in to Discord, as bot_token is invalid");
            return;
        } catch (RuntimeException e) {
            LOGGER.error("Couldn't log in to Discord", e);
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
            // Stopped before it was ready
            return;
        }

        usableChannel(connected, config.channelId()).ifPresent(channel -> useWebhook(connected, channel));
    }

    /**
     * Sends the message through the bot's webhook, if it's connected. Mentions are turned off, so
     * players can't ping anyone (e.g. with {@code @everyone}).
     */
    public static void send(WebhookMessage message) {
        final var current = webhook;
        if (current == null) {
            return;
        }

        current.sendMessage(message.content())
            .setUsername(message.username())
            .setAvatarUrl(message.avatarUrl())
            .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
            .queue(null, error -> LOGGER.warn("Couldn't send a message to Discord", error));
    }

    /**
     * Uses the webhook the bot made in the channel before, or makes one.
     */
    private static void useWebhook(JDA connected, TextChannel channel) {
        final Webhook found;
        try {
            final var self = connected.getSelfUser().getIdLong();
            found = channel.retrieveWebhooks().complete().stream()
                .filter(existing -> existing.getOwnerAsUser() != null && existing.getOwnerAsUser().getIdLong() == self)
                .filter(existing -> existing.getToken() != null)
                .findFirst()
                .orElseGet(() -> channel.createWebhook(WEBHOOK_NAME).complete());
        } catch (RuntimeException e) {
            LOGGER.error("Couldn't set up a webhook in #{}, so messages can't be sent to Discord", channel.getName(), e);
            return;
        }

        synchronized (LOCK) {
            // Unless the server's stopped since
            if (jda == connected) {
                webhook = found;
            }
        }
    }

    /**
     * Stops the HTTP client JDA made, which JDA can leave running when it shuts down. Its threads
     * can keep the server's process running for up to a minute after the server stops.
     *
     * <p>See <a href="https://jda.wiki/using-jda/troubleshooting/#shutdown-but-the-process-doesnt-exit">
     * this page</a> for more information.
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
