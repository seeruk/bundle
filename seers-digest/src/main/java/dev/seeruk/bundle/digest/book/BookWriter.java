package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.book.BookConfig.Align;
import dev.seeruk.bundle.digest.book.BookConfig.Fixed;
import dev.seeruk.bundle.digest.book.BookConfig.Listed;
import dev.seeruk.bundle.digest.book.BookConfig.Section;
import dev.seeruk.bundle.digest.book.BookConfig.Variations;
import dev.seeruk.bundle.digest.book.ListItems.ListItem;
import dev.seeruk.bundle.digest.book.template.Value;
import dev.seeruk.bundle.digest.book.template.Value.Amount;
import dev.seeruk.bundle.digest.book.template.Value.Text;
import dev.seeruk.bundle.digest.book.template.Variables;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Writes an issue's book from the book's config: its cover, and each section with something to
 * say. Each entry is written in one of its ways that reads well, picked the same way every time for
 * the same issue.
 */
public final class BookWriter {
    // Where an entry is in its section, which picks how it's written: the entry's index in a fixed
    // section, or one of these in a list section
    private static final int ITEMS = 0;
    private static final int INTRO = -1;
    private static final int MORE = -2;

    private BookWriter() {}

    /**
     * An issue's book, ready to be laid out (see {@link BookLayout}).
     *
     * @param cover        The cover page.
     * @param centredCover Whether each of the cover's lines is centred.
     * @param sections     The sections with something to say, in order.
     * @param wording      The words the layout adds around the sections.
     */
    public record Written(Component cover, boolean centredCover, List<BookSection> sections, BookLayout.Wording wording) {}

    /**
     * @param issue  The issue the book is about.
     * @param config Says each group's unit, the schedule, and the time zone.
     * @param book   What the book says.
     * @param names  Gives each player's name.
     */
    public static Written write(Issue issue, DigestConfig config, BookConfig book, Function<UUID, Component> names) {
        final var variables = variables(issue, config, book, names);
        final var sections = book.sections().stream()
            .flatMap(section -> write(section, issue, config, variables, names).stream())
            .toList();

        return new Written(
            variables.format(book.cover().template()),
            book.cover().align() == Align.CENTRE,
            sections,
            wording(book, variables)
        );
    }

    // The contents' and carried-on titles' words, filled in
    private static BookLayout.Wording wording(BookConfig book, Variables variables) {
        final var contents = book.contents();
        return new BookLayout.Wording(
            variables.format(contents.title()),
            book.continued(),
            (title, page) -> variables.with(Map.of("section", new Text(title), "page", Amount.count(page))).format(contents.entry()),
            page -> variables.with(Map.of("page", Amount.count(page))).format(contents.hover())
        );
    }

    private static Optional<BookSection> write(
        Section section,
        Issue issue,
        DigestConfig config,
        Variables variables,
        Function<UUID, Component> names
    ) {
        return switch (section) {
            case Fixed fixed -> fixedSection(fixed, issue, variables);
            case Listed listed -> listedSection(listed, issue, variables, ListItems.of(listed.source(), issue, config, names));
        };
    }

    // Each entry written in a way that reads well, leaving out any that can't be, and the section
    // too if none can be
    private static Optional<BookSection> fixedSection(Fixed fixed, Issue issue, Variables variables) {
        final var entries = IntStream.range(0, fixed.entries().size())
            .mapToObj(index -> pick(fixed.entries().get(index), variables, issue, fixed.id(), index, 0))
            .flatMap(Optional::stream)
            .toList();

        return entries.isEmpty()
            ? Optional.empty()
            : Optional.of(new BookSection(title(fixed, variables), entries, fixed.maxPages(), left -> Optional.empty()));
    }

