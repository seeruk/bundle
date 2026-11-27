package dev.seeruk.bundle.digest.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.time.TimeCodecs;
import net.minecraft.core.UUIDUtil;

import java.time.Instant;
import java.util.*;

/**
 * One issue of the digest, as numbers rather than text, so it can be shown however it's wanted,
 * whenever it's read. Players are kept by UUID, so they're shown with their names as they are now.
 *
 * @param number        Which issue it is, from 1.
 * @param started       When the time it covers started, i.e. when the last issue was published.
 * @param ended         When the time it covers ended, i.e. when it was published.
 * @param groups        Each stat group's results, by the group's ID.
 * @param activePlayers Everyone whose stats went up, or who any moments are about, in that time.
 * @param newPlayers    Active players who had no stats before that time started.
 * @param moments       What happened in that time that isn't a stat, e.g. deaths.
 */
public record Issue(
    int number,
    Instant started,
    Instant ended,
    Map<String, GroupResult> groups,
    List<UUID> activePlayers,
    List<UUID> newPlayers,
    Moments moments
) {
    public static final Codec<Issue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("number").forGetter(Issue::number),
        TimeCodecs.INSTANT.fieldOf("started").forGetter(Issue::started),
        TimeCodecs.INSTANT.fieldOf("ended").forGetter(Issue::ended),
        Codec.unboundedMap(Codec.STRING, GroupResult.CODEC).fieldOf("groups").forGetter(Issue::groups),
        UUIDUtil.STRING_CODEC.listOf().fieldOf("active_players").forGetter(Issue::activePlayers),
        UUIDUtil.STRING_CODEC.listOf().fieldOf("new_players").forGetter(Issue::newPlayers),
        // Optional, so issues saved before moments were kept can still be read
        Moments.CODEC.optionalFieldOf("moments", Moments.EMPTY).forGetter(Issue::moments)
    ).apply(instance, Issue::new));

    /**
     * A stat group's results for the issue.
     *
     * @param total   Everyone's totals added together.
     * @param players Everyone who scored, highest first.
     */
    public record GroupResult(long total, List<PlayerTotal> players) {
        public static final Codec<GroupResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("total").forGetter(GroupResult::total),
            PlayerTotal.CODEC.listOf().fieldOf("players").forGetter(GroupResult::players)
        ).apply(instance, GroupResult::new));
    }

    /**
     * @param player Who it is.
     * @param value  Their total for the issue.
     */
    public record PlayerTotal(UUID player, long value) {
        public static final Codec<PlayerTotal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(PlayerTotal::player),
            Codec.LONG.fieldOf("value").forGetter(PlayerTotal::value)
        ).apply(instance, PlayerTotal::new));
    }

    /**
     * Things that happened, rather than stats that went up, oldest first.
     *
     * @param deaths       Every player death.
     * @param advancements Every advancement the game announced.
     * @param firsts       Everything that happened for the first time on the server.
     */
    public record Moments(List<Death> deaths, List<Earned> advancements, List<First> firsts) {
        public static final Moments EMPTY = new Moments(List.of(), List.of(), List.of());

        public static final Codec<Moments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Death.CODEC.listOf().fieldOf("deaths").forGetter(Moments::deaths),
            Earned.CODEC.listOf().fieldOf("advancements").forGetter(Moments::advancements),
            First.CODEC.listOf().fieldOf("firsts").forGetter(Moments::firsts)
        ).apply(instance, Moments::new));

        /**
         * Everyone any of the moments are about.
         */
        public Set<UUID> players() {
            final var players = new HashSet<UUID>();
            deaths.forEach(death -> players.add(death.player()));
            advancements.forEach(earned -> players.add(earned.player()));
            firsts.forEach(first -> players.add(first.player()));
            return players;
        }
    }

    /**
     * A player dying.
     *
     * @param player Who died.
     * @param cause  The damage type's ID, e.g. {@code minecraft:fall}.
     * @param killer The ID of the type of entity credited with killing them, if any, e.g.
     *               {@code minecraft:warden}, as in Vanilla's death messages.
     * @param at     When they died.
     */
    public record Death(UUID player, String cause, Optional<String> killer, Instant at) {
        public static final Codec<Death> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(Death::player),
            Codec.STRING.fieldOf("cause").forGetter(Death::cause),
            Codec.STRING.optionalFieldOf("killer").forGetter(Death::killer),
            TimeCodecs.INSTANT.fieldOf("at").forGetter(Death::at)
        ).apply(instance, Death::new));
    }

    /**
     * A player earning an advancement.
     *
     * @param player      Who earned it.
     * @param advancement Its ID, e.g. {@code minecraft:story/mine_stone}.
     * @param at          When they earned it.
     */
    public record Earned(UUID player, String advancement, Instant at) {
        public static final Codec<Earned> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(Earned::player),
            Codec.STRING.fieldOf("advancement").forGetter(Earned::advancement),
            TimeCodecs.INSTANT.fieldOf("at").forGetter(Earned::at)
        ).apply(instance, Earned::new));
    }

    /**
     * Something happening for the first time on the server.
     *
     * @param player Who it happened to.
     * @param what   What happened: {@code advancement:<id>} for earning an advancement,
     *               {@code death:<damage type>} for dying of a cause, or
     *               {@code killer:<entity type>} for being killed by a kind of entity.
     * @param at     When it happened.
     */
    public record First(UUID player, String what, Instant at) {
        public static final Codec<First> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(First::player),
            Codec.STRING.fieldOf("what").forGetter(First::what),
            TimeCodecs.INSTANT.fieldOf("at").forGetter(First::at)
        ).apply(instance, First::new));
    }
}
