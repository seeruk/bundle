package dev.seeruk.bundle.digest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.time.Schedule;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;

/**
 * @param timezone The time zone the schedule is in.
 * @param schedule When each issue is published, covering everything since the one before.
 * @param groups   The stats each issue adds up, for every player.
 */
public record DigestConfig(ZoneId timezone, Schedule schedule, List<StatGroup> groups) {
    private static final Codec<ZoneId> ZONE_CODEC = Codec.STRING.comapFlatMap(
        zone -> {
            try {
                return DataResult.success(ZoneId.of(zone));
            } catch (DateTimeException e) {
                return DataResult.error(() -> "Not a time zone: " + zone);
            }
        },
        ZoneId::getId
    );

    public static final Codec<DigestConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ZONE_CODEC.optionalFieldOf("timezone", ZoneId.systemDefault()).forGetter(DigestConfig::timezone),
        Schedule.CODEC.fieldOf("schedule").forGetter(DigestConfig::schedule),
        StatGroup.CODEC.listOf().validate(DigestConfig::uniqueIds).fieldOf("groups").forGetter(DigestConfig::groups)
    ).apply(instance, DigestConfig::new));

    private static DataResult<List<StatGroup>> uniqueIds(List<StatGroup> groups) {
        final var ids = new HashSet<String>();
        return groups.stream()
            .filter(group -> !ids.add(group.id()))
            .findFirst()
            .map(group -> DataResult.<List<StatGroup>>error(() -> "Group ID used more than once: " + group.id()))
            .orElseGet(() -> DataResult.success(groups));
    }

    /**
     * Stats added up together for each player, e.g. every block they've mined.
     *
     * @param id    Names the group in templates. Only a-z, 0-9, and _.
     * @param stats The stats added up for each player.
     */
    public record StatGroup(String id, StatSelection stats) {
        // Template variables are written like `${miner.total}`, so IDs can't have dots or dashes
        private static final Codec<String> ID_CODEC = Codec.STRING.validate(id -> id.matches("[a-z0-9_]+")
            ? DataResult.success(id)
            : DataResult.error(() -> "Group IDs can only use a-z, 0-9, and _: " + id));

        public static final Codec<StatGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ID_CODEC.fieldOf("id").forGetter(StatGroup::id),
            StatSelection.CODEC.fieldOf("stats").forGetter(StatGroup::stats)
        ).apply(instance, StatGroup::new));
    }
}
