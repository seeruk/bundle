package dev.seeruk.bundle.core.command;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestHelper;

public class NamespacedCommandsGameTest implements ModInitializer {
    private static final String MOD_ID = "seers-core-gametest";
    private static final String NAME = "namespaced-commands-test";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            NamespacedCommands.register(
                dispatcher, MOD_ID, NAME,
                name -> Commands.literal(name).executes(context -> 1)
            );
        });
    }

    @GameTest
    public void registersWithNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(root.getChild(NAME) != null, "Command not registered as " + NAME);
        helper.assertTrue(
            root.getChild(MOD_ID + ":" + NAME) != null,
            "Command not registered as " + MOD_ID + ":" + NAME
        );

        helper.succeed();
    }
}
