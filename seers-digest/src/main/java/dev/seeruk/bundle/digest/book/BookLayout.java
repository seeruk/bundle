package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.text.TextComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

/**
 * Lays out a book's pages: a cover, a contents page linking to each section, then each section
 * starting on a new page. Lines are fitted to Vanilla's book pages, measured as the client's
 * default font would show them (see {@code TextWidths}).
 */
public final class BookLayout {
    /** The most pages a Vanilla book can have. */
    public static final int MAX_PAGES = 100;

    static final int PAGE_WIDTH = 114;
    // Pages show 14 lines, but one is left spare, so a line the client wraps once more than
    // expected (e.g. with a resource pack's wider font) moves into it rather than off the page
    static final int PAGE_LINES = 13;

    // While sections are being left out, contents entries are measured with the widest page
    // number a book can have, so the contents take at least as many pages as they'll need
    private static final int WIDEST_PAGE = MAX_PAGES;

    private BookLayout() {}

    /**
     * A laid out book.
     *
     * @param pages   Every page, in order.
     * @param leftOut Sections that didn't fit within {@link #MAX_PAGES}, which were left out.
     */
    public record LaidOut(List<Component> pages, List<BookSection> leftOut) {}

    /**
     * The words the layout adds to a book, around what its sections say.
     *
     * @param contents  The contents page's title.
     * @param continued Added to a title on each page its section carries on to, in the title's
     *                  style, e.g. " (cont.)".
     * @param entry     A contents entry, given a section's title and the page it starts on.
     * @param goTo      Shown when a contents entry is hovered, given the page it goes to.
     */
    public record Wording(
        Component contents,
        String continued,
        BiFunction<Component, Integer, Component> entry,
        IntFunction<Component> goTo
    ) {}

    /**
     * Lays out the cover, the contents, and the sections. The cover is always one page, cut short
     * if it's too long. Sections are left out from the end, whole, until the book fits within
     * {@link #MAX_PAGES}.
     *
     * @param cover        The cover page.
     * @param centredCover Whether each of the cover's lines is centred.
     * @param sections     Each section, in order.
     * @param wording      The words the layout adds.
     */
    public static LaidOut layOut(Component cover, boolean centredCover, List<BookSection> sections, Wording wording) {
        final var sectionPages = sections.stream().map(section -> pagesOf(section, wording)).toList();
        var kept = sections.size();
        var contentsPages = contents(sections, List.of(), wording).size();

        while (kept > 0 && 1 + contentsPages + pageCount(sectionPages.subList(0, kept)) > MAX_PAGES) {
            kept--;
            contentsPages = contents(sections.subList(0, kept), List.of(), wording).size();
        }

        // Real page numbers are never wider than the widest, so the contents can only take the
        // same number of pages with them, or fewer. Fewer moves every section forward, which can
        // only make the numbers narrower again, so this is repeated until the contents stop
        // shrinking, when their page numbers are right.
        final var keptSections = sections.subList(0, kept);
        final var keptPages = sectionPages.subList(0, kept);
        var contents = contents(keptSections, starts(keptPages, contentsPages), wording);
        while (contents.size() < contentsPages) {
            contentsPages = contents.size();
            contents = contents(keptSections, starts(keptPages, contentsPages), wording);
        }

        final var pages = new ArrayList<List<Component>>();
        pages.add(lines(cover, centredCover).stream().limit(PAGE_LINES).toList());
        pages.addAll(contents);
        keptPages.forEach(pages::addAll);

        return new LaidOut(
            pages.stream().map(BookLayout::join).toList(),
            List.copyOf(sections.subList(kept, sections.size()))
        );
    }

    // The page each section starts on, after the cover, the contents, and every section before it
    private static List<Integer> starts(List<List<List<Component>>> sectionPages, int contentsPages) {
        final var starts = new ArrayList<Integer>();
        var next = 2 + contentsPages;
        for (final var pages : sectionPages) {
            starts.add(next);
            next += pages.size();
        }
        return starts;
    }

    // The section's pages, with as many of its entries as fit in its most pages, then its more
    // entry for the rest. Fewer entries never take more pages, so the most that fit can be found
    // by halving.
    private static List<List<Component>> pagesOf(BookSection section, Wording wording) {
        final var wrapped = Wrapped.of(section, wording);
        final var all = wrapped.pagesWithFirstNEntries(section.entries().size());
        if (section.maxPages() == 0 || all.size() <= section.maxPages()) {
            return all;
        }

        var fits = 0;
        var tooMany = section.entries().size();
        while (tooMany - fits > 1) {
            final var middle = (fits + tooMany) / 2;
            if (wrapped.pagesWithFirstNEntries(middle).size() <= section.maxPages()) {
                fits = middle;
            } else {
                tooMany = middle;
            }
        }
        return wrapped.pagesWithFirstNEntries(fits);
    }

