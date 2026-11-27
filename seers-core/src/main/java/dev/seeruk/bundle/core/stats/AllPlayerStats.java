package dev.seeruk.bundle.core.stats;

import dev.seeruk.bundle.core.mixin.StatsCounterAccessor;
import dev.seeruk.bundle.core.names.PlayerNames;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Reads every player's stats, whether they're online or not, for use with {@link StatSelection}.
 * Vanilla stats are named by their scoreboard criterion (e.g.
 * {@code minecraft.mined:minecraft.stone}), and scoreboard scores as
 * {@code scoreboard:<objective>}.
 */
public final class AllPlayerStats {
    public static final String SCOREBOARD_PREFIX = "scoreboard:";

    // Offline players' stats can't change, so each file is only read again once it's been saved.
    // Concurrent, as it can be read from many threads.
    private static final Map<UUID, FileStats> OFFLINE = new ConcurrentHashMap<>();

    private record FileStats(FileTime modified, Map<String, Integer> values) {}

    private AllPlayerStats() {}

    /**
     * Returns each player's stat values by UUID, for every player who's online or has a stats file.
     * Scores are only included for score holders named after one of those players.
     */
    public static Map<UUID, Map<String, Integer>> read(MinecraftServer server) {
        final var values = new HashMap<UUID, Map<String, Integer>>();
        final var online = server.getPlayerList().getPlayers();
        final var onlineIds = online.stream().map(ServerPlayer::getUUID).collect(Collectors.toSet());

        final var offlineIds = offlineIds(server, onlineIds);
        OFFLINE.keySet().retainAll(offlineIds); // Prune as players come and go.

        // Offline and online players are handled exclusively, and cannot collide.
        offlineIds.forEach(id ->
            fileStats(server, id).ifPresent(stats -> values.put(id, new HashMap<>(stats))));
        online.forEach(player -> values.put(player.getUUID(), statValues(player.getStats())));

        // Adding scoreboard scores is the same for online or offline players.
        addScores(server, values);
        return values;
    }

    /**
     * The selection's exact stat names (see {@link StatSelection#exactNames()}) that aren't a
     * Vanilla stat or a scoreboard objective, e.g. typos. Objectives may not exist yet on a new
     * world, until the mods that add them start.
     */
    public static List<String> unknownNames(MinecraftServer server, StatSelection selection) {
        return selection.exactNames().stream()
            .filter(name -> name.startsWith(SCOREBOARD_PREFIX)
                ? server.getScoreboard().getObjective(name.substring(SCOREBOARD_PREFIX.length())) == null
                : ObjectiveCriteria.byName(name).isEmpty())
            .toList();
    }

    /**
     * Forgets the cached stats from files, e.g. when the server stops.
     */
    public static void clear() {
        OFFLINE.clear();
    }

    private static Path statsFolder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
    }

    private static Set<UUID> offlineIds(MinecraftServer server, Set<UUID> onlineIds) {
        try (final var files = Files.list(statsFolder(server))) {
            return files
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".json"))
                .flatMap(name -> parseId(name.substring(0, name.length() - ".json".length())).stream())
                .filter(id -> !onlineIds.contains(id))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            return Set.of(); // No stats saved yet
        }
    }

    private static Optional<UUID> parseId(String name) {
        try {
            return Optional.of(UUID.fromString(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // Not a stats file Vanilla still uses
        }
    }

    private static Optional<Map<String, Integer>> fileStats(MinecraftServer server, UUID id) {
        final var path = statsFolder(server).resolve(id + ".json");
        try {
            final var modified = Files.getLastModifiedTime(path);
            final var cached = OFFLINE.get(id);
            if (cached != null && cached.modified().equals(modified)) {
                return Optional.of(cached.values());
            }

            final var stats = Map.copyOf(statValues(new ServerStatsCounter(server, path)));
            OFFLINE.put(id, new FileStats(modified, stats));
            return Optional.of(stats);
        } catch (IOException e) {
            return Optional.empty(); // Removed since it was listed
        }
    }

    private static Map<String, Integer> statValues(StatsCounter counter) {
        final var values = new HashMap<String, Integer>();
        ((StatsCounterAccessor) counter).getStats().object2IntEntrySet()
            .forEach(entry -> values.put(entry.getKey().getName(), entry.getIntValue()));
        return values;
    }

    /**
     * Add scoreboard scores to the given map, so they can be used alongside player stats.
     * Scoreboard scores are prefixed with {@link #SCOREBOARD_PREFIX}.
     */
    private static void addScores(MinecraftServer server, Map<UUID, Map<String, Integer>> values) {
        final var idsByName = new HashMap<String, UUID>();
        values.keySet().forEach(id -> PlayerNames.username(server, id).ifPresent(name -> idsByName.put(
            name.toLowerCase(Locale.ROOT),
            id
        )));

        final var scoreboard = server.getScoreboard();
        for (final var objective : scoreboard.getObjectives()) {
            for (final var entry : scoreboard.listPlayerScores(objective)) {
                final var id = idsByName.get(entry.owner().toLowerCase(Locale.ROOT));
                if (id != null) {
                    values.get(id).put(SCOREBOARD_PREFIX + objective.getName(), entry.value());
                }
            }
        }
    }
}
