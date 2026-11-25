package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.leaders.LeadersConfig.Announcement;
import dev.seeruk.bundle.leaders.LeadersConfig.Category;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

/**
 * Shared helpers for Leaders' GameTests. Starts recording leader changes when the game starts, so
 * every test's changes are recorded.
 *
 * <p>The scoreboard and leaders are shared by every test on the server, so each test ranks its own
 * category, from its own objective.
 */
public final class LeadersFixtures implements ModInitializer {
    public record Changed(String category, Optional<UUID> previous, Optional<UUID> leader) {}

    // The block mannequins are placed on in tests
    public static final BlockPos BLOCK = new BlockPos(2, 1, 2);

    private static final EventLog<Changed> EVENTS = new EventLog<>();

    @Override
    public void onInitialize() {
        LeaderEvents.CHANGED.register((server, category, previous, leader) ->
            EVENTS.record(new Changed(category, previous, leader)));
    }

    /**
     * Every leader change in the category, oldest first.
     */
    public static List<Changed> eventsFor(String category) {
        return EVENTS.matching(event -> event.category().equals(category));
    }

    /**
     * A config with one category, ranked by the given objective's scores. Its mannequins hold a
     * stick.
     */
    public static LeadersConfig config(String category, Objective objective) {
        return new LeadersConfig(
            300,
            new Announcement(false, TextTemplate.parse("${player} leads ${category}")),
            List.of(new Category(
                category,
                TextTemplate.parse("Test"),
                StatSelection.of(List.of("scoreboard:" + objective.getName())),
                StatUnit.COUNT,
                TextTemplate.parse("${category} #${rank}"),
                TextTemplate.parse("${player} - ${value}"),
                TextTemplate.parse("${category} #${rank}"),
                TextTemplate.parse("Nobody yet"),
                Optional.of(Items.STICK)
            ))
        );
    }

    /**
     * Adds a brand-new objective, removing any existing one with the same name first.
     */
    public static Objective addObjective(MinecraftServer server, String name) {
        final var scoreboard = server.getScoreboard();
        final var existing = scoreboard.getObjective(name);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }

        return scoreboard.addObjective(
            name, ObjectiveCriteria.DUMMY, Component.literal(name), ObjectiveCriteria.RenderType.INTEGER, false, null
        );
    }

    /**
     * A player standing two blocks in front of a stone block at {@link #BLOCK}, looking at it.
     */
    public static ServerPlayer playerFacingBlock(GameTestHelper helper) {
        helper.setBlock(BLOCK, Blocks.STONE);

        final var player = player(helper);
        final var standing = helper.absoluteVec(new Vec3(2.5, 1, 4.5));
        player.snapTo(standing.x, standing.y, standing.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(helper.absolutePos(BLOCK)));
        return player;
    }
}