    // The intro, then an entry for each item up to the limit, then how many were left out, or
    // nothing if no item's entry can be written in a way that reads well (e.g. it has no items)
    private static Optional<BookSection> listedSection(Listed listed, Issue issue, Variables variables, List<ListItem> items) {
        final var shown = listed.limit() == 0 ? items : items.subList(0, Math.min(listed.limit(), items.size()));
        final var listVariables = variables.with(Map.of("count", Amount.count(items.size())));
        final var entries = new ArrayList<Component>();
        listed.intro().flatMap(intro -> pick(intro, listVariables, issue, listed.id(), INTRO, 0)).ifPresent(entries::add);
        final var introduced = entries.size();
        for (var index = 0; index < shown.size(); index++) {
            final var item = shown.get(index);
            final var entry = item.group().map(listed.entry()::forGroup).orElse(listed.entry().fallback());
            pick(entry, listVariables.with(item.values()), issue, listed.id(), ITEMS, index).ifPresent(entries::add);
        }

        // Items left out by the limit, and items whose entry can't be written in a way that reads
        // well, are left out before the layout leaves out any more
        final var itemEntries = entries.size() - introduced;
        if (itemEntries == 0) {
            return Optional.empty();
        }

        final var leftOutBeforeLayout = items.size() - itemEntries;
        return Optional.of(new BookSection(
            title(listed, variables),
            List.copyOf(entries),
            listed.maxPages(),
            leftOutByLayout -> more(listed, listVariables, issue, Math.min(leftOutByLayout, itemEntries) + leftOutBeforeLayout)
        ));
    }

    // What the list says after its items about how many were left out, if any were. The layout
    // leaves entries out from the end, so if it leaves out the intro too, it's not counted as an
    // item.
    private static Optional<Component> more(Listed listed, Variables listVariables, Issue issue, int leftOut) {
        return leftOut == 0
            ? Optional.empty()
            : listed.more().flatMap(more -> pick(more, listVariables.with(Map.of("more", Amount.count(leftOut))), issue, listed.id(), MORE, 0));
    }

    private static Component title(Section section, Variables variables) {
        return variables.format(section.title());
    }

    // One of the ways of writing the entry that reads well, picked by where it is in the issue, so
    // it's picked the same way every time
    private static Optional<Component> pick(Variations variations, Variables variables, Issue issue, String section, int entry, int item) {
        final var readable = variations.templates().stream()
            .map(variables::formatIfReadable)
            .flatMap(Optional::stream)
            .toList();
        if (readable.isEmpty()) {
            return Optional.empty();
        }

        final var random = new Random(Objects.hash(issue.number(), section, entry, item));
        return Optional.of(readable.get(random.nextInt(readable.size())));
    }

    // The values every template can show
    private static Variables variables(Issue issue, DigestConfig config, BookConfig book, Function<UUID, Component> names) {
        final var dates = book.dates().withZone(config.timezone());
        final var values = new HashMap<String, Value>();
        values.put("title", Text.of(book.title()));
        values.put("author", Text.of(book.author()));
        values.put("issue.number", Amount.count(issue.number()));
        values.put("issue.dates", Text.of(IssueDates.between(issue.started(), issue.ended(), config.timezone())));
        values.put("issue.from", Text.of(dates.format(issue.started())));
        values.put("issue.to", Text.of(dates.format(issue.ended())));
        values.put("issue.period", Text.of(switch (config.schedule().every()) {
            case DAY -> "Daily";
            case WEEK -> "Weekly";
            case MONTH -> "Monthly";
        }));
        values.put("issue.active", Amount.count(issue.activePlayers().size()));
        values.put("issue.new", Amount.count(issue.newPlayers().size()));
        values.put("issue.deaths", Amount.count(issue.moments().deaths().size()));
        values.put("issue.advancements", Amount.count(issue.moments().advancements().size()));
        values.put("issue.firsts", Amount.count(issue.moments().firsts().size()));

        for (final var group : config.groups()) {
            final var result = issue.groups().getOrDefault(group.id(), new Issue.GroupResult(0, List.of()));
            values.put(group.id() + ".total", Amount.of(result.total(), group.unit()));
            values.put(group.id() + ".players", Amount.count(result.players().size()));
            result.players().stream().findFirst().ifPresent(leader -> {
                values.put(group.id() + ".leader", new Text(names.apply(leader.player())));
                values.put(group.id() + ".leader.value", Amount.of(leader.value(), group.unit()));
            });
        }

        return new Variables(values);
    }
}
