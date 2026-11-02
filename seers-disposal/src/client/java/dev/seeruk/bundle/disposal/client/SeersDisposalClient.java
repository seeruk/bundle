package dev.seeruk.bundle.disposal.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static dev.seeruk.bundle.disposal.SeersDisposal.LOGGER;
import static dev.seeruk.bundle.disposal.SeersDisposal.id;

/**
 * An optional client mod for Seer's Disposal. Simply provides a key binding that runs the server's
 * {@code /seers-disposal:disposal} command, so players don't always have to type it.
 *
 * <p>The key is unbound by default, just to avoid clashing with other mods potentially.
 *
 * <p>The client mod is harmless on servers that don't have Disposal, as it only sends the command
 * if the server actually has it. This mod is not required to play on servers with Disposal.
 */
public class SeersDisposalClient implements ClientModInitializer {
    private static final String COMMAND = id("disposal").toString();

    public static final String OPEN_KEY = "key.seers-disposal.open";

    private static final KeyMapping OPEN = new KeyMapping(
        OPEN_KEY,
        InputConstants.UNKNOWN.getValue(),
        KeyMapping.Category.INVENTORY
    );

    private static void onEndClientTick(Minecraft minecraft) {
        while (OPEN.consumeClick()) {
            doOpen(minecraft);
        }
    }

    private static void doOpen(Minecraft minecraft) {
        final var player = minecraft.player;
        final var connection = minecraft.getConnection();
        if (player == null || connection == null) {
            return;
        }

        if (connection.getCommands().getRoot().getChild(COMMAND) != null) {
            // We always use the aliased, fully-qualified command, as it's less likely to collide
            // with another mod, and should always be present if the server has Disposal.
            connection.sendCommand(COMMAND);
            return;
        }

        player.sendOverlayMessage(Component.literal(
            "This server doesn't have Seer's Disposal installed"));
    }

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN);

        ClientTickEvents.END_CLIENT_TICK.register(SeersDisposalClient::onEndClientTick);

        LOGGER.info("Initialized client");
    }
}
