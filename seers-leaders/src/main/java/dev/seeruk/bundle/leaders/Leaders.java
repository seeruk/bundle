package dev.seeruk.bundle.leaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.leaders.Leaderboard.Standing;
import dev.seeruk.bundle.leaders.LeadersConfig.Category;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.seeruk.bundle.leaders.SeersLeaders.MOD_ID;

/**
 * Each category's leaderboard, worked out from every player's stats.
 */
public final class Leaders {
    // Each category's ranked players, by category ID. Concurrent, as it's read from many threads.
    private static final Map<String, List<Standing>> BOARDS = new ConcurrentHashMap<>();

    // Each category's leader as of the last refresh, kept so restarting doesn't count as a change
    private static final WorldStorage<Map<String, Leader>> LEADERS = WorldStorage.create(
        MOD_ID, "leaders", Codec.unboundedMap(Codec.STRING, Leader.CODEC), Map::of
    );

    // Each periodic category's ranked players when its last period ended, by category ID
    private static final WorldStorage<Map<String, List<Standing>>> RESULTS = WorldStorage.create(
        MOD_ID, "results", Codec.unboundedMap(Codec.STRING, Standing.CODEC.listOf()), Map::of
    );

    private record Leader(Optional<UUID> player) {
        private static final Codec<Leader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.optionalFieldOf("player").forGetter(Leader::player)
        ).apply(instance, Leader::new));
    }

    private Leaders() {}

    /**
     * Ranks every category again from players' current stats, firing
     * {@link LeaderEvents#CHANGED} for any category with a new leader and announcing them together,
     * then updates loaded leader mannequins.
     */
    public static void refresh(MinecraftServer server, LeadersConfig config) {
        final var stats = AllPlayerStats.read(server);
        final var changes = new ArrayList<LeaderEvents.Change>();
        // Each period's stats, worked out the first time a category in it needs them
        final var sinceStart = new HashMap<String, Map<UUID, Map<String, Integer>>>();

        for (final var category : config.categories()) {
            final var counted = category.period()
                .map(period -> sinceStart.computeIfAbsent(period, ignored -> Periods.sinceStart(server, period, stats)))
                .orElse(stats);

            final var board = rank(category, counted);
            BOARDS.put(category.id(), board);
            recordLeader(server, category, board.stream().findFirst().map(Standing::player)).ifPresent(changes::add);
        }

        LeaderAnnouncements.announce(server, config, changes);
        LeaderMannequins.showAll(config);
    }

    /**
     * Who is sitting in the seat, as of the last refresh, if anyone.
     */
    public static Optional<Standing> standing(Seat seat) {
        return at(BOARDS.getOrDefault(seat.category(), List.of()), seat);
    }

    /**
     * Who was sitting in the seat when its category's last period ended, if anyone.
     */
    public static Optional<Standing> previous(MinecraftServer server, Seat seat) {
        return at(RESULTS.get(server).getOrDefault(seat.category(), List.of()), seat);
    }

    /**
     * Saves the period's categories' boards, as of the last refresh, as their final results,
     * replacing the last period's. Returns the winner of each that anyone scored in.
     */
    public static List<LeaderEvents.Winner> saveFinalResults(MinecraftServer server, LeadersConfig config, String period) {
        final var results = new HashMap<>(RESULTS.get(server));
        final var winners = new ArrayList<LeaderEvents.Winner>();
        for (final var category : config.categoriesIn(period)) {
            final var board = BOARDS.getOrDefault(category.id(), List.of());
            // Only as many ranks as a seat can show are kept, so the saved data stays small
            results.put(category.id(), List.copyOf(board.subList(0, Math.min(board.size(), Seat.MAX_RANK))));
            board.stream().findFirst().ifPresent(winner -> winners.add(new LeaderEvents.Winner(
                category.id(),
                category.displayTitle(),
                winner.player(),
                winner.value(),
                category.unit().format(winner.value())
            )));
        }

        RESULTS.set(server, Map.copyOf(results));
        return List.copyOf(winners);
    }

    public static void clear() {
        BOARDS.clear();
    }

    // Ranks players by their totals in the category. Ties keep the order from the last refresh.
    private static List<Standing> rank(Category category, Map<UUID, Map<String, Integer>> counted) {
        final var totals = new HashMap<UUID, Long>();
        counted.forEach((player, values) -> totals.put(player, category.stats().sum(values)));

        final var previous = BOARDS.getOrDefault(category.id(), List.of()).stream().map(Standing::player).toList();
        return Leaderboard.rank(totals, previous);
    }

    private static Optional<Standing> at(List<Standing> board, Seat seat) {
        return seat.rank() <= board.size() ? Optional.of(board.get(seat.rank() - 1)) : Optional.empty();
    }

    // Saves the category's leader, and returns the change if it's someone else (or nobody) now
    private static Optional<LeaderEvents.Change> recordLeader(MinecraftServer server, Category category, Optional<UUID> leader) {
        // Nothing recorded means the category has never been ranked, which isn't a change. A
        // recorded leader of nobody is, so the first person to score in a category is a change.
        final var recorded = Optional.ofNullable(LEADERS.get(server).get(category.id())).map(Leader::player);
        if (recorded.equals(Optional.of(leader))) {
            return Optional.empty();
        }

        LEADERS.update(
            server, leaders -> {
                final var updated = new HashMap<>(leaders);
                updated.put(category.id(), new Leader(leader));
                return Map.copyOf(updated);
            }
        );

        return recorded.map(previous -> {
            // Only someone taking the lead from someone else is news. Being first in an empty
            // category isn't, and nor is a period starting again with nobody in the lead.
            final var announceable = previous.isPresent() && leader.isPresent() && category.announceChanges();
            final var change = new LeaderEvents.Change(
                category.id(),
                category.displayTitle(),
                previous,
                leader,
                announceable
            );
            LeaderEvents.CHANGED.invoker().onLeaderChanged(server, change);
            return change;
        });
    }

    /**
     * Forgets the final results of categories that aren't in the config, e.g. on start-up, as
     * that's the only time the config can change.
     */
    public static void forgetRemovedResults(MinecraftServer server, LeadersConfig config) {
        final var ids = config.categories().stream().map(Category::id).toList();
        final var results = RESULTS.get(server);
        if (!ids.containsAll(results.keySet())) {
            final var kept = new HashMap<>(results);
            kept.keySet().retainAll(ids);
            RESULTS.set(server, Map.copyOf(kept));
        }
    }
}