    /**
     * A section's lines, wrapped once, so it can be laid out with different numbers of entries.
     *
     * @param section   The section.
     * @param title     Its title's lines.
     * @param continued Its title's lines on each page it carries on to.
     * @param entries   Each entry's lines.
     */
    private record Wrapped(
        BookSection section,
        List<Component> title,
        List<Component> continued,
        List<List<Component>> entries
    ) {
        static Wrapped of(BookSection section, Wording wording) {
            return new Wrapped(
                section,
                lines(section.title(), false),
                lines(continuedTitle(section.title(), wording), false),
                section.entries().stream().map(entry -> lines(entry, false)).toList()
            );
        }

        // The section's pages with only its first entries, then its more entry for those left out
        List<List<Component>> pagesWithFirstNEntries(int kept) {
            final var pager = new Pager(continued);
            pager.add(title, false);
            entries.subList(0, kept).forEach(entry -> pager.add(entry, true));
            section.more().apply(entries.size() - kept).ifPresent(more -> pager.add(lines(more, false), true));
            return pager.pages();
        }
    }

    // The contents' pages, with each entry going to its section's start page, or showing the
    // widest page number if the starts aren't known yet
    private static List<List<Component>> contents(List<BookSection> sections, List<Integer> starts, Wording wording) {
        final var pager = new Pager(lines(continuedTitle(wording.contents(), wording), false));
        pager.add(lines(wording.contents(), false), false);
        for (var index = 0; index < sections.size(); index++) {
            final var page = starts.isEmpty() ? WIDEST_PAGE : starts.get(index);
            pager.add(lines(contentsEntry(sections.get(index).title(), page, wording), false), false);
        }
        return pager.pages();
    }

    // e.g. "Spotlight - 4", going to that page when clicked
    private static Component contentsEntry(Component title, int page, Wording wording) {
        return Component.empty()
            .append(wording.entry().apply(title, page))
            .withStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.ChangePage(page))
                .withHoverEvent(new HoverEvent.ShowText(wording.goTo().apply(page))));
    }

    // The title as it's shown on each page its section carries on to
    private static Component continuedTitle(Component title, Wording wording) {
        return Component.empty().append(title).append(Component.literal(wording.continued()).withStyle(lastStyle(title)));
    }

    // The style the text ends in, so more can be added to it looking the same, e.g. a title's
    // colour and boldness, which a template puts on the text inside the title rather than on it
    private static Style lastStyle(Component text) {
        final var last = new Style[] {Style.EMPTY};
        text.visit(
            (style, content) -> {
                if (!content.isEmpty()) {
                    last[0] = style;
                }
                return Optional.empty();
            }, Style.EMPTY
        );
        return last[0];
    }

    // The text's lines, as it would be shown on a page
    private static List<Component> lines(Component text, boolean centred) {
        return TextComponents.splitLines(text).stream()
            .flatMap(line -> TextComponents.wrapToWidth(line, PAGE_WIDTH).stream())
            .map(line -> centred ? TextComponents.centre(line, PAGE_WIDTH) : line)
            .toList();
    }

    private static int pageCount(List<List<List<Component>>> sections) {
        return sections.stream().mapToInt(List::size).sum();
    }

    private static Component join(List<Component> lines) {
        final var page = Component.empty();
        for (var index = 0; index < lines.size(); index++) {
            if (index > 0) {
                page.append("\n");
            }
            page.append(lines.get(index));
        }
        return page;
    }

    /**
     * Fills pages with lines, keeping each group of lines (e.g. an entry) on one page where it
     * can. Every page after the first starts with a heading (e.g. a title marked as carried on).
     */
    private static final class Pager {
        private final List<Component> continued;
        private final List<List<Component>> pages = new ArrayList<>();
        private List<Component> page = new ArrayList<>();
        // How many groups of lines are on the current page, counting its heading
        private int groups = 0;

        Pager(List<Component> continued) {
            this.continued = continued;
        }

        /**
         * Adds the lines after what's already there, after a blank line if they're spaced and not
         * at the top of a page. If they don't fit in what's left of the page, but would fit on the
         * next one, they go on the next page. Otherwise they carry on over the next page. The
         * first lines after a page's first group (its title or heading) never move, so a title
         * isn't left on its own.
         */
        void add(List<Component> lines, boolean spaced) {
            final var fits = page.size() + gap(page.size(), spaced) + lines.size() <= PAGE_LINES;
            final var fitsNext = continued.size() + gap(continued.size(), spaced) + lines.size() <= PAGE_LINES;

            if (!fits && fitsNext && groups > 1) {
                newPage();
            } else if (gap(page.size(), spaced) > 0 && page.size() + 1 >= PAGE_LINES) {
                // No room for the blank line and a line after it
                newPage();
            }
            if (gap(page.size(), spaced) > 0) {
                page.add(Component.empty());
            }

            groups++;
            for (final var line : lines) {
                if (page.size() == PAGE_LINES) {
                    newPage();
                }
                page.add(line);
            }
        }

        List<List<Component>> pages() {
            if (!page.isEmpty()) {
                newPage();
            }
            return List.copyOf(pages);
        }

        // A blank line goes before spaced lines, unless they're at the top of a page
        private static int gap(int linesAbove, boolean spaced) {
            return spaced && linesAbove > 0 ? 1 : 0;
        }

        private void newPage() {
            pages.add(List.copyOf(page));
            page = new ArrayList<>(continued);
            groups = 1;
        }
    }
}
