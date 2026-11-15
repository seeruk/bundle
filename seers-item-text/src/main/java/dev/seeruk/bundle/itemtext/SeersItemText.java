package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.core.action.CustomActions;
import dev.seeruk.bundle.itemtext.api.ItemLoreEvents;
import dev.seeruk.bundle.itemtext.api.ItemNameEvents;
import dev.seeruk.bundle.itemtext.command.ItemLoreCommand;
import dev.seeruk.bundle.itemtext.command.ItemNameCommand;
import dev.seeruk.bundle.itemtext.dialog.LoreDialog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersItemText implements ModInitializer {
    public static final String MOD_ID = "seers-item-text";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            ItemLoreCommand.register(dispatcher);
            ItemNameCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(ItemTextStats::ensureObjectives);
        ItemNameEvents.CHANGED.register(ItemTextStats::recordNameWritten);
        ItemLoreEvents.CHANGED.register(ItemTextStats::recordLoreWritten);

        CustomActions.register(LoreDialog.SAVE_ACTION, LoreDialog::save);

        LOGGER.info("Initialized");
    }
}
