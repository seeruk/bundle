package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.leaders.LeadersConfig.Announcement;
import dev.seeruk.bundle.leaders.LeadersConfig.Category;
import dev.seeruk.bundle.leaders.LeadersConfig.Period;
import dev.seeruk.bundle.leaders.LeadersConfig.PeriodAnnouncement;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;

import java.time.ZoneOffset;
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
    // What tests check of each event, leaving out titles, which are checked by announcements
    public record Changed(String category, Optional<UUID> previous, Optional<UUID> leader, boolean announceable) {}

    public record PeriodEnded(String period, List<Won> winners, boolean announceable) {}

    public record Won(String category, UUID player, long value, String displayValue) {}

    // The block mannequins are placed on in tests
    public static final BlockPos BLOCK = new BlockPos(2, 1, 2);

    private static final EventLog<Changed> EVENTS = new EventLog<>();
    private static final EventLog<PeriodEnded> PERIOD_EVENTS = new EventLog<>();

    @Override
    public void onInitialize() {
        LeaderEvents.CHANGED.register((server, change) ->
            EVENTS.record(new Changed(change.category(), change.previous(), change.leader(), change.announceable())));
        LeaderEvents.PERIOD_ENDED.register((server, end) ->
            PERIOD_EVENTS.record(new PeriodEnded(
                end.period(),
                end.winners().stream()
                    .map(winner -> new Won(winner.category(), winner.player(), winner.value(), winner.displayValue()))
                    .toList(),
                end.announceable()
            )));
    }

    /**
     * Every leader change in the category, oldest first.
     */
    public static List<Changed> eventsFor(String category) {
        return EVENTS.matching(event -> event.category().equals(category));
    }

    /**
     * Every time the period ended, oldest first.
     */
    public static List<PeriodEnded> periodEventsFor(String period) {
        return PERIOD_EVENTS.matching(event -> event.period().equals(period));
    }

    /**
     * A config with one category, ranked by the given objective's scores. Its mannequins hold a
     * stick.
     */
    public static LeadersConfig config(String category, Objective objective) {
        return config(category(category, objective, true));
    }

    /**
     * A config with the given categories, and no periods. New leaders are announced as
     * "<player> leads Test", so each test can find its own players' announcements.
     */
    public static LeadersConfig config(Category... categories) {
        return config(List.of(), List.of(categories));
    }

    /**
     * A config with one period, and one category of the same name in it, ranked by the given
     * objective's scores. When the period ends, it's announced as "<period> ended", so each test
     * can find its own announcement.
     */
    public static LeadersConfig periodConfig(String period, Objective objective, Schedule schedule) {
        return config(
            List.of(new Period(period, TextTemplate.parse(period), schedule)),
            List.of(category(period, objective, Optional.of(period), true))
        );
    }

    /**
     * A category titled "Test", ranked by the given objective's scores.
     *
     * @param id              The category's ID.
     * @param objective       The objective it's ranked by.
     * @param announceChanges Whether its new leaders are announced.
     */
    public static Category category(String id, Objective objective, boolean announceChanges) {
        return category(id, objective, Optional.empty(), announceChanges);
    }

    private static LeadersConfig config(List<Period> periods, List<Category> categories) {
        return new LeadersConfig(
            300,
            ZoneOffset.UTC,
            new Announcement(true, TextTemplate.parse("${player} leads ${category}")),
            new PeriodAnnouncement(
                true,
                TextTemplate.parse("${period} ended"),
                TextTemplate.parse("${category}: ${player}")
            ),
            periods,
            categories
        );
    }

    private static Category category(String id, Objective objective, Optional<String> period, boolean announceChanges) {
        return new Category(
            id,
            TextTemplate.parse("Test"),
            period,
            StatSelection.of(List.of("scoreboard:" + objective.getName())),
            StatUnit.COUNT,
            TextTemplate.parse("${category} #${rank}"),
            TextTemplate.parse("${player} - ${value}"),
            TextTemplate.parse("${category} #${rank}"),
            TextTemplate.parse("Nobody yet"),
            Optional.of(TextTemplate.parse("Last: ${player}")),
            Optional.of(Items.STICK),
            announceChanges
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
