package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.api.PlayerEvents;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.discord.config.DiscordConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

public class SeersDiscord implements ModInitializer {
    public static final String MOD_ID = "seers-discord";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, DiscordConfig.CODEC);

        final var events = config.discord().events();
        final var isAfkLoaded = FabricLoader.getInstance().isModLoaded("seers-afk");
        final Predicate<ServerPlayer> isAfk = isAfkLoaded ? AfkEventsBridge::isAfk : player -> false;

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            final var started = Instant.now();

            // Messages and commands from Discord arrive on JDA's threads, so they're handled on
            // the server's own
            DiscordBot.start(
                config,
                config.discord().server().avatarLocation(server.getServerDirectory(), serverIcon(server)),
                message -> server.execute(() -> broadcast(server, message, config.game())),
                (command, reply) -> {
                    // A stopping server won't run anything else on its thread, so it'd never reply
                    if (!server.isRunning()) {
                        reply.accept(DiscordCommands.stopping());
                        return;
                    }

                    server.execute(() -> reply.accept(DiscordCommands.reply(
                        command,
                        server,
                        Duration.between(started, Instant.now()),
                        isAfk,
                        config.discord().commands()
                    )));
                }
            );

            // Sent once the bot's logged in, which happens in the background
            DiscordRelay.serverStarted(events).ifPresent(DiscordBot::send);
        });

        // The bot only stops once the server has, so the server stopping is sent before it does
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
            DiscordRelay.serverStopped(events).ifPresent(DiscordBot::send));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DiscordBot.stop());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            DiscordRelay.joined(handler.player, events).ifPresent(DiscordBot::send));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            DiscordRelay.left(handler.player, events).ifPresent(DiscordBot::send));
        PlayerEvents.DEATH_MESSAGE.register((player, message) ->
            DiscordRelay.died(player, message, events).ifPresent(DiscordBot::send));

        // Advancements require some more specific handling. We don't want to fill the JDA queue
        // with tons of advancements if someone is granted tons of them at once. To handle that, we
        // batch them up, and send them at the end of the current tick.
        final var advancements = new AdvancementBatcher();
        PlayerEvents.ADVANCEMENT_MESSAGE.register((player, advancement, message) ->
            DiscordRelay.advancement(player, advancement, message, events)
                .ifPresent(embed -> advancements.add(player, embed)));
        ServerTickEvents.END_SERVER_TICK.register(server ->
            advancements.flush(events).forEach(DiscordBot::send));

        if (isAfkLoaded) {
            AfkEventsBridge.register(events);
        }

        if (FabricLoader.getInstance().isModLoaded("seers-resource-zones")) {
            ResourceZonesEventsBridge.register(events);
        }

        // Fired for messages players send with commands (e.g. /me) too
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, chat) ->
            DiscordRelay.message(message, chat, sender, config.discord()).ifPresent(DiscordBot::send));

        LOGGER.info("Initialized");
    }

    /**
     * Broadcasts a message from Discord in game, to every player (and the server's log), unless
     * there's nothing in it the game can show.
     */
    static void broadcast(MinecraftServer server, DiscordMessage message, DiscordConfig.GameSettings settings) {
        GameRelay.message(message, settings)
            .ifPresent(shown -> server.getPlayerList().broadcastSystemMessage(shown, false));
    }

    /**
     * Returns the server's icon, if it has one, i.e. the picture the server list shows for it: its
     * server-icon.png, or in single player, the world's icon.
     */
    private static Optional<Path> serverIcon(MinecraftServer server) {
        return Optional.of(server.getFile("server-icon.png"))
            .filter(Files::isRegularFile)
            .or(() -> server.getWorldScreenshotFile().filter(Files::isRegularFile));
    }
}
