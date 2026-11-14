package dev.seeruk.bundle.nicks;

import dev.seeruk.bundle.nicks.api.NicksEvents;
import dev.seeruk.bundle.nicks.command.NickCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.players.NameAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersNicks implements ModInitializer {
    public static final String MOD_ID = "seers-nicks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            NickCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(Nicknames::load);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Nicknames.clear());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            Nicknames.releaseUsername(server,
                new NameAndId(handler.player.getUUID(), handler.player.getGameProfile().name())));

        ServerLifecycleEvents.SERVER_STARTED.register(NicksStats::ensureObjectives);
        NicksEvents.CHANGED.register(NicksStats::recordNickChanged);

        LOGGER.info("Initialized");
    }
}
