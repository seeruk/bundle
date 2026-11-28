package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.core.testing.TextChunks;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dev.seeruk.bundle.core.testing.TextChunks.chunk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookLayoutTest {
    private static final BookLayout.Wording WORDING = new BookLayout.Wording(
        Component.literal("Contents").withStyle(ChatFormatting.BOLD),
        " (cont.)",
        (title, page) -> Component.empty().append(title).append(" - " + page),
        page -> Component.literal("Go to page " + page)
    );

    // Pages are laid out 13 lines tall, and 19 a's wide (most letters are 6 pixels wide, and pages
    // are 114)

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // Each page's lines, as plain text
    private static List<List<String>> layOut(Component cover, List<BookSection> sections) {
        return BookLayout.layOut(cover, false, sections, WORDING).pages().stream()
            .map(page -> Arrays.asList(page.getString().split("\n", -1)))
            .toList();
    }

    private static List<List<String>> layOut(List<BookSection> sections) {
        return layOut(Component.literal("Cover"), sections);
    }

    private static BookSection section(String title, Component... entries) {
        return new BookSection(Component.literal(title), List.of(entries), 0, left -> Optional.empty());
    }

    // A section of at most the given pages, ending with how many entries were left out, if any
    private static BookSection limited(int maxPages, Component... entries) {
        return new BookSection(
            Component.literal("One"),
            List.of(entries),
            maxPages,
            left -> left == 0 ? Optional.empty() : Optional.of(Component.literal("...and " + left + " more"))
        );
    }

    // An entry of the given number of lines, numbered from 1
    private static Component entry(int lines) {
        return Component.literal(String.join("\n", lines(lines)));
    }

    private static List<String> lines(int lines) {
        return IntStream.rangeClosed(1, lines).mapToObj(Integer::toString).toList();
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    @Test
    void startsWithTheCoverThenTheContents() {
        final var pages = layOut(List.of(section("Awards", Component.literal("Miner"))));

        assertEquals(List.of(List.of("Cover"), List.of("Contents", "Awards - 3"), List.of("Awards", "", "Miner")), pages);
    }

    @Test
    void cutsTheCoverShortToOnePage() {
        final var pages = layOut(entry(20), List.of());

        assertEquals(lines(13), pages.getFirst());
        assertEquals(List.of("Contents"), pages.get(1));
    }

    @Test
    void startsEachSectionOnANewPage() {
        final var pages = layOut(List.of(section("One", entry(1)), section("Two", entry(1))));

        assertEquals(List.of("Contents", "One - 3", "Two - 4"), pages.get(1));
        assertEquals(List.of("One", "", "1"), pages.get(2));
        assertEquals(List.of("Two", "", "1"), pages.get(3));
    }

    @Test
    void separatesEntriesWithABlankLine() {
        final var pages = layOut(List.of(section("One", entry(2), entry(1))));

        assertEquals(List.of("One", "", "1", "2", "", "1"), pages.get(2));
    }

    @Test
    void movesAnEntryThatDoesNotFitToTheNextPageWhole() {
        final var pages = layOut(List.of(section("One", entry(10), entry(5))));

        assertEquals(concat(List.of("One", ""), lines(10)), pages.get(2));
        assertEquals(concat(List.of("One (cont.)", ""), lines(5)), pages.get(3));
    }

    @Test
    void carriesAnEntryLongerThanAPageOverTheNextPage() {
        final var pages = layOut(List.of(section("One", entry(3), entry(20))));

        assertEquals(concat(concat(List.of("One", ""), lines(3)), concat(List.of(""), lines(7))), pages.get(2));
        // Carrying on with the same entry, so without a blank line after the title
        assertEquals(concat(List.of("One (cont.)"), lines(20).subList(7, 19)), pages.get(3));
        assertEquals(List.of("One (cont.)", "20"), pages.get(4));
    }

    @Test
    void neverLeavesATitleOnItsOwn() {
        final var pages = layOut(List.of(section("One", entry(12))));

        assertEquals(concat(List.of("One", ""), lines(11)), pages.get(2));
        assertEquals(List.of("One (cont.)", "12"), pages.get(3));
    }

    @Test
    void carriesASectionOverAsManyPagesAsItNeeds() {
        final var pages = layOut(List.of(section("One", entry(11), entry(11), entry(11)), section("Two", entry(1))));

        assertEquals(List.of("Contents", "One - 3", "Two - 6"), pages.get(1));
        assertEquals(6, pages.size());
        assertEquals(concat(List.of("One (cont.)", ""), lines(11)), pages.get(3));
        assertEquals(List.of("Two", "", "1"), pages.get(5));
    }

    @Test
    void endsALimitedSectionWithHowManyEntriesWereLeftOut() {
        // An entry of 11 lines fills a page after the title, so only one fits with the more line
        final var pages = layOut(List.of(limited(2, entry(11), entry(11), entry(11), entry(11), entry(11))));

        assertEquals(List.of("Contents", "One - 3"), pages.get(1));
        assertEquals(concat(List.of("One", ""), lines(11)), pages.get(2));
        assertEquals(List.of("One (cont.)", "", "...and 4 more"), pages.get(3));
        assertEquals(4, pages.size());
    }

    @Test
    void keepsEveryEntryOfALimitedSectionThatFits() {
        final var pages = layOut(List.of(limited(2, entry(1), entry(1))));

        assertEquals(List.of("One", "", "1", "", "1"), pages.get(2));
        assertEquals(3, pages.size());
    }

    @Test
    void keepsOnlyTheTitleAndMoreLineIfNoEntryFits() {
        final var pages = layOut(List.of(limited(1, entry(20))));

        assertEquals(List.of("One", "", "...and 1 more"), pages.get(2));
    }

    @Test
    void centresTheCoverWhenItsCentred() {
        // aa is 12 pixels wide, so 12 spaces of 4 pixels centre it on a 114 pixel page
        final var pages = BookLayout.layOut(Component.literal("aa\n\naa"), true, List.of(), WORDING).pages();

        assertEquals(" ".repeat(12) + "aa\n\n" + " ".repeat(12) + "aa", pages.getFirst().getString());
    }

    @Test
    void marksCarriedOnTitlesInTheTitlesStyle() {
        final var style = Style.EMPTY.withBold(true).withColor(ChatFormatting.DARK_RED);
        final var section = new BookSection(Component.literal("One").withStyle(style), List.of(entry(12)), 0, left -> Optional.empty());
        final var pages = BookLayout.layOut(Component.literal("Cover"), false, List.of(section), WORDING).pages();

        assertEquals(
            List.of(chunk("One", style), chunk(" (cont.)", style), chunk("\n", Style.EMPTY), chunk("12", Style.EMPTY)),
            TextChunks.chunks(pages.get(3))
        );
    }

    @Test
    void marksCarriedOnTitlesInTheStyleTemplatesGiveThem() {
        // Templates style the text inside the title, rather than the title itself
        final var style = Style.EMPTY.withBold(true).withColor(ChatFormatting.DARK_RED);
        final var title = Component.empty().append(Component.literal("One").withStyle(style));
        final var section = new BookSection(title, List.of(entry(12)), 0, left -> Optional.empty());
        final var pages = BookLayout.layOut(Component.literal("Cover"), false, List.of(section), WORDING).pages();

        assertEquals(List.of(chunk("One", style), chunk(" (cont.)", style)), TextChunks.chunks(pages.get(3)).subList(0, 2));
    }

    @Test
    void usesTheWordingItsGiven() {
        final var wording = new BookLayout.Wording(
            Component.literal("Inhalt"),
            " (Forts.)",
            (title, page) -> Component.empty().append(title).append(" (S. " + page + ")"),
            page -> Component.literal("Seite " + page)
        );
        final var book = BookLayout.layOut(Component.literal("Cover"), false, List.of(section("One", entry(12))), wording);

        assertEquals("Inhalt\nOne (S. 3)", book.pages().get(1).getString());
        assertEquals("One (Forts.)\n12", book.pages().get(3).getString());
        final var hovers = TextChunks.chunks(book.pages().get(1)).stream()
            .map(chunk -> chunk.style().getHoverEvent())
            .filter(HoverEvent.ShowText.class::isInstance)
            .map(event -> ((HoverEvent.ShowText) event).value().getString())
            .distinct()
            .toList();
        assertEquals(List.of("Seite 3"), hovers);
    }

    @Test
    void leavesTitlesStyledAsTheyAre() {
        final var pages = BookLayout.layOut(Component.literal("Cover"), false, List.of(section("One", entry(1))), WORDING).pages();

        assertTrue(TextChunks.chunks(pages.get(2)).stream().noneMatch(chunk -> chunk.style().isBold()), "Title was made bold");
    }

    @Test
    void wrapsLinesTooWideForAPage() {
        final var pages = layOut(List.of(section("One", Component.literal("a".repeat(19) + " " + "a".repeat(5)))));

        assertEquals(List.of("One", "", "a".repeat(19), "a".repeat(5)), pages.get(2));
    }

    @Test
    void measuresBoldTitlesAsBold() {
        // 19 a's fit on a line, but only 16 bold ones do, as bold adds a pixel to each
        final var title = Component.literal("a".repeat(19)).withStyle(ChatFormatting.BOLD);
        final var section = new BookSection(title, List.of(entry(1)), 0, left -> Optional.empty());
        final var pages = layOut(List.of(section));

        assertEquals(List.of("a".repeat(16), "a".repeat(3), "", "1"), pages.get(2));
    }

    @Test
    void showsWhereContentsEntriesGoWhenHovered() {
        final var book = BookLayout.layOut(Component.literal("Cover"), false, List.of(section("One", entry(1))), WORDING);

        final var hovers = TextChunks.chunks(book.pages().get(1)).stream()
            .map(chunk -> chunk.style().getHoverEvent())
            .filter(HoverEvent.ShowText.class::isInstance)
            .map(event -> ((HoverEvent.ShowText) event).value().getString())
            .distinct()
            .toList();

        assertEquals(List.of("Go to page 3"), hovers);
    }

    @Test
    void linksContentsEntriesToTheirPages() {
        // 20 entries and the heading take two pages of contents, so the first section is page 4
        final var sections = IntStream.rangeClosed(1, 20).mapToObj(index -> section("S" + index, entry(1))).toList();
        final var book = BookLayout.layOut(Component.literal("Cover"), false, sections, WORDING);

        final var targets = book.pages().subList(1, 3).stream()
            .flatMap(page -> TextChunks.chunks(page).stream())
            .map(chunk -> chunk.style().getClickEvent())
            .filter(ClickEvent.ChangePage.class::isInstance)
            .map(event -> ((ClickEvent.ChangePage) event).page())
            .distinct()
            .toList();

        assertEquals(IntStream.rangeClosed(4, 23).boxed().toList(), targets);
        assertEquals("S1", book.pages().get(3).getString().split("\n")[0]);
    }

    @Test
    void startsSectionsRightAfterContentsThatTakeFewerPagesThanFirstMeasured() {
        // "aaaaaaaaaaaaaa - 100" is too wide for a line, but with a 2-digit page number it fits.
        // Measured with 3 digits, the heading and 13 two-line entries take 3 pages. With their real
        // page numbers, they take 2, so the first section starts on page 4.
        final var sections = IntStream.rangeClosed(1, 13).mapToObj(index -> section("a".repeat(14), entry(1))).toList();
        final var book = BookLayout.layOut(Component.literal("Cover"), false, sections, WORDING);

        assertEquals(List.of("Contents", "a".repeat(14) + " - 4"), List.of(book.pages().get(1).getString().split("\n")).subList(0, 2));
        assertEquals(List.of("a".repeat(14), "", "1"), List.of(book.pages().get(3).getString().split("\n")));
        assertEquals(1 + 2 + 13, book.pages().size());
    }

    @Test
    void leavesOutSectionsFromTheEndToFitTheMostPages() {
        // With 91 one-page sections, the cover and 8 pages of contents make 100 pages
        final var sections = IntStream.rangeClosed(1, 120).mapToObj(index -> section("S" + index, entry(1))).toList();
        final var book = BookLayout.layOut(Component.literal("Cover"), false, sections, WORDING);

        assertEquals(BookLayout.MAX_PAGES, book.pages().size());
        assertEquals(sections.subList(91, 120), book.leftOut());
        assertTrue(book.pages().getLast().getString().startsWith("S91"));
    }
}
