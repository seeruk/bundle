package dev.seeruk.bundle.leaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.leaders.Leaderboard.Standing;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import net.minecraft.SharedConstants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.seeruk.bundle.leaders.SeersLeaders.LOGGER;
import static dev.seeruk.bundle.leaders.SeersLeaders.MOD_ID;

/**
 * Works out who holds each rank in each category, from every player's stats.
 */
public final class Leaders {
    // Each category's ranked players, by category ID. Concurrent, as it can be read from many threads.
    private static final Map<String, List<Standing>> BOARDS = new ConcurrentHashMap<>();

    // Each category's leader as of the last refresh, kept so restarting doesn't count as a change
    private static final WorldStorage<Map<String, Leader>> LEADERS = WorldStorage.create(
        MOD_ID, "leaders", Codec.unboundedMap(Codec.STRING, Leader.CODEC), Map::of
    );

    private record Leader(Optional<UUID> player) {
        private static final Codec<Leader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.optionalFieldOf("player").forGetter(Leader::player)
        ).apply(instance, Leader::new));
    }

    private Leaders() {}

    public static void started(MinecraftServer server, LeadersConfig config) {
        warnUnknownStats(server, config);
        refresh(server, config);
    }

    public static void tick(MinecraftServer server, LeadersConfig config) {
        if (server.getTickCount() % (config.refreshSeconds() * SharedConstants.TICKS_PER_SECOND) == 0) {
            refresh(server, config);
        }
    }

    /**
     * Ranks every category again from players' current stats, firing
     * {@link LeaderEvents#CHANGED} for any category with a new leader.
     */
    public static void refresh(MinecraftServer server, LeadersConfig config) {
        final var stats = AllPlayerStats.read(server);

        for (final var category : config.categories()) {
            final var totals = new HashMap<UUID, Long>();
            stats.forEach((player, values) -> totals.put(player, category.stats().sum(values)));

            final var previous = BOARDS.getOrDefault(category.id(), List.of()).stream().map(Standing::player).toList();
            final var board = Leaderboard.rank(totals, previous);
            BOARDS.put(category.id(), board);

            recordLeader(server, category.id(), board.stream().findFirst().map(Standing::player));
        }
    }

    /**
     * Who holds the given rank (starting at 1) in a category, as of the last refresh.
     */
    public static Optional<Standing> standing(String category, int rank) {
        final var board = BOARDS.getOrDefault(category, List.of());
        return rank >= 1 && rank <= board.size() ? Optional.of(board.get(rank - 1)) : Optional.empty();
    }

    public static void clear() {
        BOARDS.clear();
    }

    private static void recordLeader(MinecraftServer server, String category, Optional<UUID> leader) {
        final var recorded = LEADERS.get(server).get(category);
        if (recorded != null && recorded.player().equals(leader)) {
            return;
        }

        LEADERS.update(server, leaders -> {
            final var updated = new HashMap<>(leaders);
            updated.put(category, new Leader(leader));
            return Map.copyOf(updated);
        });

        if (recorded != null) {
            LeaderEvents.CHANGED.invoker().onLeaderChanged(server, category, recorded.player(), leader);
        }
    }

    // Scoreboard objectives may not exist yet on a new world, until the mods that add them start
    private static void warnUnknownStats(MinecraftServer server, LeadersConfig config) {
        for (final var category : config.categories()) {
            for (final var name : category.stats().exactNames()) {
                final var known = name.startsWith(AllPlayerStats.SCOREBOARD_PREFIX)
                    ? server.getScoreboard().getObjective(name.substring(AllPlayerStats.SCOREBOARD_PREFIX.length())) != null
                    : ObjectiveCriteria.byName(name).isPresent();

                if (!known) {
                    LOGGER.warn("Category '{}' uses '{}', which isn't a stat (yet), so it's counted as 0", category.id(), name);
                }
            }
        }
    }
}
