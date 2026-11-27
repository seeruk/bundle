package dev.seeruk.bundle.digest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.text.TextTemplate;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * @param timezone   The time zone the schedule and night hours are in.
 * @param schedule   When each issue is published, covering everything since the one before.
 * @param nightHours When players playing count as night owls.
 * @param groups     The stats each issue adds up, for every player.
 * @param book       How each issue's book looks.
 */
public record DigestConfig(
    ZoneId timezone,
    Schedule schedule,
    NightHours nightHours,
    List<StatGroup> groups,
    Book book
) {
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
        StatGroup.CODEC.listOf().validate(DigestConfig::uniqueIds).fieldOf("groups").forGetter(DigestConfig::groups),
        Book.CODEC.fieldOf("book").forGetter(DigestConfig::book)
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
        private static final Codec<String> ID_CODEC = Codec.STRING.validate(id -> id.matches("[a-z0-9_]+")
            ? DataResult.success(id)
            : DataResult.error(() -> "Group IDs can only use a-z, 0-9, and _: " + id));

        public static final Codec<StatGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ID_CODEC.fieldOf("id").forGetter(StatGroup::id),
            StatSelection.CODEC.fieldOf("stats").forGetter(StatGroup::stats),
            StatUnit.CODEC.optionalFieldOf("unit", StatUnit.COUNT).forGetter(StatGroup::unit)
        ).apply(instance, StatGroup::new));
    }

    /**
     * @param title      The book's name, shown on the item. At most 32 characters, as in Vanilla.
     * @param author     Who the book is by, shown in its tooltip.
     * @param dateFormat How dates are shown on the cover, as a {@link DateTimeFormatter} pattern.
     * @param cover      The cover page, with {@code ${title}}, {@code ${number}}, {@code ${from}},
     *                   {@code ${to}}, and {@code ${period}} filled in.
     */
    public record Book(String title, String author, String dateFormat, TextTemplate cover) {
        private static final int MAX_TITLE_LENGTH = 32;

        private static final Codec<String> TITLE_CODEC = Codec.STRING.validate(title -> title.length() <= MAX_TITLE_LENGTH
            ? DataResult.success(title)
            : DataResult.error(() -> "Book titles can be at most " + MAX_TITLE_LENGTH + " characters: " + title));

        private static final Codec<String> DATE_FORMAT_CODEC = Codec.STRING.validate(pattern -> {
            try {
                DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
                return DataResult.success(pattern);
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Not a date format: " + pattern);
            }
        });

        public static final Codec<Book> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TITLE_CODEC.fieldOf("title").forGetter(Book::title),
            Codec.STRING.fieldOf("author").forGetter(Book::author),
            DATE_FORMAT_CODEC.fieldOf("date_format").forGetter(Book::dateFormat),
            TextTemplate.CODEC.fieldOf("cover").forGetter(Book::cover)
        ).apply(instance, Book::new));

        /**
         * Shows dates as the date format says.
         */
        public DateTimeFormatter dates() {
            return DateTimeFormatter.ofPattern(dateFormat, Locale.ROOT);
        }
    }
}
