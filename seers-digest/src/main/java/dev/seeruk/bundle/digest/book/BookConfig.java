package dev.seeruk.bundle.digest.book;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.util.StringRepresentable;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * How each issue's book looks and what it says, from {@code book.yml}.
 *
 * @param title      The book's name, shown on the item. At most 32 characters, as in Vanilla.
 * @param author     Who the book is by, shown in its tooltip.
 * @param dateFormat How {@code issue.from} and {@code issue.to} are shown, as a
 *                   {@link DateTimeFormatter} pattern.
 * @param cover      The cover page.
 * @param contents   The contents page.
 * @param continued  Added to a section's title on each page it carries on to, in the title's style.
 * @param sections   The book's sections, in order.
 */
public record BookConfig(
    String title,
    String author,
    String dateFormat,
    Cover cover,
    Contents contents,
    String continued,
    List<Section> sections
) {
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

    public static final Codec<BookConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        TITLE_CODEC.fieldOf("title").forGetter(BookConfig::title),
        Codec.STRING.fieldOf("author").forGetter(BookConfig::author),
        DATE_FORMAT_CODEC.fieldOf("date_format").forGetter(BookConfig::dateFormat),
        Cover.CODEC.fieldOf("cover").forGetter(BookConfig::cover),
        Contents.CODEC.fieldOf("contents").forGetter(BookConfig::contents),
        Codec.STRING.fieldOf("continued").forGetter(BookConfig::continued),
        Section.CODEC.listOf().validate(BookConfig::uniqueIds).fieldOf("sections").forGetter(BookConfig::sections)
    ).apply(instance, BookConfig::new));

    private static DataResult<List<Section>> uniqueIds(List<Section> sections) {
        final var ids = new HashSet<String>();
        return sections.stream()
            .filter(section -> !ids.add(section.id()))
            .findFirst()
            .map(section -> DataResult.<List<Section>>error(() -> "Section ID used more than once: " + section.id()))
            .orElseGet(() -> DataResult.success(sections));
    }

    /**
     * Shows dates as the date format says.
     */
    public DateTimeFormatter dates() {
        return DateTimeFormatter.ofPattern(dateFormat, Locale.ROOT);
    }

    /**
     * The groups the book names that aren't among the given groups, e.g. in {@code ranking: miner}.
     */
    public List<String> unknownGroups(Collection<String> groups) {
        return sections.stream()
            .flatMap(section -> section instanceof Listed listed ? listed.groups() : Stream.empty())
            .filter(group -> !groups.contains(group))
            .distinct()
            .toList();
    }

    public enum Align implements StringRepresentable {
        LEFT("left"),
        CENTRE("centre");

        public static final Codec<Align> CODEC = StringRepresentable.fromEnum(Align::values);

        private final String name;

        Align(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * @param align    How each of the cover's lines is aligned.
     * @param template What the cover says.
     */
    public record Cover(Align align, TextTemplate template) {
        public static final Codec<Cover> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Align.CODEC.optionalFieldOf("align", Align.LEFT).forGetter(Cover::align),
            TextTemplate.CODEC.fieldOf("template").forGetter(Cover::template)
        ).apply(instance, Cover::new));
    }

    /**
     * @param title The contents page's title.
     * @param entry Each section's entry, with {@code ${section}} for its title and {@code ${page}}
     *              for the page it starts on.
     * @param hover Shown when an entry is hovered, with {@code ${page}}.
     */
    public record Contents(TextTemplate title, TextTemplate entry, TextTemplate hover) {
        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextTemplate.CODEC.fieldOf("title").forGetter(Contents::title),
            TextTemplate.CODEC.fieldOf("entry").forGetter(Contents::entry),
            TextTemplate.CODEC.fieldOf("hover").forGetter(Contents::hover)
        ).apply(instance, Contents::new));
    }

    /**
     * Ways of writing an entry, one of which is picked for each issue from those that read well.
     */
    public record Variations(List<TextTemplate> templates) {
        public static final Codec<Variations> CODEC = TextTemplate.CODEC.listOf()
            .validate(templates -> templates.isEmpty()
                ? DataResult.error(() -> "An entry needs at least one way of writing it")
                : DataResult.success(templates))
            .xmap(Variations::new, Variations::templates);
    }

    /**
     * How each item of a list is written, either one way for every item, or (for
     * {@link ListSource.Kind#LEADERS}) a way for each group, falling back to {@code default}.
     *
     * @param fallback For items without their own way of being written.
     * @param byGroup  Each group's own way of being written, by its ID.
     */
    public record ListEntry(Variations fallback, Map<String, Variations> byGroup) {
        /** The key for items without their own way of being written, so no group can be called it. */
        public static final String DEFAULT = "default";

        public static final Codec<ListEntry> CODEC = Codec.either(
            Variations.CODEC,
            Codec.unboundedMap(Codec.STRING, Variations.CODEC)
        ).comapFlatMap(
            either -> either.map(
                variations -> DataResult.success(new ListEntry(variations, Map.of())),
                ListEntry::byGroup
            ),
            entry -> entry.byGroup.isEmpty() ? Either.left(entry.fallback) : Either.right(entry.written())
        );

        private static DataResult<ListEntry> byGroup(Map<String, Variations> written) {
            final var fallback = written.get(DEFAULT);
            if (fallback == null) {
                return DataResult.error(() -> "An entry written for each group needs a " + DEFAULT);
            }

            final var byGroup = new HashMap<>(written);
            byGroup.remove(DEFAULT);
            return DataResult.success(new ListEntry(fallback, Map.copyOf(byGroup)));
        }

        private Map<String, Variations> written() {
            final var written = new HashMap<>(byGroup);
            written.put(DEFAULT, fallback);
            return written;
        }

        /**
         * How an item in the group is written.
         */
        public Variations forGroup(String group) {
            return byGroup.getOrDefault(group, fallback);
        }
    }

    /**
     * A part of the book, starting on a page of its own.
     */
    public sealed interface Section permits Fixed, Listed {
        // A section with a list or a ranking says so, and every other section is fixed. Which it is
        // is decided first, so a mistake is only reported for the kind of section it's in.
        Codec<Section> CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<Section, T>> decode(DynamicOps<T> ops, T input) {
                return ops.get(input, "list").result().isPresent() || ops.get(input, "ranking").result().isPresent()
                    ? Listed.CODEC.decode(ops, input).map(decoded -> decoded.mapFirst(listed -> listed))
                    : Fixed.CODEC.decode(ops, input).map(decoded -> decoded.mapFirst(fixed -> fixed));
            }

            @Override
            public <T> DataResult<T> encode(Section section, DynamicOps<T> ops, T prefix) {
                return switch (section) {
                    case Listed listed -> Listed.CODEC.encode(listed, ops, prefix);
                    case Fixed fixed -> Fixed.CODEC.encode(fixed, ops, prefix);
                };
            }
        };

        /** Names the section, so it reads the same however the sections are ordered. */
        String id();

        TextTemplate title();

        /** The most pages it can take, or 0 for as many as it needs. */
        int maxPages();
    }

    /**
     * A section that says the same things in every issue.
     *
     * @param id       Names the section, so it reads the same however the sections are ordered.
     * @param title    Shown at its top, and in the contents.
     * @param maxPages The most pages it can take, or 0 for as many as it needs.
     * @param entries  What it says, in order.
     */
    public record Fixed(String id, TextTemplate title, int maxPages, List<Variations> entries)
        implements Section {
        static final Codec<Fixed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Fixed::id),
            TextTemplate.CODEC.fieldOf("title").forGetter(Fixed::title),
            Codec.intRange(0, BookLayout.MAX_PAGES).optionalFieldOf("max_pages", 0).forGetter(Fixed::maxPages),
            Variations.CODEC.listOf().fieldOf("entries").forGetter(Fixed::entries)
        ).apply(instance, Fixed::new));
    }

    /**
     * A section with an entry for each item of a list, or of a group's ranking.
     *
     * @param id       Names the section, so it reads the same however the sections are ordered.
     * @param title    Shown at its top, and in the contents.
     * @param maxPages The most pages it can take, or 0 for as many as it needs.
     * @param source   Which list or ranking it shows.
     * @param limit    The most items it shows, or 0 for all of them.
     * @param intro    What it says before the items.
     * @param entry    How each item is written.
     * @param more     What it says after the items, if any were left out.
     */
    public record Listed(
        String id,
        TextTemplate title,
        int maxPages,
        ListSource source,
        int limit,
        Optional<Variations> intro,
        ListEntry entry,
        Optional<Variations> more
    ) implements Section {
        static final Codec<Listed> CODEC = RecordCodecBuilder.<Listed>create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Listed::id),
            TextTemplate.CODEC.fieldOf("title").forGetter(Listed::title),
            Codec.intRange(0, BookLayout.MAX_PAGES).optionalFieldOf("max_pages", 0).forGetter(Listed::maxPages),
            ListSource.MAP_CODEC.forGetter(Listed::source),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("limit", 0).forGetter(Listed::limit),
            Variations.CODEC.optionalFieldOf("intro").forGetter(Listed::intro),
            ListEntry.CODEC.fieldOf("entry").forGetter(Listed::entry),
            Variations.CODEC.optionalFieldOf("more").forGetter(Listed::more)
        ).apply(instance, Listed::new)).validate(listed ->
            listed.entry.byGroup.isEmpty() || listed.source.kind() == ListSource.Kind.LEADERS
                ? DataResult.success(listed)
                : DataResult.error(() -> "Only the leaders list can be written for each group: " + listed.id));

        // The groups this section names
        private Stream<String> groups() {
            return Stream.concat(source.group().stream(), entry.byGroup.keySet().stream());
        }
    }
}
