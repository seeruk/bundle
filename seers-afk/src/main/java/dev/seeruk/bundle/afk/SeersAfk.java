package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.api.AfkEvents;
import dev.seeruk.bundle.afk.command.AfkCommand;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.core.tablist.TabList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersAfk implements ModInitializer {
    public static final String MOD_ID = "seers-afk";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, AfkConfig.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            AfkCommand.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> AfkStatus.updateAll(server, config.timeoutTicks()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> AfkStatus.respawned(newPlayer));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AfkStatus.forget(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> AfkStatus.clear());

        TabList.NAME.register((player, name) -> AfkTabList.name(player, name, config.tabListName()));
        TabList.ORDER.register(AfkTabList::order);

        ServerLifecycleEvents.SERVER_STARTED.register(AfkStats::ensureObjectives);
        ServerTickEvents.END_SERVER_TICK.register(AfkStats::recordAfkTick);

        AfkEvents.STATUS_CHANGED.register((player, isAfk, reason) -> {
            AfkMessages.announce(player, isAfk, config.messages());
            TabList.refresh(player);
            player.level().updateSleepingPlayerList();
        });

        LOGGER.info("Initialized");
    }
}
