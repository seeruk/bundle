package dev.seeruk.bundle.resourcezones.edit;

import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;

public class ZoneEditStatusGameTest {
    private static final TextTemplate STATUS = TextTemplate.parse("${mode} +${added} -${removed}");

    @GameTest
    public void showsTheModeAndChanges(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, new ChunkPos(7500, 0));
        ZoneEditor.start(player, EditMode.ADD);
        ZoneEditor.mark(player);

        helper.assertValueEqual(
            ZoneEditStatus.message(player, STATUS).map(Component::getString),
            Optional.of("adding +1 -0"),
            "status"
        );
        helper.succeed();
    }

    @GameTest
    public void showsNothingWithoutAnEdit(GameTestHelper helper) {
        helper.assertValueEqual(ZoneEditStatus.message(player(helper), STATUS), Optional.empty(), "status");
        helper.succeed();
    }
}
