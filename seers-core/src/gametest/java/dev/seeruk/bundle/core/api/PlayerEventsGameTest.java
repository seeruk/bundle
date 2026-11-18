package dev.seeruk.bundle.core.api;

import dev.seeruk.bundle.core.testing.EventLog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Objects;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class PlayerEventsGameTest implements ModInitializer {
    private record Announced(ServerPlayer player, AdvancementHolder advancement, Component message) {}

    private record Died(ServerPlayer player, Component message) {}

    private static final EventLog<Announced> ANNOUNCED = new EventLog<>();
    private static final EventLog<Died> DIED = new EventLog<>();

    @Override
    public void onInitialize() {
        PlayerEvents.ADVANCEMENT_MESSAGE.register((player, advancement, message) ->
            ANNOUNCED.record(new Announced(player, advancement, message)));
        PlayerEvents.DEATH_MESSAGE.register((player, message) -> DIED.record(new Died(player, message)));
    }

    @GameTest
    public void completingAnAdvancementFiresTheEventOnceWithItsAnnouncement(GameTestHelper helper) {
        final var player = player(helper);
        final var advancement = advancement(helper, "story/mine_stone");

        // Its only criterion, so this completes it, and granting it again does nothing
        player.getAdvancements().award(advancement, "get_stone");
        player.getAdvancements().award(advancement, "get_stone");

        final var announced = ANNOUNCED.matching(event -> event.player() == player);
        helper.assertValueEqual(announced.size(), 1, "announcements");
        helper.assertValueEqual(announced.getFirst().advancement(), advancement, "advancement");
        helper.assertValueEqual(
            announced.getFirst().message().getString(),
            player.getGameProfile().name() + " has made the advancement [Stone Age]",
            "announcement"
        );
        helper.succeed();
    }

    @GameTest
    public void completingAnAdvancementThatIsNotAnnouncedDoesNotFireTheEvent(GameTestHelper helper) {
        final var player = player(helper);

        // Root advancements (e.g. "Minecraft") have a display, but aren't announced in chat
        player.getAdvancements().award(advancement(helper, "story/root"), "crafting_table");

        helper.assertValueEqual(ANNOUNCED.matching(event -> event.player() == player).size(), 0, "announcements");
        helper.succeed();
    }

    @GameTest
    public void completingAnAdvancementDoesNotFireTheEventWithAdvancementMessagesOff(GameTestHelper helper) {
        final var player = player(helper);
        final var rules = helper.getLevel().getGameRules();
        final var server = helper.getLevel().getServer();

        rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);
        try {
            player.getAdvancements().award(advancement(helper, "story/mine_stone"), "get_stone");
        } finally {
            rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, true, server);
        }

        helper.assertValueEqual(ANNOUNCED.matching(event -> event.player() == player).size(), 0, "announcements");
        helper.succeed();
    }

    @GameTest
    public void dyingFiresTheEventWithTheDeathMessage(GameTestHelper helper) {
        final var player = player(helper);

        player.kill(helper.getLevel());

        final var died = DIED.matching(event -> event.player() == player);
        helper.assertValueEqual(died.size(), 1, "death messages");
        // Read too late, it would only say "<player> died", as the combat tracker's been reset
        helper.assertValueEqual(
            died.getFirst().message().getString(),
            player.getGameProfile().name() + " was killed",
            "death message"
        );
        helper.succeed();
    }

    @GameTest
    public void dyingDoesNotFireTheEventWithDeathMessagesOff(GameTestHelper helper) {
        final var player = player(helper);
        final var rules = helper.getLevel().getGameRules();
        final var server = helper.getLevel().getServer();

        rules.set(GameRules.SHOW_DEATH_MESSAGES, false, server);
        try {
            player.kill(helper.getLevel());
        } finally {
            rules.set(GameRules.SHOW_DEATH_MESSAGES, true, server);
        }

        helper.assertValueEqual(DIED.matching(event -> event.player() == player).size(), 0, "death messages");
        helper.succeed();
    }

    private static AdvancementHolder advancement(GameTestHelper helper, String path) {
        return Objects.requireNonNull(
            helper.getLevel().getServer().getAdvancements().get(Identifier.withDefaultNamespace(path)),
            path
        );
    }
}
