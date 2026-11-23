package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.actionbar.ActionBarMessages;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import dev.seeruk.bundle.resourcezones.command.ResourceZonesCommand;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditStatus;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditor;
import dev.seeruk.bundle.resourcezones.export.ZoneExports;
import dev.seeruk.bundle.resourcezones.particles.EditMarkers;
import dev.seeruk.bundle.resourcezones.particles.ZoneParticles;
import dev.seeruk.bundle.resourcezones.reset.ZoneResets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SeersResourceZones implements ModInitializer {
    public static final String MOD_ID = "seers-resource-zones";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, ResourceZonesConfig.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            ResourceZonesCommand.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> ZoneParticles.showAll(server, config.particles()));

        final var notice = config.notice().format(Map.of());
        ActionBarMessages.register(id("notice"), player -> ZoneNotice.message(player, notice));

        // Editing zones
        final var edit = config.edit();
        ServerTickEvents.END_SERVER_TICK.register(ZoneEditor::markAll);
        ServerTickEvents.END_SERVER_TICK.register(server -> EditMarkers.showAll(server, edit.markers()));
        ActionBarMessages.register(id("edit_status"), player -> ZoneEditStatus.message(player, edit.status()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ZoneEditor.forget(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ZoneEditor.clear());

        // Exporting zones (automatically)
        final var exportFolder = config.export().folder();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ZoneExports.exportAll(server, exportFolder));
        ResourceZoneEvents.CHANGED.register((server, dimension) -> ZoneExports.export(server, exportFolder, dimension));

        // Zone resets, found as levels load, before chunks are, and recorded on server started
        final var reset = config.reset();
        ServerLevelEvents.LOAD.register(ZoneResets::levelLoaded);
        ServerLifecycleEvents.SERVER_STARTED.register(ZoneResets::started);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ZoneResets.clear());
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> ZoneResets.chunkLoaded(level, chunk));
        ResourceZoneEvents.CHANGED.register(ZoneResets::refreshGenerated);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ZoneResets.rejoined(
            handler.player,
            reset
        ));

        // Who's in a zone, before stats count their time there
        ServerTickEvents.END_SERVER_TICK.register(ZonePresence::updateAll);
        ResourceZoneEvents.CHANGED.register(ZonePresence::zonesChanged);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ZonePresence.forget(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ZonePresence.clear());

        ServerLifecycleEvents.SERVER_STARTED.register(ResourceZonesStats::ensureObjectives);
        ServerTickEvents.END_SERVER_TICK.register(ResourceZonesStats::recordZoneTick);
        ResourceZoneEvents.PLAYER_ENTERED.register(ResourceZonesStats::recordZoneEntered);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
            ResourceZonesStats.recordBlockMined(level, player, pos));

        LOGGER.info("Initialized");
    }
}
