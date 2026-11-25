package dev.seeruk.bundle.leaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * @param refreshSeconds How often leaders are worked out again, in seconds.
 * @param announcement   What's sent to everyone when a category has a new leader.
 * @param categories     What players are ranked by.
 */
public record LeadersConfig(int refreshSeconds, Announcement announcement, List<Category> categories) {
    public static final Codec<LeadersConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, 86_400).fieldOf("refresh_seconds").forGetter(LeadersConfig::refreshSeconds),
        Announcement.CODEC.fieldOf("announcement").forGetter(LeadersConfig::announcement),
        Category.CODEC.listOf().validate(LeadersConfig::uniqueIds).fieldOf("categories").forGetter(LeadersConfig::categories)
    ).apply(instance, LeadersConfig::new));

    public Optional<Category> category(String id) {
        return categories.stream().filter(category -> category.id().equals(id)).findFirst();
    }

    private static DataResult<List<Category>> uniqueIds(List<Category> categories) {
        final var ids = new HashSet<String>();
        return categories.stream()
            .filter(category -> !ids.add(category.id()))
            .findFirst()
            .map(category -> DataResult.<List<Category>>error(() -> "Category ID used more than once: " + category.id()))
            .orElseGet(() -> DataResult.success(categories));
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
     * @param id               Names the category in commands, and on mannequins showing it.
     * @param title            Names the category to players, as {@code ${category}}.
     * @param stats            The stats added up for each player.
     * @param unit             How the total is shown.
     * @param name             A mannequin's name, with {@code ${player}}, {@code ${value}},
     *                         {@code ${rank}}, and {@code ${category}}.
     * @param description      Shown under a mannequin's name, with the same placeholders.
     * @param emptyName        A mannequin's name when nobody holds its rank, with {@code ${rank}}
     *                         and {@code ${category}}.
     * @param emptyDescription Shown under it when nobody holds its rank.
     * @param item             What mannequins hold, if anything.
     */
    public record Category(
        String id,
        TextTemplate title,
        StatSelection stats,
        StatUnit unit,
        TextTemplate name,
        TextTemplate description,
        TextTemplate emptyName,
        TextTemplate emptyDescription,
        Optional<Item> item
    ) {
        private static final Codec<String> ID_CODEC = Codec.STRING.validate(id -> id.matches("[a-z0-9_-]+")
            ? DataResult.success(id)
            : DataResult.error(() -> "Category ID can only use a-z, 0-9, _ and -: " + id));

        public static final Codec<Category> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ID_CODEC.fieldOf("id").forGetter(Category::id),
            TextTemplate.CODEC.fieldOf("title").forGetter(Category::title),
            StatSelection.CODEC.fieldOf("stats").forGetter(Category::stats),
            StatUnit.CODEC.fieldOf("unit").forGetter(Category::unit),
            TextTemplate.CODEC.fieldOf("name").forGetter(Category::name),
            TextTemplate.CODEC.fieldOf("description").forGetter(Category::description),
            TextTemplate.CODEC.fieldOf("empty_name").forGetter(Category::emptyName),
            TextTemplate.CODEC.fieldOf("empty_description").forGetter(Category::emptyDescription),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(Category::item)
        ).apply(instance, Category::new));
    }
}
