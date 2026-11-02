package dev.seeruk.bundle.disposal.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

@SuppressWarnings("UnstableApiUsage")
public class SeersDisposalClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(@NonNull ClientGameTestContext context) {
        keyIsUnboundByDefault(context);
        keyNameIsTranslated(context);
        keyOpensDisposalScreen(context);
    }

    private void keyIsUnboundByDefault(ClientGameTestContext context) {
        final var defaultKey = context.computeOnClient(client -> openKey(client.options).getDefaultKey());

        if (!defaultKey.equals(InputConstants.UNKNOWN)) {
            throw new AssertionError("Expected the key to be unbound by default, but got " + defaultKey.getName());
        }
    }

    private void keyNameIsTranslated(ClientGameTestContext context) {
        final var name = context.computeOnClient(
            client -> Component.translatable(SeersDisposalClient.OPEN_KEY).getString());

        if (!name.equals("Open Disposal")) {
            throw new AssertionError("Expected the key's name to be 'Open Disposal', but got '" + name + "'");
        }
    }

    private void keyOpensDisposalScreen(ClientGameTestContext context) {
        // Grab this to restore it later, so we're not assuming anything.
        final var originalKey = context.computeOnClient(client ->
            openKey(client.options).saveString()
        );

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();

            context.runOnClient(client -> {
                openKey(client.options).setKey(InputConstants.getKey("key.keyboard.f24"));
                KeyMapping.resetMapping();
            });

            context.getInput().pressKey(SeersDisposalClientGameTest::openKey);

            context.waitFor(client ->
                client.gui.screen() instanceof ContainerScreen screen
                    && screen.getTitle().getString().equals("Disposal")
            );
        } finally {
            context.runOnClient(client -> {
                openKey(client.options).setKey(InputConstants.getKey(originalKey));
                KeyMapping.resetMapping();
            });
        }
    }

    private static KeyMapping openKey(Options options) {
        return Arrays.stream(options.keyMappings)
            .filter(mapping -> mapping.getName().equals(SeersDisposalClient.OPEN_KEY))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Key mapping " + SeersDisposalClient.OPEN_KEY + " isn't registered"));
    }
}
