package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.core.config.TestConfigs;
import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.DigestConfig.StatGroup;
import dev.seeruk.bundle.digest.NightHours;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.api.Issue.GroupResult;
import dev.seeruk.bundle.digest.api.Issue.PlayerTotal;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookWriterTest {
    private static final UUID SEER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant STARTED = Instant.parse("2026-11-16T18:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-11-23T18:00:00Z");

    private static final DigestConfig CONFIG = new DigestConfig(
        ZoneOffset.UTC,
        Schedule.daily(LocalTime.of(18, 0)),
        new NightHours(LocalTime.MIDNIGHT, LocalTime.of(6, 0)),
        List.of(
            new StatGroup("miner", StatSelection.of(List.of("mined")), StatUnit.COUNT),
            new StatGroup("night_owl", StatSelection.of(List.of("night")), StatUnit.TIME),
            new StatGroup("angler", StatSelection.of(List.of("fish")), StatUnit.COUNT)
        )
    );

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Issue issue(int number, Map<String, GroupResult> groups, List<UUID> newPlayers, Issue.Moments moments) {
        return new Issue(number, STARTED, ENDED, groups, List.of(SEER, ALEX), newPlayers, moments);
    }

    private static Issue issue() {
        return issue(1, Map.of(), List.of(), Issue.Moments.EMPTY);
    }

    private static Issue.Moments deaths(int count) {
        final var deaths = IntStream.range(0, count)
            .mapToObj(index -> new Issue.Death(SEER, "minecraft:fall", Optional.empty(), STARTED))
            .toList();
        return new Issue.Moments(deaths, List.of(), List.of());
    }

    // A book written as book.yml is, with the given sections (as a YAML list)
    private static BookConfig book(String sections) {
        return book("${title} by ${author}\\n${issue.period} #${issue.number}, ${issue.dates}", sections);
    }

    private static BookConfig book(String cover, String sections) {
        return TestConfigs.parse("""
            title: "Seer's Digest"
            author: "The Editors"
            date_format: "d MMM yyyy"
            cover:
              align: centre
              template: "%s"
            contents:
              title: "Contents"
              entry: "${section} - ${page}"
              hover: "Go to page ${page}"
            continued: " (cont.)"
            sections:
            %s""".formatted(cover, sections.indent(2)), BookConfig.CODEC);
    }

    // A fixed section with each entry's ways of writing it (as YAML lists)
    private static String fixed(String... entries) {
        return """
            - id: fixed
              title: "Fixed"
              entries:
            """ + String.join("\n", Arrays.stream(entries).map(entry -> "  - " + entry).toList());
    }

    // A list section with an intro that counts its items, and a more line, with what it lists written
    // as in book.yml, e.g. "list: deaths" or "ranking: miner"
    private static String listed(String source, int limit, String entry) {
        return """
            - id: listed
              title: "Listed"
              %s
              limit: %d
              intro: ["${count|plural:item}"]
              entry: %s
              more: ["${more} more"]""".formatted(source, limit, entry);
    }

    private static BookWriter.Written write(Issue issue, BookConfig book) {
        return BookWriter.write(issue, CONFIG, book, id -> Component.literal(id.equals(SEER) ? "Seer" : "Alex"));
    }

    // Each section's entries, as plain text
    private static List<String> entries(BookWriter.Written written) {
        return written.sections().getFirst().entries().stream().map(Component::getString).toList();
    }

    @Test
    void fillsInTheCover() {
        final var written = write(issue(), book("[]"));

        assertEquals("Seer's Digest by The Editors\nDaily #1, 16 to 23 Nov 2026", written.cover().getString());
        assertTrue(written.centredCover(), "Cover wasn't centred");
    }

    @Test
    void leavesOnlyCoverPlaceholdersThatDoNotReadWellAsWritten() {
        final var book = book("${title} #${issue.numbr}", "[]");

        assertEquals("Seer's Digest #${issue.numbr}", write(issue(), book).cover().getString());
    }

    @Test
    void fillsInTitles() {
        final var written = write(issue(), book("""
            - id: news
              title: "${issue.period} news"
              entries: [["Hi"]]""")).sections().getFirst();

        assertEquals("Daily news", written.title().getString());
    }

    @Test
    void fillsInTheContentsWording() {
        final var wording = write(issue(), book("[]")).wording();

        assertEquals("Contents", wording.contents().getString());
        assertEquals(" (cont.)", wording.continued());
        assertEquals("Fixed - 3", wording.entry().apply(Component.literal("Fixed"), 3).getString());
        assertEquals("Go to page 3", wording.goTo().apply(3).getString());
    }

    @Test
    void writesEachEntryInAWayThatReadsWell() {
        final var written = write(issue(), book(fixed("[\"${nobody}\", \"${issue.active|plural:player} played\"]")));

        assertEquals(List.of("2 players played"), entries(written));
    }

    @Test
    void picksTheSameWayEveryTimeForTheSameIssue() {
        final var book = book(fixed("[\"One\", \"Two\", \"Three\"]"));

        assertEquals(entries(write(issue(), book)), entries(write(issue(), book)));
    }

    @Test
    void picksFromEveryWayThatReadsWell() {
        final var book = book(fixed("[\"One\", \"Two\", \"Three\", \"${nobody}\"]"));
        final var picked = new HashSet<String>();
        IntStream.rangeClosed(1, 50).forEach(number ->
            picked.addAll(entries(write(issue(number, Map.of(), List.of(), Issue.Moments.EMPTY), book))));

        assertEquals(new HashSet<>(List.of("One", "Two", "Three")), picked);
    }

    @Test
    void leavesOutEntriesAndSectionsWithNothingThatReadsWell() {
        final var written = write(issue(), book(fixed("[\"${nobody}\"]") + """

            - id: other
              title: "Other"
              entries: [["Kept"], ["${nobody}"]]"""));

        assertEquals(1, written.sections().size());
        assertEquals(List.of("Kept"), entries(written));
    }

    @Test
    void writesAnEntryForEachItemAfterTheIntro() {
        final var written = write(issue(1, Map.of(), List.of(), deaths(2)), book(listed("list: deaths", 0, "[\"${player} died to ${cause}\"]")));

        assertEquals(List.of("2 items", "Seer died to minecraft:fall", "Seer died to minecraft:fall"), entries(written));
    }

    @Test
    void leavesOutListsWithNoItems() {
        assertEquals(List.of(), write(issue(), book(listed("list: deaths", 0, "[\"${player}\"]"))).sections());
    }

    @Test
    void countsItemsLeftOutByTheLimitAndTheLayout() {
        final var more = write(issue(1, Map.of(), List.of(), deaths(5)), book(listed("list: deaths", 2, "[\"${player}\"]")))
            .sections().getFirst().more();

        assertEquals(Optional.of("3 more"), more.apply(0).map(Component::getString));
        assertEquals(Optional.of("4 more"), more.apply(1).map(Component::getString));
        // The intro was left out too, which isn't an item
        assertEquals(Optional.of("5 more"), more.apply(3).map(Component::getString));
    }

    @Test
    void countsItemsThatCantBeWrittenAsLeftOut() {
        // Only the zombie's death has a killer to write
        final var deaths = new Issue.Moments(List.of(
            new Issue.Death(SEER, "minecraft:mob_attack", Optional.of("minecraft:zombie"), STARTED),
            new Issue.Death(ALEX, "minecraft:fall", Optional.empty(), STARTED)
        ), List.of(), List.of());
        final var section = write(issue(1, Map.of(), List.of(), deaths), book(listed("list: deaths", 0, "[\"${player} was killed by ${killer}\"]")))
            .sections().getFirst();

        assertEquals(List.of("2 items", "Seer was killed by minecraft:zombie"), section.entries().stream().map(Component::getString).toList());
        assertEquals(Optional.of("1 more"), section.more().apply(0).map(Component::getString));
    }

    @Test
    void leavesOutListsWhereNoItemCanBeWritten() {
        final var written = write(issue(1, Map.of(), List.of(), deaths(2)), book(listed("list: deaths", 0, "[\"${player} was killed by ${killer}\"]")));

        assertEquals(List.of(), written.sections());
    }

    @Test
    void saysNothingMoreIfNothingWasLeftOut() {
        final var more = write(issue(1, Map.of(), List.of(), deaths(2)), book(listed("list: deaths", 0, "[\"${player}\"]")))
            .sections().getFirst().more();

        assertEquals(Optional.empty(), more.apply(0));
    }

    @Test
    void writesLeadersInEachGroupsOwnWayOrTheDefault() {
        final var groups = Map.of(
            "miner", new GroupResult(5, List.of(new PlayerTotal(SEER, 5))),
            "night_owl", new GroupResult(72_000, List.of(new PlayerTotal(ALEX, 72_000))),
            "angler", new GroupResult(0, List.of())
        );
        final var entry = "{miner: [\"${player} mined ${value|plural:block}\"], default: [\"${group}: ${player}, ${value}\"]}";
        final var written = write(issue(1, groups, List.of(), Issue.Moments.EMPTY), book(listed("list: leaders", 0, entry)));

        // In the config's order, leaving out groups nobody scored in
        assertEquals(List.of("2 items", "Seer mined 5 blocks", "Night owl: Alex, 1"), entries(written));
    }

    @Test
    void ranksAGroupsPlayers() {
        final var groups = Map.of("miner", new GroupResult(8, List.of(new PlayerTotal(ALEX, 5), new PlayerTotal(SEER, 3))));
        final var written = write(issue(1, groups, List.of(), Issue.Moments.EMPTY), book(listed("ranking: miner", 0, "[\"${rank}. ${player}, ${value}\"]")));

        assertEquals(List.of("2 items", "1. Alex, 5", "2. Seer, 3"), entries(written));
    }

    @Test
    void givesGroupsTheirLeadersAndTotals() {
        final var groups = Map.of("miner", new GroupResult(8, List.of(new PlayerTotal(ALEX, 5), new PlayerTotal(SEER, 3))));
        final var written = write(
            issue(1, groups, List.of(), Issue.Moments.EMPTY),
            book(fixed("[\"${miner.leader} led ${miner.players} with ${miner.leader.value} of ${miner.total}\"]", "[\"${angler.leader}\"]"))
        );

        assertEquals(List.of("Alex led 2 with 5 of 8"), entries(written));
    }
}
