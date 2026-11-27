package dev.seeruk.bundle.leaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @param refreshSeconds     How often leaders are worked out again, in seconds.
 * @param timezone           The time zone periods' schedules are in.
 * @param announcement       What's sent to everyone when a category has a new leader.
 * @param periodAnnouncement What's sent to everyone when a period ends, with its winners.
 * @param periods            When periodic categories start again from nothing.
 * @param categories         What players are ranked by.
 */
public record LeadersConfig(
    int refreshSeconds,
    ZoneId timezone,
    Announcement announcement,
    PeriodAnnouncement periodAnnouncement,
    List<Period> periods,
    List<Category> categories
) {
    private static final Codec<String> ID_CODEC = Codec.STRING.validate(id -> id.matches("[a-z0-9_-]+")
        ? DataResult.success(id)
        : DataResult.error(() -> "IDs can only use a-z, 0-9, _ and -: " + id));

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

    public static final Codec<LeadersConfig> CODEC = RecordCodecBuilder.<LeadersConfig>create(instance -> instance.group(
        Codec.intRange(1, 86_400).fieldOf("refresh_seconds").forGetter(LeadersConfig::refreshSeconds),
        ZONE_CODEC.optionalFieldOf("timezone", ZoneId.systemDefault()).forGetter(LeadersConfig::timezone),
        Announcement.CODEC.fieldOf("announcement").forGetter(LeadersConfig::announcement),
        PeriodAnnouncement.CODEC.fieldOf("period_announcement").forGetter(LeadersConfig::periodAnnouncement),
        Period.CODEC.listOf().validate(periods -> uniqueIds(periods, Period::id)).fieldOf("periods").forGetter(
            LeadersConfig::periods),
        Category.CODEC.listOf().validate(categories -> uniqueIds(
            categories,
            Category::id
        )).fieldOf("categories").forGetter(LeadersConfig::categories)
    ).apply(instance, LeadersConfig::new)).validate(LeadersConfig::knownPeriods);

    public Optional<Category> category(String id) {
        return categories.stream().filter(category -> category.id().equals(id)).findFirst();
    }

    /**
     * The categories that start again from nothing whenever the period ends.
     */
    public List<Category> categoriesIn(String period) {
        return categories.stream().filter(category -> category.period().equals(Optional.of(period))).toList();
    }

    private static <T> DataResult<List<T>> uniqueIds(List<T> entries, Function<T, String> id) {
        final var ids = new HashSet<String>();
        return entries.stream()
            .map(id)
            .filter(found -> !ids.add(found))
            .findFirst()
            .map(found -> DataResult.<List<T>>error(() -> "ID used more than once: " + found))
            .orElseGet(() -> DataResult.success(entries));
    }

    private static DataResult<LeadersConfig> knownPeriods(LeadersConfig config) {
        final var ids = config.periods().stream().map(Period::id).toList();
        return config.categories().stream()
            .filter(category -> category.period().isPresent() && !ids.contains(category.period().get()))
            .findFirst()
            .map(category -> DataResult.<LeadersConfig>error(() ->
                "Category '" + category.id() + "' uses period '" + category.period().get() + "', which isn't in periods"))
            .orElseGet(() -> DataResult.success(config));
    }

    /**
     * @param enabled Whether it's sent at all.
     * @param format  The message, with {@code ${player}} and {@code ${category}}.
     */
    public record Announcement(boolean enabled, TextTemplate format) {
        public static final Codec<Announcement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(Announcement::enabled),
            TextTemplate.CODEC.fieldOf("format").forGetter(Announcement::format)
        ).apply(instance, Announcement::new));
    }

    /**
     * @param enabled Whether it's sent at all.
     * @param header  The first line, with {@code ${period}}.
     * @param line    A line for each category's winner, with {@code ${category}},
     *                {@code ${player}}, and {@code ${value}}.
     */
    public record PeriodAnnouncement(boolean enabled, TextTemplate header, TextTemplate line) {
        public static final Codec<PeriodAnnouncement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(PeriodAnnouncement::enabled),
            TextTemplate.CODEC.fieldOf("header").forGetter(PeriodAnnouncement::header),
            TextTemplate.CODEC.fieldOf("line").forGetter(PeriodAnnouncement::line)
        ).apply(instance, PeriodAnnouncement::new));
    }

    /**
     * A stretch of time that periodic categories only count stats from, e.g. a month. When it ends,
     * the next one starts straight away.
     *
     * @param id       Names it in categories.
     * @param title    Names it to players, as {@code ${period}}.
     * @param schedule When each one ends, from its {@code every}, {@code on}, and {@code at}.
     */
    public record Period(String id, TextTemplate title, Schedule schedule) {
        public static final Codec<Period> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ID_CODEC.fieldOf("id").forGetter(Period::id),
            TextTemplate.CODEC.fieldOf("title").forGetter(Period::title),
            Schedule.MAP_CODEC.forGetter(Period::schedule)
        ).apply(instance, Period::new));

        /**
         * The title as it's shown to players. A title has no placeholders of its own.
         */
        public Component displayTitle() {
            return title.format(Map.of());
        }
    }

    /**
     * @param id                  Names the category in commands, and on mannequins showing it.
     * @param title               Names the category to players, as {@code ${category}}.
     * @param period              Only stats from the current period count, if set. Otherwise,
     *                            every stat ever counts.
     * @param stats               The stats added up for each player.
     * @param unit                How the total is shown.
     * @param name                A mannequin's name, with {@code ${player}}, {@code ${value}},
     *                            {@code ${rank}}, and {@code ${category}}.
     * @param description         Shown under a mannequin's name, with the same placeholders.
     * @param emptyName           A mannequin's name when nobody holds its rank, with
     *                            {@code ${rank}} and {@code ${category}}.
     * @param emptyDescription    Shown under it when nobody holds its rank.
     * @param previousDescription Shown under it instead, when nobody holds its rank but someone did
     *                            at the end of the last period, with the same placeholders as
     *                            {@code description}, for them.
     * @param item                What mannequins hold, if anything.
     * @param announceChanges     Whether a new leader is announced.
     */
    public record Category(
        String id,
        TextTemplate title,
        Optional<String> period,
        StatSelection stats,
        StatUnit unit,
        TextTemplate name,
        TextTemplate description,
        TextTemplate emptyName,
        TextTemplate emptyDescription,
        Optional<TextTemplate> previousDescription,
        Optional<Item> item,
        boolean announceChanges
    ) {
        public static final Codec<Category> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ID_CODEC.fieldOf("id").forGetter(Category::id),
            TextTemplate.CODEC.fieldOf("title").forGetter(Category::title),
            Codec.STRING.optionalFieldOf("period").forGetter(Category::period),
            StatSelection.CODEC.fieldOf("stats").forGetter(Category::stats),
            StatUnit.CODEC.fieldOf("unit").forGetter(Category::unit),
            TextTemplate.CODEC.fieldOf("name").forGetter(Category::name),
            TextTemplate.CODEC.fieldOf("description").forGetter(Category::description),
            TextTemplate.CODEC.fieldOf("empty_name").forGetter(Category::emptyName),
            TextTemplate.CODEC.fieldOf("empty_description").forGetter(Category::emptyDescription),
            TextTemplate.CODEC.optionalFieldOf("previous_description").forGetter(Category::previousDescription),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(Category::item),
            Codec.BOOL.optionalFieldOf("announce_changes", true).forGetter(Category::announceChanges)
        ).apply(instance, Category::new));

        /**
         * The title as it's shown to players. A title has no placeholders of its own.
         */
        public Component displayTitle() {
            return title.format(Map.of());
        }
    }
}
