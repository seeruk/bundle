package dev.seeruk.bundle.digest.book;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.seeruk.bundle.core.config.DistConfigs;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.SeersDigest;
import dev.seeruk.bundle.digest.book.BookConfig.Fixed;
import dev.seeruk.bundle.digest.book.BookConfig.Listed;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookConfigTest {
    private static final Map<String, Object> FIXED = Map.of("id", "headlines", "title", "Headlines", "entries", List.of(List.of("Hi")));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // A book with everything it needs, with the given settings replacing those
    private static DataResult<BookConfig> parse(Map<String, Object> settings) {
        final var book = new HashMap<String, Object>(Map.of(
            "title", "Seer's Digest",
            "author", "The Editors",
            "date_format", "d MMM yyyy",
            "cover", Map.of("template", "${title}"),
            "contents", Map.of("title", "Contents", "entry", "${section} - ${page}", "hover", "Go to page ${page}"),
            "continued", " (cont.)",
            "sections", List.of(FIXED)
        ));
        book.putAll(settings);
        return BookConfig.CODEC.parse(JavaOps.INSTANCE, book);
    }

    private static DataResult<BookConfig> parseSection(Map<String, Object> section) {
        return parse(Map.of("sections", List.of(section)));
    }

    private static Map<String, Object> ranking(String group, Object entry) {
        return Map.of("id", "ranking", "title", "Ranking", "ranking", group, "entry", entry);
    }

    private static Map<String, Object> listed(String list, Object entry) {
        return Map.of("id", "listed", "title", "Listed", "list", list, "entry", entry);
    }

    @Test
    void readsTheShippedBookWhichOnlyNamesTheShippedGroups() {
        final var config = DistConfigs.read(SeersDigest.MOD_ID, ModConfig.CONFIG, DigestConfig.CODEC);
        final var book = DistConfigs.read(SeersDigest.MOD_ID, "book", BookConfig.CODEC);

        assertEquals(List.of(), book.unknownGroups(config.groups().stream().map(DigestConfig.StatGroup::id).toList()));
    }

    @Test
    void readsEverySetting() {
        final var book = parse(Map.of("cover", Map.of("align", "centre", "template", "${title}"))).getOrThrow();

        assertEquals("Seer's Digest", book.title());
        assertEquals("The Editors", book.author());
        assertEquals("d MMM yyyy", book.dateFormat());
        assertEquals(BookConfig.Align.CENTRE, book.cover().align());
        assertEquals("${title}", book.cover().template().template());
        assertEquals("${section} - ${page}", book.contents().entry().template());
        assertEquals(" (cont.)", book.continued());
    }

    @Test
    void readsFixedSectionsWithTheirDefaults() {
        final var section = assertInstanceOf(Fixed.class, parse(Map.of()).getOrThrow().sections().getFirst());

        assertEquals("headlines", section.id());
        assertEquals(0, section.maxPages());
        assertEquals("Hi", section.entries().getFirst().templates().getFirst().template());
    }

    @Test
    void readsListSections() {
        final var section = new HashMap<>(listed("deaths", List.of("${player} died")));
        section.putAll(Map.of("max_pages", 3, "limit", 10, "intro", List.of("Deaths"), "more", List.of("${more} more")));
        final var listed = assertInstanceOf(Listed.class, parseSection(section).getOrThrow().sections().getFirst());

        assertEquals(new ListSource(ListSource.Kind.DEATHS, Optional.empty()), listed.source());
        assertEquals(3, listed.maxPages());
        assertEquals(10, listed.limit());
        assertTrue(listed.intro().isPresent(), "Intro wasn't read");
        assertTrue(listed.more().isPresent(), "More wasn't read");
    }

    @Test
    void readsAGroupsRanking() {
        final var listed = assertInstanceOf(
            Listed.class,
            parseSection(ranking("miner", List.of("${player}"))).getOrThrow().sections().getFirst()
        );

        assertEquals(new ListSource(ListSource.Kind.RANKING, Optional.of("miner")), listed.source());
    }

    @Test
    void refusesListsThatDoNotExist() {
        assertTrue(parseSection(listed("everything", List.of("${player}"))).isError());
        // A ranking is written with ranking, not as a list
        assertTrue(parseSection(listed("ranking", List.of("${player}"))).isError());
    }

    @Test
    void readsSectionsWithNeitherAListNorARankingAsFixed() {
        final var error = parseSection(Map.of("id", "fixed", "title", "Fixed", "entry", List.of("${player}"))).error().orElseThrow().message();

        assertTrue(error.contains("entries"), "Error isn't about a fixed section: " + error);
    }

    @Test
    void refusesListSectionsWithNeitherAListNorARanking() {
        final var section = Map.of("id", "listed", "title", "Listed", "entry", List.of("${player}"));
        final var error = Listed.CODEC.parse(JavaOps.INSTANCE, section).error().orElseThrow().message();

        assertTrue(error.contains("needs a list or a ranking"), "Error doesn't say what's missing: " + error);
    }

    @Test
    void refusesSectionsWithBothAListAndARanking() {
        final var section = new HashMap<>(listed("deaths", List.of("${player}")));
        section.put("ranking", "miner");

        assertTrue(parseSection(section).isError());
    }

    @Test
    void readsLeadersWrittenForEachGroup() {
        final var entry = Map.of("miner", List.of("${player} mined"), "default", List.of("${player}"));
        final var listed = assertInstanceOf(
            Listed.class,
            parseSection(listed("leaders", entry)).getOrThrow().sections().getFirst()
        );

        assertEquals("${player} mined", listed.entry().forGroup("miner").templates().getFirst().template());
        assertEquals("${player}", listed.entry().forGroup("angler").templates().getFirst().template());
    }

    @Test
    void refusesEntriesWrittenForEachGroupWithoutADefault() {
        assertTrue(parseSection(listed("leaders", Map.of("miner", List.of("${player}")))).isError());
    }

    @Test
    void refusesEntriesWrittenForEachGroupOutsideLeaders() {
        final var entry = Map.of("miner", List.of("${player}"), "default", List.of("${player}"));

        assertTrue(parseSection(listed("deaths", entry)).isError());
    }

    @Test
    void reportsMistakesOnlyForTheKindOfSectionTheyreIn() {
        final var error = parseSection(Map.of("id", "fixed", "title", "Fixed")).error().orElseThrow().message();

        assertTrue(error.contains("entries"), "Error doesn't say what's missing: " + error);
        assertTrue(!error.contains("list"), "Error is about a list section too: " + error);
    }

    @Test
    void refusesEntriesWithNoWaysOfWritingThem() {
        assertTrue(parseSection(Map.of("id", "empty", "title", "Empty", "entries", List.of(List.of()))).isError());
    }

    @Test
    void refusesSectionIdsUsedMoreThanOnce() {
        assertTrue(parse(Map.of("sections", List.of(FIXED, FIXED))).isError());
    }

    @Test
    void refusesBookTitlesLongerThanVanillaAllows() {
        assertTrue(parse(Map.of("title", "a".repeat(32))).isSuccess());
        assertTrue(parse(Map.of("title", "a".repeat(33))).isError());
    }

    @Test
    void refusesDateFormatsThatAreNotDateFormats() {
        assertTrue(parse(Map.of("date_format", "d MMM {")).isError());
    }

    @Test
    void listsTheGroupsItNamesThatAreNotGiven() {
        final var leaders = new HashMap<>(listed("leaders", Map.of("miner", List.of("${player}"), "angler", List.of("${player}"), "default", List.of("${player}"))));
        final var ranking = new HashMap<>(ranking("farmer", List.of("${player}")));
        ranking.put("id", "ranking");
        final var book = parse(Map.of("sections", List.of(leaders, ranking))).getOrThrow();

        assertEquals(List.of("farmer"), book.unknownGroups(List.of("miner", "angler")));
    }
}
