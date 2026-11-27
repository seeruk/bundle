package dev.seeruk.bundle.digest;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.digest.api.Issue;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static dev.seeruk.bundle.digest.SeersDigest.LOGGER;
import static dev.seeruk.bundle.digest.SeersDigest.MOD_ID;

/**
 * Moments (see {@link Issue.Moments}): things that happen, rather than stats that go up, kept until
 * the next issue is published.
 */
public final class Moments {
    // What's happened since the last issue
    private static final WorldStorage<Issue.Moments> PENDING = WorldStorage.create(
        MOD_ID, "moments", Issue.Moments.CODEC, () -> Issue.Moments.EMPTY
    );

    private static final String KILLED_BY = "minecraft.killed_by:";

    // Everything that's ever happened on the server, as First.what keys, kept forever, so only the
    // first time each happens is a first. Seeded once with what happened before Digest was added.
    private static final WorldStorage<Seen> SEEN = WorldStorage.create(
        MOD_ID, "firsts", Seen.CODEC, () -> new Seen(false, Set.of())
    );

    /**
     * @param seeded Whether what happened before Digest was added has been added to it.
     * @param keys   Everything that's happened, as First.what keys.
     */
    private record Seen(boolean seeded, Set<String> keys) {
        private static final Codec<Seen> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("seeded").forGetter(Seen::seeded),
            Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, List::copyOf).fieldOf("keys").forGetter(Seen::keys)
        ).apply(instance, Seen::new));

        private Seen with(Collection<String> happened) {
            final var updated = new HashSet<>(keys);
            updated.addAll(happened);
            return new Seen(seeded, Set.copyOf(updated));
        }
    }

    private Moments() {}

    /**
     * Records a player's death, and any firsts it makes: the first death of its cause, and the
     * first death to its kind of killer.
     */
    public static void recordDeath(ServerPlayer player, DamageSource source, Clock clock) {
        final var at = clock.instant();
        final var cause = source.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
        // Whoever Vanilla credits, as in its killed_by stats and death messages, so a player
        // knocked off a cliff by a skeleton was killed by the skeleton
        final var killer = Optional.ofNullable(player.getKillCredit())
            .map(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        final var death = new Issue.Death(player.getUUID(), cause, killer, at);

        final var keys = new ArrayList<String>();
        keys.add("death:" + cause);
        killer.ifPresent(found -> keys.add("killer:" + found));

        record(player, at, keys, moments -> moments.withDeath(death));
    }

    /**
     * Records a player earning an advancement, and whether they're the first on the server to.
     */
    public static void recordAdvancement(ServerPlayer player, AdvancementHolder advancement, Clock clock) {
        final var at = clock.instant();
        final var id = advancement.id().toString();
        final var earned = new Issue.Earned(player.getUUID(), id, at);

        record(player, at, List.of("advancement:" + id), moments -> moments.withAdvancement(earned));
    }

    /**
     * The first time Digest runs on a world, notes everything players have already done, so none of
     * it counts as a first later: the advancements they've earned, from their advancement files,
     * and the kinds of entity that have killed them, from their stats. Vanilla doesn't keep what
     * else players died of (e.g. falling), so those can still be firsts once, after Digest is
     * added to an existing world.
     */
    public static void seed(MinecraftServer server) {
        final var seen = SEEN.get(server);
        if (seen.seeded()) {
            return;
        }

        SEEN.set(server, new Seen(true, seen.with(existing(server)).keys()));
    }

    /**
     * Everything players have already done that could be a first, as First.what keys.
     */
    static Set<String> existing(MinecraftServer server) {
        final var keys = new HashSet<String>();
        earnedAdvancements(server).forEach(id -> keys.add("advancement:" + id));
        AllPlayerStats.read(server).values().forEach(stats -> stats.keySet().forEach(name ->
            killerFromStat(name).ifPresent(killer -> keys.add("killer:" + killer))));
        return keys;
    }

    /**
     * Everything that's happened since the last issue.
     */
    public static Issue.Moments pending(MinecraftServer server) {
        return PENDING.get(server);
    }

    /**
     * Forgets everything that's happened since the last issue, when the next issue starts.
     */
    public static void forget(MinecraftServer server) {
        PENDING.set(server, Issue.Moments.EMPTY);
    }

    // Records a moment, given as how it's added to the pending moments, with a first for each of
    // its keys that's never happened before. From then on, those keys have happened.
    private static void record(ServerPlayer player, Instant at, List<String> keys, UnaryOperator<Issue.Moments> add) {
        final var server = player.level().getServer();
        final var seen = SEEN.get(server).keys();
        final var firsts = keys.stream()
            .filter(key -> !seen.contains(key))
            .map(key -> new Issue.First(player.getUUID(), key, at))
            .toList();

        if (!firsts.isEmpty()) {
            SEEN.update(server, existing -> existing.with(firsts.stream().map(Issue.First::what).toList()));
        }
        PENDING.update(server, moments -> add.apply(moments).withFirsts(firsts));
    }

    // The kind of entity a killed-by stat is for, e.g. "minecraft.killed_by:minecraft.spider" is
    // for "minecraft:spider", as the stat's name uses a dot where the entity's ID has a colon
    static Optional<String> killerFromStat(String statName) {
        if (!statName.startsWith(KILLED_BY)) {
            return Optional.empty();
        }

        final var entity = statName.substring(KILLED_BY.length());
        final var dot = entity.indexOf('.');
        return Optional.of(dot < 0 ? entity : entity.substring(0, dot) + ":" + entity.substring(dot + 1));
    }

    // Every advancement the game announces that any player has earned, from their saved files
    private static Set<String> earnedAdvancements(MinecraftServer server) {
        try (final var files = Files.list(server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR))) {
            return files
                .filter(file -> file.getFileName().toString().endsWith(".json"))
                .flatMap(file -> finishedAdvancements(file).stream())
                .filter(id -> announced(server, id))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            return Set.of(); // No advancements saved yet
        }
    }

    // The advancements a player's file says they've finished, by ID
    private static Set<String> finishedAdvancements(Path file) {
        try {
            final var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return json.entrySet().stream()
                .filter(entry -> entry.getValue().isJsonObject())
                .filter(entry -> {
                    final var done = entry.getValue().getAsJsonObject().get("done");
                    return done != null && done.getAsBoolean();
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Couldn't read advancements from {}, so they can still be firsts", file, e);
            return Set.of();
        }
    }

    // Whether it's an advancement the game announces, as only those are recorded
    private static boolean announced(MinecraftServer server, String id) {
        final var parsed = Identifier.tryParse(id);
        final var advancement = parsed == null ? null : server.getAdvancements().get(parsed);
        return advancement != null && advancement.value().display().map(DisplayInfo::announceToChat).orElse(false);
    }
}
