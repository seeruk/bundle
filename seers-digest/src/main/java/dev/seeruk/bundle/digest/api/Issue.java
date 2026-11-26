package dev.seeruk.bundle.digest.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.time.TimeCodecs;
import net.minecraft.core.UUIDUtil;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One issue of the digest, as numbers rather than text, so it can be shown however it's wanted,
 * whenever it's read. Players are kept by UUID, so they're shown with their names as they are now.
 *
 * @param number        Which issue it is, from 1.
 * @param started       When the time it covers started, i.e. when the last issue was published.
 * @param ended         When the time it covers ended, i.e. when it was published.
 * @param groups        Each stat group's results, by the group's ID.
 * @param activePlayers Everyone whose stats went up in that time.
 * @param newPlayers    Active players who had no stats before that time started.
 */
public record Issue(
    int number,
    Instant started,
    Instant ended,
    Map<String, GroupResult> groups,
    List<UUID> activePlayers,
    List<UUID> newPlayers
) {
    public static final Codec<Issue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("number").forGetter(Issue::number),
        TimeCodecs.INSTANT.fieldOf("started").forGetter(Issue::started),
        TimeCodecs.INSTANT.fieldOf("ended").forGetter(Issue::ended),
        Codec.unboundedMap(Codec.STRING, GroupResult.CODEC).fieldOf("groups").forGetter(Issue::groups),
        UUIDUtil.STRING_CODEC.listOf().fieldOf("active_players").forGetter(Issue::activePlayers),
        UUIDUtil.STRING_CODEC.listOf().fieldOf("new_players").forGetter(Issue::newPlayers)
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
}
