package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.text.TextComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

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
     * Lays out the cover, the contents, and the sections. The cover is always one page, cut short
     * if it's too long. Sections are left out from the end, whole, until the book fits within
     * {@link #MAX_PAGES}.
     */
    public static LaidOut layOut(Component cover, List<BookSection> sections) {
        final var sectionPages = sections.stream().map(BookLayout::pagesOf).toList();
        var kept = sections.size();
        var contentsPages = contents(sections, List.of()).size();

        while (kept > 0 && 1 + contentsPages + pageCount(sectionPages.subList(0, kept)) > MAX_PAGES) {
            kept--;
            contentsPages = contents(sections.subList(0, kept), List.of()).size();
        }

        // Real page numbers are never wider than the widest, so the contents can only take the
        // same number of pages with them, or fewer. Fewer moves every section forward, which can
        // only make the numbers narrower again, so this is repeated until the contents stop
        // shrinking, when their page numbers are right.
        final var keptSections = sections.subList(0, kept);
        final var keptPages = sectionPages.subList(0, kept);
        var contents = contents(keptSections, starts(keptPages, contentsPages));
        while (contents.size() < contentsPages) {
            contentsPages = contents.size();
            contents = contents(keptSections, starts(keptPages, contentsPages));
        }

        final var pages = new ArrayList<List<Component>>();
        pages.add(lines(cover).stream().limit(PAGE_LINES).toList());
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

    // The section's pages, starting with its title
    private static List<List<Component>> pagesOf(BookSection section) {
        final var pager = new Pager();
        pager.add(lines(title(section.title())), false);
        section.entries().forEach(entry -> pager.add(lines(entry), true));
        return pager.pages();
    }

    // The contents' pages, with each entry going to its section's start page, or showing the
    // widest page number if the starts aren't known yet
    private static List<List<Component>> contents(List<BookSection> sections, List<Integer> starts) {
        final var pager = new Pager();
        pager.add(lines(title(Component.literal("Contents"))), false);
        for (var index = 0; index < sections.size(); index++) {
            final var page = starts.isEmpty() ? WIDEST_PAGE : starts.get(index);
            pager.add(lines(contentsEntry(sections.get(index).title(), page)), false);
        }
        return pager.pages();
    }

    // e.g. "Awards - 4", going to that page when clicked
    private static Component contentsEntry(Component title, int page) {
        return Component.empty()
            .append(title)
            .append(" - " + page)
            .withStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.ChangePage(page))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Go to page " + page))));
    }

    private static Component title(Component title) {
        return Component.empty().append(title).withStyle(Style.EMPTY.withBold(true));
    }

    // The text's lines, as it would be shown on a page
    private static List<Component> lines(Component text) {
        return TextComponents.splitLines(text).stream()
            .flatMap(line -> TextComponents.wrapToWidth(line, PAGE_WIDTH).stream())
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
     * can.
     */
    private static final class Pager {
        private final List<List<Component>> pages = new ArrayList<>();
        private List<Component> page = new ArrayList<>();
        // How many groups of lines are on the current page
        private int groups = 0;

        /**
         * Adds the lines after what's already there, after a blank line if they're spaced and not
         * at the top of a page. If they don't fit in what's left of the page, but would fit on an
         * empty one, they go on the next page. Otherwise they carry on over the next page. The
         * first lines after the page's first group never move, so a title isn't left on its own.
         */
        void add(List<Component> lines, boolean spaced) {
            final var gap = spaced && !page.isEmpty() ? 1 : 0;
            final var fits = page.size() + gap + lines.size() <= PAGE_LINES;
            final var movable = !pages.isEmpty() || groups > 1;

            if (!fits && lines.size() <= PAGE_LINES && movable) {
                newPage();
            } else if (gap > 0 && page.size() + 1 < PAGE_LINES) {
                page.add(Component.empty());
            } else if (gap > 0) {
                newPage();
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

        private void newPage() {
            pages.add(List.copyOf(page));
            page = new ArrayList<>();
            groups = 0;
        }
    }
}
