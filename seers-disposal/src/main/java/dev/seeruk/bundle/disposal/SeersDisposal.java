package dev.seeruk.bundle.disposal;

import dev.seeruk.bundle.disposal.api.DisposalEvents;
import dev.seeruk.bundle.disposal.command.DisposalCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersDisposal implements ModInitializer {
    public static final String MOD_ID = "seers-disposal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            DisposalCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(DisposalStats::ensureObjectives);
        DisposalEvents.DISPOSED.register(DisposalStats::recordDisposed);

        LOGGER.info("Initialized");
    }
}
