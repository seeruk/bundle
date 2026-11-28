package dev.seeruk.bundle.digest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.digest.book.BookConfig.ListEntry;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @param timezone   The time zone the schedule and night hours are in.
 * @param schedule   When each issue is published, covering everything since the one before.
 * @param nightHours When players playing count as night owls.
 * @param groups     The stats each issue adds up, for every player.
 */
public record DigestConfig(ZoneId timezone, Schedule schedule, NightHours nightHours, List<StatGroup> groups) {
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
        NightHours.CODEC.fieldOf("night_hours").forGetter(DigestConfig::nightHours),
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
     * @param unit  How its totals are shown.
     */
    public record StatGroup(String id, StatSelection stats, StatUnit unit) {
        // Template variables are written like `${miner.total}`, so IDs can't have dots or dashes
        // Names the book already uses: `issue` for the issue's own values (e.g. `${issue.number}`),
        // and `default` for how Spotlight writes groups without their own wording
        private static final Map<String, String> RESERVED = Map.of(
            "issue", "templates use it for the issue",
            ListEntry.DEFAULT, "the book uses it for groups written the default way"
        );

        private static final Codec<String> ID_CODEC = Codec.STRING.validate(id -> {
            if (!id.matches("[a-z0-9_]+")) {
                return DataResult.error(() -> "Group IDs can only use a-z, 0-9, and _: " + id);
            }
            return RESERVED.containsKey(id)
                ? DataResult.error(() -> "Groups can't be called " + id + ", as " + RESERVED.get(id))
                : DataResult.success(id);
        });

        public static final Codec<StatGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ID_CODEC.fieldOf("id").forGetter(StatGroup::id),
            StatSelection.CODEC.fieldOf("stats").forGetter(StatGroup::stats),
            StatUnit.CODEC.optionalFieldOf("unit", StatUnit.COUNT).forGetter(StatGroup::unit)
        ).apply(instance, StatGroup::new));
    }
}
