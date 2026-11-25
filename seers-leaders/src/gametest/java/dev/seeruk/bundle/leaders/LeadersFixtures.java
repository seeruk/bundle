package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.leaders.LeadersConfig.Announcement;
import dev.seeruk.bundle.leaders.LeadersConfig.Category;
import dev.seeruk.bundle.leaders.LeadersConfig.Period;
import dev.seeruk.bundle.leaders.LeadersConfig.PeriodAnnouncement;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import dev.seeruk.bundle.leaders.api.LeaderEvents.Winner;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    public record PeriodEnded(String period, List<Winner> winners) {}

    // When test periods end: every day at 18:00, in UTC
    public static final Schedule DAILY = Schedule.daily(LocalTime.of(18, 0));

    // The block mannequins are placed on in tests
    public static final BlockPos BLOCK = new BlockPos(2, 1, 2);

    private static final EventLog<Changed> EVENTS = new EventLog<>();
    private static final EventLog<PeriodEnded> PERIOD_EVENTS = new EventLog<>();

    @Override
    public void onInitialize() {
        LeaderEvents.CHANGED.register((server, category, previous, leader) ->
            EVENTS.record(new Changed(category, previous, leader)));
        LeaderEvents.PERIOD_ENDED.register((server, period, winners) ->
            PERIOD_EVENTS.record(new PeriodEnded(period, winners)));
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
        return config(List.of(), category(category, objective, Optional.empty()));
    }

    /**
     * A config with one period, and one category of the same name in it, ranked by the given
     * objective's scores. When the period ends, it's announced as "<period> ended", so each test
     * can find its own announcement.
     */
    public static LeadersConfig periodConfig(String period, Objective objective, Schedule schedule) {
        return config(
            List.of(new Period(period, TextTemplate.parse(period), schedule)),
            category(period, objective, Optional.of(period))
        );
    }

    private static LeadersConfig config(List<Period> periods, Category category) {
        return new LeadersConfig(
            300,
            ZoneOffset.UTC,
            new Announcement(false, TextTemplate.parse("${player} leads ${category}")),
            new PeriodAnnouncement(true, TextTemplate.parse("${period} ended"), TextTemplate.parse("${category}: ${player}")),
            periods,
            List.of(category)
        );
    }

    private static Category category(String id, Objective objective, Optional<String> period) {
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
            true
        );
    }

    /**
     * 19:00 on a day no other test uses, an hour after that day's {@link #DAILY} period started.
     * Snapshots are shared by every period that starts at the same moment, so tests can't share
     * days.
     */
    public static ZonedDateTime uniqueDay() {
        final var day = LocalDate.of(2000, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(1_000_000));
        return ZonedDateTime.of(day, LocalTime.of(19, 0), ZoneOffset.UTC);
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
